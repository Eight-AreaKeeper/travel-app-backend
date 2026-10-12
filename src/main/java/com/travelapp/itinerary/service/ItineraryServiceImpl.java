package com.travelapp.itinerary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.travelapp.common.BusinessException;
import com.travelapp.common.ErrorCode;
import com.travelapp.common.PageResult;
import com.travelapp.content.entity.Poi;
import com.travelapp.content.mapper.PoiMapper;
import com.travelapp.customize.entity.CustomizeAnswer;
import com.travelapp.customize.entity.CustomizeDestination;
import com.travelapp.customize.mapper.CustomizeAnswerMapper;
import com.travelapp.customize.mapper.CustomizeDestinationMapper;
import com.travelapp.itinerary.dto.AdjustItemRequestDTO;
import com.travelapp.itinerary.dto.AdjustItemResponseDTO;
import com.travelapp.itinerary.dto.AlternativeDTO;
import com.travelapp.itinerary.dto.ItineraryDayDTO;
import com.travelapp.itinerary.dto.ItineraryDestinationDTO;
import com.travelapp.itinerary.dto.ItineraryItemDTO;
import com.travelapp.itinerary.dto.ItineraryPlanDetailDTO;
import com.travelapp.itinerary.dto.ItineraryPlanSummaryDTO;
import com.travelapp.itinerary.dto.ItineraryStatusDTO;
import com.travelapp.itinerary.dto.RegenerateRequestDTO;
import com.travelapp.itinerary.dto.RegenerateResultDTO;
import com.travelapp.itinerary.engine.DestinationRef;
import com.travelapp.itinerary.engine.DestinationSegment;
import com.travelapp.itinerary.engine.DraftItem;
import com.travelapp.itinerary.engine.ItineraryCategories;
import com.travelapp.itinerary.engine.ItineraryPlanBuilder;
import com.travelapp.itinerary.entity.ItineraryDay;
import com.travelapp.itinerary.entity.ItineraryItem;
import com.travelapp.itinerary.entity.ItineraryPlan;
import com.travelapp.itinerary.mapper.ItineraryDayMapper;
import com.travelapp.itinerary.mapper.ItineraryItemMapper;
import com.travelapp.itinerary.mapper.ItineraryPlanMapper;
import com.travelapp.itinerary.mq.ItineraryGenerationMessage;
import com.travelapp.itinerary.mq.ItineraryMqConfig;
import com.travelapp.map.AmapPoiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Itinerary generation engine: AI engine (Gemini, generated_by=2) + rule engine fallback (generated_by=1),
 * supports multi-destination trips and true asynchronous generation (ItineraryGenerationListener consumes a
 * RabbitMQ message and calls generateContent).
 * The specific logic for "how to generate a single day's itinerary" lives in the itinerary.engine package;
 * this class is only responsible for orchestration (loading context / Amap POI backfill / persistence /
 * status transitions) and doesn't implement the generation algorithm itself.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItineraryServiceImpl implements ItineraryService {

    private static final int DEFAULT_DAYS = 3;
    private static final int MAX_DAYS = 30;

    private static final int STATUS_GENERATING = 0;
    private static final int STATUS_GENERATED = 1;
    private static final int STATUS_USER_EDITED = 2;
    private static final int STATUS_FAILED = 4;

    private final CustomizeAnswerMapper answerMapper;
    private final CustomizeDestinationMapper customizeDestinationMapper;
    private final PoiMapper poiMapper;
    private final ItineraryPlanMapper planMapper;
    private final ItineraryDayMapper dayMapper;
    private final ItineraryItemMapper itemMapper;
    private final AmapPoiService amapPoiService;
    private final ItineraryPlanBuilder planBuilder;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public Long requestGeneration(Long sessionId, Long userId) {
        ItineraryPlan plan = new ItineraryPlan();
        plan.setSessionId(sessionId);
        plan.setUserId(userId);
        plan.setVersion(1);
        plan.setStatus(STATUS_GENERATING);
        planMapper.insert(plan);
        publishGenerationMessage(plan.getId(), sessionId, userId, null);
        return plan.getId();
    }

    @Override
    @Transactional
    public void generateContent(Long planId, Long sessionId, Long userId, Long basedOnPlanId) {
        ItineraryPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            log.warn("The itinerary plan for this generation task does not exist, may have been deleted: planId={}", planId);
            return;
        }
        GenerationContext ctx = loadContext(sessionId);
        Map<Integer, List<ItineraryItem>> preservedByDay = basedOnPlanId == null ? null : collectUserEditedItems(basedOnPlanId);
        PersistResult persisted = persistDays(planId, ctx, preservedByDay);
        plan.setTotalEstimatedCost(persisted.totalCost());
        plan.setGeneratedBy(persisted.usedAi() ? 2 : 1);
        plan.setStatus(STATUS_GENERATED);
        planMapper.updateById(plan);
    }

    @Override
    @Transactional
    public void markFailed(Long planId) {
        ItineraryPlan plan = planMapper.selectById(planId);
        if (plan != null) {
            plan.setStatus(STATUS_FAILED);
            planMapper.updateById(plan);
        }
    }

    @Override
    public ItineraryPlanDetailDTO getDetail(Long userId, Long planId) {
        ItineraryPlan plan = requireOwnedPlan(userId, planId);
        List<DestinationRef> destinations = resolvePlanDestinations(plan);
        DestinationRef primary = destinations.isEmpty() ? null : destinations.get(0);

        List<ItineraryDay> days = dayMapper.selectList(
                new LambdaQueryWrapper<ItineraryDay>().eq(ItineraryDay::getPlanId, planId).orderByAsc(ItineraryDay::getDayNumber));
        List<ItineraryDayDTO> dayDTOs = days.stream().map(day -> {
            List<ItineraryItem> items = itemMapper.selectList(
                    new LambdaQueryWrapper<ItineraryItem>().eq(ItineraryItem::getDayId, day.getId()).orderByAsc(ItineraryItem::getSortOrder));
            List<ItineraryItemDTO> itemDTOs = items.stream().map(this::toItemDTO).collect(Collectors.toList());
            return ItineraryDayDTO.builder()
                    .dayNumber(day.getDayNumber()).date(day.getDate())
                    .destination(toDestinationDTO(day.getDestinationId(), day.getDestinationName()))
                    .dailyEstimatedCost(day.getDailyEstimatedCost()).items(itemDTOs).build();
        }).collect(Collectors.toList());

        return ItineraryPlanDetailDTO.builder()
                .id(plan.getId()).sessionId(plan.getSessionId()).version(plan.getVersion())
                .status(statusToString(plan.getStatus())).totalEstimatedCost(plan.getTotalEstimatedCost())
                .destination(toDestinationDTO(primary))
                .destinations(destinations.stream().map(this::toDestinationDTO).collect(Collectors.toList()))
                .days(dayDTOs).build();
    }

    @Override
    public ItineraryStatusDTO getStatus(Long userId, Long planId) {
        ItineraryPlan plan = requireOwnedPlan(userId, planId);
        Integer status = plan.getStatus();
        int progress = (status == null || status == STATUS_GENERATING) ? 30 : 100;
        return ItineraryStatusDTO.builder().status(statusToString(status)).progress(progress).build();
    }

    @Override
    @Transactional
    public AdjustItemResponseDTO adjustItem(Long userId, Long planId, Long itemId, AdjustItemRequestDTO request) {
        ItineraryPlan plan = requireOwnedPlan(userId, planId);
        ItineraryItem item = itemMapper.selectById(itemId);
        ItineraryDay day = item == null ? null : dayMapper.selectById(item.getDayId());
        if (item == null || day == null || !planId.equals(day.getPlanId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary item does not exist");
        }

        String action = request.getAction();
        if ("remove".equals(action)) {
            itemMapper.deleteById(itemId);
            recomputeTotals(day, plan);
            return AdjustItemResponseDTO.builder().item(null).build();
        }
        if ("replace".equals(action)) {
            if (request.getPoiId() == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "The replace action requires a poiId");
            }
            Poi poi = poiMapper.selectById(request.getPoiId());
            if (poi == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "Candidate location does not exist");
            }
            item.setPoiId(poi.getId());
            item.setTitle(poi.getName());
            item.setDescription(poi.getAddress());
            item.setEstimatedCost(poi.getAvgCost());
            item.setIsUserEdited(true);
            itemMapper.updateById(item);
        } else if ("edit".equals(action)) {
            if (request.getTitle() != null) item.setTitle(request.getTitle());
            if (request.getDescription() != null) item.setDescription(request.getDescription());
            if (request.getEstimatedCost() != null) item.setEstimatedCost(request.getEstimatedCost());
            item.setIsUserEdited(true);
            itemMapper.updateById(item);
        } else {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "action must be one of replace/remove/edit");
        }
        recomputeTotals(day, plan);
        return AdjustItemResponseDTO.builder().item(toItemDTO(item)).build();
    }

    @Override
    public List<AlternativeDTO> alternatives(Long userId, Long planId, Long itemId) {
        ItineraryPlan plan = requireOwnedPlan(userId, planId);
        ItineraryItem item = itemMapper.selectById(itemId);
        ItineraryDay day = item == null ? null : dayMapper.selectById(item.getDayId());
        if (item == null || day == null || !planId.equals(day.getPlanId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary item does not exist");
        }

        Integer poiType = ItineraryCategories.categoryToPoiType(item.getCategory());
        DestinationRef fallbackDestination = day.getDestinationName() == null ? resolvePlanDestination(plan) : null;
        String destinationName = day.getDestinationName() != null ? day.getDestinationName()
                : (fallbackDestination == null ? null : fallbackDestination.name());
        if (poiType == null || destinationName == null) {
            return List.of();
        }

        LambdaQueryWrapper<Poi> query = new LambdaQueryWrapper<Poi>()
                .eq(Poi::getDestinationName, destinationName).eq(Poi::getType, poiType);
        if (item.getPoiId() != null) {
            query.ne(Poi::getId, item.getPoiId());
        }
        List<Poi> candidates = poiMapper.selectList(query.orderByAsc(Poi::getId));

        return candidates.stream().limit(5)
                .map(poi -> AlternativeDTO.builder().poiId(poi.getId()).name(poi.getName())
                        .estimatedCost(poi.getAvgCost()).reason(buildReason(poi, item.getEstimatedCost())).build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RegenerateResultDTO regenerate(Long userId, Long planId, RegenerateRequestDTO request) {
        ItineraryPlan oldPlan = requireOwnedPlan(userId, planId);
        Long sessionId = oldPlan.getSessionId();
        applyAnswerOverrides(sessionId, request);

        ItineraryPlan newPlan = new ItineraryPlan();
        newPlan.setSessionId(sessionId);
        newPlan.setUserId(userId);
        newPlan.setVersion(oldPlan.getVersion() + 1);
        newPlan.setStatus(STATUS_GENERATING);
        planMapper.insert(newPlan);

        publishGenerationMessage(newPlan.getId(), sessionId, userId, oldPlan.getId());

        return RegenerateResultDTO.builder().planId(newPlan.getId()).version(newPlan.getVersion()).status("generating").build();
    }

    @Override
    public PageResult<ItineraryPlanSummaryDTO> listMyPlans(Long userId, long page, long pageSize) {
        Page<ItineraryPlan> result = planMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<ItineraryPlan>().eq(ItineraryPlan::getUserId, userId).orderByDesc(ItineraryPlan::getId));
        List<ItineraryPlanSummaryDTO> records = result.getRecords().stream().map(plan -> {
            DestinationRef destination = resolvePlanDestination(plan);
            long dayCount = dayMapper.selectCount(new LambdaQueryWrapper<ItineraryDay>().eq(ItineraryDay::getPlanId, plan.getId()));
            return ItineraryPlanSummaryDTO.builder()
                    .id(plan.getId()).version(plan.getVersion())
                    .destination(toDestinationDTO(destination))
                    .days((int) dayCount).totalEstimatedCost(plan.getTotalEstimatedCost())
                    .status(statusToString(plan.getStatus())).createdAt(plan.getCreatedAt()).build();
        }).collect(Collectors.toList());
        return PageResult.<ItineraryPlanSummaryDTO>builder().total(result.getTotal()).page(page).pageSize(pageSize).records(records).build();
    }

    @Override
    public byte[] exportPdf(Long userId, Long planId) {
        ItineraryPlanDetailDTO plan = getDetail(userId, planId);
        boolean multiDestination = plan.getDestinations() != null && plan.getDestinations().size() > 1;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont cjkFont = loadCjkFont();
            Font titleFont = new Font(cjkFont, 18, Font.BOLD);
            Font dayFont = new Font(cjkFont, 14, Font.BOLD);
            Font itemFont = new Font(cjkFont, 11, Font.NORMAL);
            Font smallFont = new Font(cjkFont, 9, Font.NORMAL, new java.awt.Color(120, 120, 120));

            String destinationName = plan.getDestination() == null ? "My Itinerary" : plan.getDestination().getName();
            if (multiDestination) {
                destinationName = plan.getDestinations().stream().map(ItineraryDestinationDTO::getName).collect(Collectors.joining(" · "));
            }
            Paragraph title = new Paragraph(destinationName + " · " + plan.getDays().size() + "-day itinerary", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph summary = new Paragraph("Estimated total cost: ¥" + formatCost(plan.getTotalEstimatedCost()), smallFont);
            summary.setAlignment(Element.ALIGN_CENTER);
            summary.setSpacingAfter(16);
            document.add(summary);

            for (ItineraryDayDTO day : plan.getDays()) {
                String destinationSuffix = multiDestination && day.getDestination() != null
                        ? " · " + day.getDestination().getName() : "";
                Paragraph dayTitle = new Paragraph("Day " + day.getDayNumber() + destinationSuffix
                        + (day.getDate() != null ? "  " + day.getDate() : "")
                        + "  ·  Est. cost ¥" + formatCost(day.getDailyEstimatedCost()), dayFont);
                dayTitle.setSpacingBefore(10);
                dayTitle.setSpacingAfter(6);
                document.add(dayTitle);

                for (ItineraryItemDTO item : day.getItems()) {
                    String time = item.getStartTime() == null ? "" : item.getStartTime() + "  ";
                    Paragraph line = new Paragraph(time + item.getTitle()
                            + (item.getEstimatedCost() != null ? " (approx. ¥" + formatCost(item.getEstimatedCost()) + ")" : ""), itemFont);
                    line.setIndentationLeft(16);
                    document.add(line);
                    if (item.getDescription() != null && !item.getDescription().isBlank()) {
                        Paragraph desc = new Paragraph(item.getDescription(), smallFont);
                        desc.setIndentationLeft(16);
                        document.add(desc);
                    }
                }
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Itinerary export failed");
        }
    }

    ItineraryPlan requireOwnedPlan(Long userId, Long planId) {
        ItineraryPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary plan does not exist");
        }
        if (!userId.equals(plan.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "No permission to access this itinerary plan");
        }
        return plan;
    }

    /**
     * Publish the message. If the message is sent out before the transaction that inserted the plan row has
     * committed, the consumer could start running faster than the producer's transaction commits and fail to
     * find the plan (under MySQL's default isolation level, uncommitted rows from other transactions aren't
     * visible) -- so if we're actually inside a transaction, defer sending the message until after the
     * transaction commits (afterCommit); if not inside a transaction (shouldn't happen in theory, this is just
     * a fallback), send it directly.
     */
    private void publishGenerationMessage(Long planId, Long sessionId, Long userId, Long basedOnPlanId) {
        ItineraryGenerationMessage message = new ItineraryGenerationMessage(planId, sessionId, userId, basedOnPlanId);
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            rabbitTemplate.convertAndSend(ItineraryMqConfig.EXCHANGE, ItineraryMqConfig.ROUTING_KEY, message);
                        }
                    });
        } else {
            rabbitTemplate.convertAndSend(ItineraryMqConfig.EXCHANGE, ItineraryMqConfig.ROUTING_KEY, message);
        }
    }

    /** Load all the context needed to generate one version of an itinerary: multi-destination day segmentation + POI candidates per segment (backfilling the local pois table from Amap on demand first). */
    private GenerationContext loadContext(Long sessionId) {
        CustomizeAnswer answer = answerMapper.selectOne(
                new LambdaQueryWrapper<CustomizeAnswer>().eq(CustomizeAnswer::getSessionId, sessionId));
        List<CustomizeDestination> customizeDestinations = customizeDestinationMapper.selectList(
                new LambdaQueryWrapper<CustomizeDestination>().eq(CustomizeDestination::getSessionId, sessionId)
                        .orderByAsc(CustomizeDestination::getSortOrder));
        if (customizeDestinations.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "Please select a destination in the questionnaire first");
        }

        int totalDays = normalizeDays(answer);
        int n = customizeDestinations.size();
        int base = totalDays / n;
        int remainder = totalDays % n;

        List<DestinationSegment> segments = new ArrayList<>();
        int cursor = 1;
        for (int i = 0; i < n; i++) {
            CustomizeDestination customizeDestination = customizeDestinations.get(i);
            String destinationName = customizeDestination.getDestinationName();
            if (destinationName == null || destinationName.isBlank()) continue; // dirty data, skip this one

            int segDays = base + (i < remainder ? 1 : 0);
            if (segDays <= 0) segDays = 1; // extreme input where destination count exceeds total days; ensure each segment has at least 1 day

            amapPoiService.ensureCached(destinationName, Poi.TYPE_SCENIC);
            amapPoiService.ensureCached(destinationName, Poi.TYPE_FOOD);
            amapPoiService.ensureCached(destinationName, Poi.TYPE_HOTEL);

            int dayFrom = cursor;
            int dayTo = cursor + segDays - 1;
            DestinationRef ref = new DestinationRef(customizeDestination.getDestinationId(), destinationName);
            segments.add(new DestinationSegment(ref, dayFrom, dayTo,
                    poisByName(destinationName, Poi.TYPE_SCENIC), poisByName(destinationName, Poi.TYPE_FOOD), poisByName(destinationName, Poi.TYPE_HOTEL)));
            cursor = dayTo + 1;
        }
        if (segments.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "Destination does not exist");
        }
        int actualTotalDays = segments.get(segments.size() - 1).dayTo();
        return new GenerationContext(segments, actualTotalDays, answer);
    }

    private void applyAnswerOverrides(Long sessionId, RegenerateRequestDTO request) {
        if (request == null) return;
        CustomizeAnswer answer = answerMapper.selectOne(
                new LambdaQueryWrapper<CustomizeAnswer>().eq(CustomizeAnswer::getSessionId, sessionId));
        if (answer == null) return;
        boolean changed = false;
        if (request.getDays() != null) { answer.setDays(request.getDays()); changed = true; }
        if (request.getBudgetMin() != null) { answer.setBudgetMin(request.getBudgetMin().doubleValue()); changed = true; }
        if (request.getBudgetMax() != null) { answer.setBudgetMax(request.getBudgetMax().doubleValue()); changed = true; }
        if (request.getTravelerCount() != null) { answer.setTravelerCount(request.getTravelerCount()); changed = true; }
        if (request.getTravelStyle() != null) { answer.setTravelStyle(request.getTravelStyle()); changed = true; }
        if (changed) answerMapper.updateById(answer);
    }

    /** Fetch the items manually adjusted by the user (isUserEdited=true) from the old plan, grouped by day number, to preserve as much as possible during regenerate. */
    private Map<Integer, List<ItineraryItem>> collectUserEditedItems(Long oldPlanId) {
        Map<Integer, List<ItineraryItem>> result = new HashMap<>();
        List<ItineraryDay> oldDays = dayMapper.selectList(new LambdaQueryWrapper<ItineraryDay>().eq(ItineraryDay::getPlanId, oldPlanId));
        for (ItineraryDay oldDay : oldDays) {
            List<ItineraryItem> edited = itemMapper.selectList(new LambdaQueryWrapper<ItineraryItem>()
                    .eq(ItineraryItem::getDayId, oldDay.getId()).eq(ItineraryItem::getIsUserEdited, true));
            if (!edited.isEmpty()) {
                result.put(oldDay.getDayNumber(), edited);
            }
        }
        return result;
    }

    /** Call the generation engine (AI first, falling back to the rule engine when per-day validation fails or AI is unavailable) and persist the result; when preservedByDay is non-null, a preserved user-edited item takes priority over the same day/sortOrder slot. */
    private PersistResult persistDays(Long planId, GenerationContext ctx, Map<Integer, List<ItineraryItem>> preservedByDay) {
        ItineraryPlanBuilder.BuildResult built = planBuilder.build(ctx.segments(), ctx.totalDays(), ctx.answer());
        BigDecimal totalCost = BigDecimal.ZERO;

        for (DestinationSegment segment : ctx.segments()) {
            for (int dayNumber = segment.dayFrom(); dayNumber <= segment.dayTo(); dayNumber++) {
                List<DraftItem> draftItems = built.itemsByDay().get(dayNumber);

                ItineraryDay day = new ItineraryDay();
                day.setPlanId(planId);
                day.setDayNumber(dayNumber);
                day.setDestinationId(segment.destination().id());
                day.setDestinationName(segment.destination().name());
                dayMapper.insert(day);

                List<ItineraryItem> preserved = preservedByDay == null ? null : preservedByDay.get(dayNumber);
                BigDecimal dailyCost = BigDecimal.ZERO;
                int sortOrder = 1;
                for (DraftItem draft : draftItems) {
                    ItineraryItem preservedItem = findBySortOrder(preserved, sortOrder);
                    ItineraryItem item = new ItineraryItem();
                    item.setDayId(day.getId());
                    item.setSortOrder(sortOrder);
                    if (preservedItem != null) {
                        item.setStartTime(preservedItem.getStartTime());
                        item.setCategory(preservedItem.getCategory());
                        item.setPoiId(preservedItem.getPoiId());
                        item.setTitle(preservedItem.getTitle());
                        item.setDescription(preservedItem.getDescription());
                        item.setEstimatedCost(preservedItem.getEstimatedCost());
                        item.setIsUserEdited(true);
                    } else {
                        item.setStartTime(draft.startTime());
                        item.setCategory(draft.category());
                        item.setPoiId(draft.poiId());
                        item.setTitle(draft.title());
                        item.setDescription(draft.description());
                        item.setEstimatedCost(draft.cost());
                        item.setIsUserEdited(false);
                    }
                    itemMapper.insert(item);
                    dailyCost = dailyCost.add(item.getEstimatedCost() == null ? BigDecimal.ZERO : item.getEstimatedCost());
                    sortOrder++;
                }

                day.setDailyEstimatedCost(dailyCost);
                dayMapper.updateById(day);
                totalCost = totalCost.add(dailyCost);
            }
        }
        return new PersistResult(totalCost, built.usedAi());
    }

    private ItineraryItem findBySortOrder(List<ItineraryItem> items, int sortOrder) {
        if (items == null) return null;
        return items.stream().filter(i -> i.getSortOrder() != null && i.getSortOrder() == sortOrder).findFirst().orElse(null);
    }

    private void recomputeTotals(ItineraryDay day, ItineraryPlan plan) {
        List<ItineraryItem> items = itemMapper.selectList(new LambdaQueryWrapper<ItineraryItem>().eq(ItineraryItem::getDayId, day.getId()));
        BigDecimal dailyCost = items.stream().map(i -> i.getEstimatedCost() == null ? BigDecimal.ZERO : i.getEstimatedCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        day.setDailyEstimatedCost(dailyCost);
        dayMapper.updateById(day);

        List<ItineraryDay> allDays = dayMapper.selectList(new LambdaQueryWrapper<ItineraryDay>().eq(ItineraryDay::getPlanId, plan.getId()));
        BigDecimal totalCost = allDays.stream().map(d -> d.getDailyEstimatedCost() == null ? BigDecimal.ZERO : d.getDailyEstimatedCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        plan.setTotalEstimatedCost(totalCost);
        plan.setStatus(STATUS_USER_EDITED);
        planMapper.updateById(plan);
    }

    /** All destinations of the plan, in visiting order (sortOrder); only one element for single-destination trips. Destinations are free text and don't have to map to the local destinations table. */
    private List<DestinationRef> resolvePlanDestinations(ItineraryPlan plan) {
        List<CustomizeDestination> customizeDestinations = customizeDestinationMapper.selectList(
                new LambdaQueryWrapper<CustomizeDestination>().eq(CustomizeDestination::getSessionId, plan.getSessionId())
                        .orderByAsc(CustomizeDestination::getSortOrder));
        return customizeDestinations.stream()
                .map(cd -> new DestinationRef(cd.getDestinationId(), cd.getDestinationName()))
                .collect(Collectors.toList());
    }

    /** The primary destination (the first one), used for scenarios that only need a single destination, such as the PDF main title / list summary. */
    private DestinationRef resolvePlanDestination(ItineraryPlan plan) {
        List<DestinationRef> all = resolvePlanDestinations(plan);
        return all.isEmpty() ? null : all.get(0);
    }

    private int normalizeDays(CustomizeAnswer answer) {
        if (answer == null || answer.getDays() == null) return DEFAULT_DAYS;
        return Math.min(Math.max(answer.getDays(), 1), MAX_DAYS);
    }

    private List<Poi> poisByName(String destinationName, int type) {
        return poiMapper.selectList(new LambdaQueryWrapper<Poi>()
                .eq(Poi::getDestinationName, destinationName).eq(Poi::getType, type)
                .orderByAsc(Poi::getId));
    }

    private ItineraryItemDTO toItemDTO(ItineraryItem item) {
        return ItineraryItemDTO.builder()
                .id(item.getId()).sortOrder(item.getSortOrder())
                .startTime(item.getStartTime() == null ? null : item.getStartTime().toString())
                .category(ItineraryCategories.toCode(item.getCategory())).title(item.getTitle())
                .description(item.getDescription()).estimatedCost(item.getEstimatedCost())
                .poiId(item.getPoiId()).userEdited(Boolean.TRUE.equals(item.getIsUserEdited())).build();
    }

    private ItineraryDestinationDTO toDestinationDTO(DestinationRef ref) {
        return ref == null ? null : toDestinationDTO(ref.id(), ref.name());
    }

    private ItineraryDestinationDTO toDestinationDTO(Long id, String name) {
        return name == null ? null : ItineraryDestinationDTO.builder().id(id).name(name).build();
    }

    private String buildReason(Poi candidate, BigDecimal currentCost) {
        BigDecimal candidateCost = candidate.getAvgCost();
        if (candidateCost != null && currentCost != null && candidateCost.compareTo(currentCost) < 0) {
            return "Lower cost per person, more budget-friendly";
        }
        if (candidate.getRating() != null && candidate.getRating().compareTo(BigDecimal.valueOf(4.5)) >= 0) {
            return "Higher rating, better reviews";
        }
        return "A popular pick in the same category, for a change of style";
    }

    /**
     * OpenPDF's built-in "STSong-Light"+UniGB-UCS2-H scheme only bundles the CJK glyph mapping (CMap), not the
     * glyphs themselves, so it relies on the system that opens the PDF having a matching Chinese font installed
     * to render correctly -- open it on a machine without a Chinese font installed, or in an online preview
     * tool, and it comes out blank. This is changed to embed the font file directly into the PDF (Noto Sans SC,
     * SIL Open Font License, safe to distribute with the project); the generated PDF then renders Chinese
     * correctly on any machine, at the cost of the build depending on the font file under resources/fonts.
     */
    private BaseFont loadCjkFont() throws Exception {
        try (java.io.InputStream in = getClass().getClassLoader().getResourceAsStream("fonts/NotoSansSC-VF.ttf")) {
            if (in == null) {
                throw new IllegalStateException("Chinese font file fonts/NotoSansSC-VF.ttf not found");
            }
            byte[] fontBytes = in.readAllBytes();
            return BaseFont.createFont("NotoSansSC-VF.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
        }
    }

    private String formatCost(BigDecimal cost) {
        return cost == null ? "0" : cost.stripTrailingZeros().toPlainString();
    }

    static String statusToString(Integer status) {
        if (status == null) return "generating";
        return switch (status) {
            case STATUS_GENERATING -> "generating";
            case STATUS_GENERATED, STATUS_USER_EDITED, 3 -> "generated";
            case STATUS_FAILED -> "failed";
            default -> "generating";
        };
    }

    private record PersistResult(BigDecimal totalCost, boolean usedAi) {
    }

    private record GenerationContext(List<DestinationSegment> segments, int totalDays, CustomizeAnswer answer) {
    }
}
