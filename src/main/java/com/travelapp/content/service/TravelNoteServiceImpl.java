package com.travelapp.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelapp.common.BusinessException;
import com.travelapp.common.ErrorCode;
import com.travelapp.common.PageResult;
import com.travelapp.content.dto.CommentDTO;
import com.travelapp.content.dto.CreateCommentRequestDTO;
import com.travelapp.content.dto.CreateTravelNoteRequestDTO;
import com.travelapp.content.dto.DayPlanDTO;
import com.travelapp.content.dto.NoteDestinationDTO;
import com.travelapp.content.dto.TravelNoteDetailDTO;
import com.travelapp.content.dto.TravelNoteSummaryDTO;
import com.travelapp.content.dto.UpdateTravelNoteRequestDTO;
import com.travelapp.content.entity.Destination;
import com.travelapp.content.entity.NoteComment;
import com.travelapp.content.entity.NoteInteraction;
import com.travelapp.content.entity.Tag;
import com.travelapp.content.entity.TravelNote;
import com.travelapp.content.entity.TravelNoteDay;
import com.travelapp.content.entity.TravelNoteImage;
import com.travelapp.content.entity.TravelNoteTag;
import com.travelapp.content.mapper.DestinationMapper;
import com.travelapp.content.mapper.NoteCommentMapper;
import com.travelapp.content.mapper.NoteInteractionMapper;
import com.travelapp.content.mapper.NoteFavoriteMapper;
import com.travelapp.content.entity.NoteFavorite;
import com.travelapp.content.mapper.TagMapper;
import com.travelapp.content.mapper.TravelNoteDayMapper;
import com.travelapp.content.mapper.TravelNoteImageMapper;
import com.travelapp.content.mapper.TravelNoteMapper;
import com.travelapp.content.mapper.TravelNoteTagMapper;
import com.travelapp.customize.entity.CustomizeSession;
import com.travelapp.customize.mapper.CustomizeSessionMapper;
import com.travelapp.user.entity.User;
import com.travelapp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TravelNoteServiceImpl implements TravelNoteService {
    private static final int NORMAL_STATUS = 2;
    private static final String LIKE = "LIKE";
    private static final String FAVORITE = "FAVORITE";

    private final TravelNoteMapper travelNoteMapper;
    private final NoteCommentMapper commentMapper;
    private final NoteInteractionMapper interactionMapper;
    private final NoteFavoriteMapper favoriteMapper;
    private final DestinationMapper destinationMapper;
    private final UserMapper userMapper;
    private final TravelNoteImageMapper imageMapper;
    private final TravelNoteTagMapper noteTagMapper;
    private final TagMapper tagMapper;
    private final TravelNoteDayMapper noteDayMapper;
    private final NoteViewCountService viewCountService;
    private final CustomizeSessionMapper customizeSessionMapper;

    @Override
    public PageResult<TravelNoteSummaryDTO> list(Long destinationId, String keyword, Integer daysMin, Integer daysMax,
            BigDecimal budgetMin, BigDecimal budgetMax, String sort, long page, long pageSize, Long viewerId) {
        LambdaQueryWrapper<TravelNote> query = new LambdaQueryWrapper<TravelNote>()
                .eq(TravelNote::getStatus, NORMAL_STATUS)
                .eq(destinationId != null, TravelNote::getDestinationId, destinationId)
                .and(keyword != null && !keyword.isBlank(), w -> w.like(TravelNote::getTitle, keyword).or().like(TravelNote::getContent, keyword))
                .ge(daysMin != null, TravelNote::getDays, daysMin)
                .le(daysMax != null, TravelNote::getDays, daysMax)
                .ge(budgetMin != null, TravelNote::getBudgetPerPerson, budgetMin)
                .le(budgetMax != null, TravelNote::getBudgetPerPerson, budgetMax);
        applySort(query, sort);
        Page<TravelNote> result = travelNoteMapper.selectPage(new Page<>(page, pageSize), query);
        return PageResult.<TravelNoteSummaryDTO>builder()
                .total(result.getTotal()).page(page).pageSize(pageSize)
                .records(result.getRecords().stream().map(note -> toSummary(note, viewerId)).collect(Collectors.toList()))
                .build();
    }

    /** sort values correspond to API Documentation 3.3: recommend (default, by engagement)/latest (most recently published)/budget_asc (lowest to highest budget per person). */
    private void applySort(LambdaQueryWrapper<TravelNote> query, String sort) {
        if ("latest".equals(sort)) {
            query.orderByDesc(TravelNote::getCreatedAt);
        } else if ("budget_asc".equals(sort)) {
            // Rows with a null budget per person go last, instead of MySQL's default ascending behavior of sorting NULLs first
            query.last("ORDER BY budget_per_person IS NULL, budget_per_person ASC");
        } else {
            query.orderByDesc(TravelNote::getLikeCount).orderByDesc(TravelNote::getFavoriteCount).orderByDesc(TravelNote::getCreatedAt);
        }
    }

    @Override
    public TravelNoteDetailDTO getById(Long noteId, Long viewerId) {
        TravelNote note = requireNote(noteId);
        viewCountService.increment(noteId);
        return toDetail(note, viewerId);
    }

    @Override
    @Transactional
    public TravelNoteDetailDTO create(Long userId, CreateTravelNoteRequestDTO request) {
        validateDestination(request.getDestinationId());
        TravelNote note = new TravelNote();
        note.setUserId(userId);
        applyRequest(note, request);
        note.setStatus(NORMAL_STATUS);
        note.setSource(1);
        note.setViewCount(0);
        note.setPublishedAt(java.time.LocalDateTime.now());
        note.setLikeCount(0);
        note.setFavoriteCount(0);
        note.setCommentCount(0);
        travelNoteMapper.insert(note);
        saveImages(note.getId(), request.getImages());
        saveTags(note.getId(), request.getTags());
        saveDayPlans(note.getId(), request.getDayPlans());
        return toDetail(note, userId);
    }

    @Override
    @Transactional
    public TravelNoteDetailDTO update(Long userId, Long noteId, UpdateTravelNoteRequestDTO request) {
        TravelNote note = requireNote(noteId);
        requireOwner(userId, note.getUserId());
        validateDestination(request.getDestinationId());
        applyRequest(note, request);
        travelNoteMapper.updateById(note);
        saveImages(noteId, request.getImages());
        saveTags(noteId, request.getTags());
        saveDayPlans(noteId, request.getDayPlans());
        return toDetail(note, userId);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long noteId) {
        TravelNote note = requireNote(noteId);
        requireOwner(userId, note.getUserId());
        commentMapper.delete(new LambdaQueryWrapper<NoteComment>().eq(NoteComment::getNoteId, noteId));
        interactionMapper.delete(new LambdaQueryWrapper<NoteInteraction>().eq(NoteInteraction::getNoteId, noteId));
        favoriteMapper.delete(new LambdaQueryWrapper<NoteFavorite>().eq(NoteFavorite::getNoteId, noteId));
        // Customize sessions started from this note (possibly by other users) keep their plans; just drop the source reference
        customizeSessionMapper.update(null, new LambdaUpdateWrapper<CustomizeSession>()
                .eq(CustomizeSession::getSourceNoteId, noteId).set(CustomizeSession::getSourceNoteId, null));
        travelNoteMapper.deleteById(noteId);
    }

    @Override
    public PageResult<CommentDTO> listComments(Long noteId, long page, long pageSize, Long viewerId) {
        requireNote(noteId);
        Page<NoteComment> result = commentMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<NoteComment>().eq(NoteComment::getNoteId, noteId)
                        .eq(NoteComment::getStatus, NORMAL_STATUS).orderByAsc(NoteComment::getCreatedAt));
        return PageResult.<CommentDTO>builder()
                .total(result.getTotal()).page(page).pageSize(pageSize)
                .records(result.getRecords().stream().map(comment -> toComment(comment, viewerId)).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public CommentDTO createComment(Long userId, Long noteId, CreateCommentRequestDTO request) {
        requireNote(noteId);
        if (request.getParentId() != null) {
            NoteComment parent = commentMapper.selectById(request.getParentId());
            if (parent == null || !noteId.equals(parent.getNoteId()) || !Integer.valueOf(NORMAL_STATUS).equals(parent.getStatus())) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "the comment being replied to does not exist");
            }
        }
        NoteComment comment = new NoteComment();
        comment.setNoteId(noteId);
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent().trim());
        comment.setStatus(NORMAL_STATUS);
        commentMapper.insert(comment);
        increaseNoteCount(noteId, "comment_count");
        return toComment(comment, userId);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long noteId, Long commentId) {
        NoteComment comment = commentMapper.selectById(commentId);
        if (comment == null || !noteId.equals(comment.getNoteId()) || !Integer.valueOf(NORMAL_STATUS).equals(comment.getStatus())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "comment does not exist");
        }
        requireOwner(userId, comment.getUserId());
        commentMapper.deleteById(commentId);
        decreaseNoteCount(noteId, "comment_count");
    }

    @Override
    @Transactional
    public void like(Long userId, Long noteId) {
        interact(userId, noteId, LIKE, true);
    }

    @Override
    @Transactional
    public void unlike(Long userId, Long noteId) {
        interact(userId, noteId, LIKE, false);
    }

    @Override
    @Transactional
    public void favorite(Long userId, Long noteId) {
        favoriteInteract(userId, noteId, true);
    }

    @Override
    @Transactional
    public void unfavorite(Long userId, Long noteId) {
        favoriteInteract(userId, noteId, false);
    }

    private void interact(Long userId, Long noteId, String type, boolean add) {
        requireNote(noteId);
        if (FAVORITE.equals(type)) return;
        LambdaQueryWrapper<NoteInteraction> query = new LambdaQueryWrapper<NoteInteraction>()
                .eq(NoteInteraction::getNoteId, noteId).eq(NoteInteraction::getUserId, userId).eq(NoteInteraction::getTargetType, 1);
        NoteInteraction existing = interactionMapper.selectOne(query);
        if (add && existing == null) {
            NoteInteraction interaction = new NoteInteraction();
            interaction.setNoteId(noteId);
            interaction.setUserId(userId);
            interaction.setTargetType(1);
            interactionMapper.insert(interaction);
            increaseNoteCount(noteId, LIKE.equals(type) ? "like_count" : "favorite_count");
        } else if (!add && existing != null) {
            interactionMapper.deleteById(existing.getId());
            decreaseNoteCount(noteId, LIKE.equals(type) ? "like_count" : "favorite_count");
        }
    }

    private void favoriteInteract(Long userId, Long noteId, boolean add) {
        requireNote(noteId);
        NoteFavorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<NoteFavorite>().eq(NoteFavorite::getNoteId, noteId).eq(NoteFavorite::getUserId, userId));
        if (add && existing == null) { NoteFavorite favorite = new NoteFavorite(); favorite.setNoteId(noteId); favorite.setUserId(userId); favoriteMapper.insert(favorite); increaseNoteCount(noteId, "favorite_count"); }
        else if (!add && existing != null) { favoriteMapper.deleteById(existing.getId()); decreaseNoteCount(noteId, "favorite_count"); }
    }

    @Override
    public PageResult<TravelNoteSummaryDTO> listFavorites(Long userId, long page, long pageSize) {
        Page<NoteFavorite> favoritePage = favoriteMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<NoteFavorite>().eq(NoteFavorite::getUserId, userId)
                        .orderByDesc(NoteFavorite::getCreatedAt));
        List<Long> noteIds = favoritePage.getRecords().stream().map(NoteFavorite::getNoteId).collect(Collectors.toList());
        Map<Long, TravelNote> notesById = noteIds.isEmpty() ? Collections.emptyMap()
                : travelNoteMapper.selectBatchIds(noteIds).stream().collect(Collectors.toMap(TravelNote::getId, n -> n));
        List<TravelNoteSummaryDTO> records = noteIds.stream().map(notesById::get).filter(Objects::nonNull)
                .map(note -> toSummary(note, userId)).collect(Collectors.toList());
        return PageResult.<TravelNoteSummaryDTO>builder()
                .total(favoritePage.getTotal()).page(page).pageSize(pageSize).records(records).build();
    }

    private void applyRequest(TravelNote note, CreateTravelNoteRequestDTO request) {
        note.setDestinationId(request.getDestinationId());
        note.setTitle(request.getTitle().trim());
        note.setContent(request.getContent().trim());
        note.setCoverImage(request.getCoverImage());
        note.setDays(request.getDays() != null ? request.getDays() : 1);
        note.setBudgetPerPerson(request.getBudgetPerPerson());
        note.setTransportType(request.getTransportType());
        note.setBestSeason(request.getBestSeason());
    }

    private void validateDestination(Long destinationId) {
        if (destinationId != null && destinationMapper.selectById(destinationId) == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "destination does not exist");
        }
    }

    private TravelNote requireNote(Long noteId) {
        TravelNote note = travelNoteMapper.selectById(noteId);
        if (note == null || !Integer.valueOf(NORMAL_STATUS).equals(note.getStatus())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "travel note does not exist");
        }
        return note;
    }

    private void requireOwner(Long actualUserId, Long ownerId) {
        if (!actualUserId.equals(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "not authorized to operate on this resource");
        }
    }

    private void increaseNoteCount(Long noteId, String column) {
        travelNoteMapper.update(null, new LambdaUpdateWrapper<TravelNote>().eq(TravelNote::getId, noteId).setSql(column + " = " + column + " + 1"));
    }

    private void decreaseNoteCount(Long noteId, String column) {
        travelNoteMapper.update(null, new LambdaUpdateWrapper<TravelNote>().eq(TravelNote::getId, noteId).setSql(column + " = GREATEST(" + column + " - 1, 0)"));
    }

    private TravelNoteSummaryDTO toSummary(TravelNote note, Long viewerId) {
        User author = userMapper.selectById(note.getUserId());
        return TravelNoteSummaryDTO.builder().id(note.getId()).destinationId(note.getDestinationId()).title(note.getTitle())
                .coverImage(note.getCoverImage()).days(note.getDays()).budgetPerPerson(note.getBudgetPerPerson())
                .tags(loadTagNames(note.getId())).authorId(note.getUserId())
                .authorNickname(author == null ? null : author.getNickname()).authorAvatarUrl(author == null ? null : author.getAvatarUrl())
                .likeCount(note.getLikeCount()).favoriteCount(note.getFavoriteCount()).commentCount(note.getCommentCount()).createdAt(note.getCreatedAt())
                .liked(hasInteraction(viewerId, note.getId(), LIKE)).favorited(hasInteraction(viewerId, note.getId(), FAVORITE)).build();
    }

    private TravelNoteDetailDTO toDetail(TravelNote note, Long viewerId) {
        User author = userMapper.selectById(note.getUserId());
        Destination destination = destinationMapper.selectById(note.getDestinationId());
        return TravelNoteDetailDTO.builder().id(note.getId()).destinationId(note.getDestinationId())
                .destination(destination == null ? null : NoteDestinationDTO.builder().id(destination.getId()).name(destination.getName()).build())
                .title(note.getTitle()).content(note.getContent())
                .coverImage(note.getCoverImage()).days(note.getDays()).budgetPerPerson(note.getBudgetPerPerson())
                .transportType(note.getTransportType()).bestSeason(note.getBestSeason()).dayPlans(loadDayPlans(note.getId()))
                .images(loadImages(note.getId())).tags(loadTagNames(note.getId())).authorId(note.getUserId())
                .authorNickname(author == null ? null : author.getNickname()).authorAvatarUrl(author == null ? null : author.getAvatarUrl())
                .viewCount(note.getViewCount()).likeCount(note.getLikeCount()).favoriteCount(note.getFavoriteCount()).commentCount(note.getCommentCount())
                .createdAt(note.getCreatedAt()).updatedAt(note.getUpdatedAt()).liked(hasInteraction(viewerId, note.getId(), LIKE))
                .favorited(hasInteraction(viewerId, note.getId(), FAVORITE)).editable(note.getUserId().equals(viewerId)).build();
    }

    private CommentDTO toComment(NoteComment comment, Long viewerId) {
        User author = userMapper.selectById(comment.getUserId());
        return CommentDTO.builder().id(comment.getId()).parentId(comment.getParentId()).content(comment.getContent()).authorId(comment.getUserId())
                .authorNickname(author == null ? null : author.getNickname()).authorAvatarUrl(author == null ? null : author.getAvatarUrl())
                .createdAt(comment.getCreatedAt()).editable(comment.getUserId().equals(viewerId)).build();
    }

    private boolean hasInteraction(Long userId, Long noteId, String type) {
        if (userId == null) return false;
        if (FAVORITE.equals(type)) return favoriteMapper.selectCount(new LambdaQueryWrapper<NoteFavorite>().eq(NoteFavorite::getNoteId, noteId).eq(NoteFavorite::getUserId, userId)) > 0;
        return interactionMapper.selectCount(new LambdaQueryWrapper<NoteInteraction>().eq(NoteInteraction::getNoteId, noteId).eq(NoteInteraction::getUserId, userId).eq(NoteInteraction::getTargetType, 1)) > 0;
    }

    /** Overwrite-style save of embedded body images: clear the old ones first, then insert the new ones in order. */
    private void saveImages(Long noteId, List<String> images) {
        imageMapper.delete(new LambdaQueryWrapper<TravelNoteImage>().eq(TravelNoteImage::getNoteId, noteId));
        if (images == null || images.isEmpty()) return;
        int sortOrder = 0;
        for (String url : images) {
            TravelNoteImage image = new TravelNoteImage();
            image.setNoteId(noteId);
            image.setImageUrl(url);
            image.setSortOrder(sortOrder++);
            imageMapper.insert(image);
        }
    }

    private List<String> loadImages(Long noteId) {
        return imageMapper.selectList(new LambdaQueryWrapper<TravelNoteImage>()
                        .eq(TravelNoteImage::getNoteId, noteId).orderByAsc(TravelNoteImage::getSortOrder))
                .stream().map(TravelNoteImage::getImageUrl).collect(Collectors.toList());
    }

    /** Overwrite-style save of the structured daily itinerary: clear the old ones first, then insert the new ones in dayNumber order. */
    private void saveDayPlans(Long noteId, List<DayPlanDTO> dayPlans) {
        noteDayMapper.delete(new LambdaQueryWrapper<TravelNoteDay>().eq(TravelNoteDay::getNoteId, noteId));
        if (dayPlans == null || dayPlans.isEmpty()) return;
        for (DayPlanDTO plan : dayPlans) {
            if (plan.getDayNumber() == null) continue; // day_number is a NOT NULL column; dirty data missing this field is simply discarded
            TravelNoteDay day = new TravelNoteDay();
            day.setNoteId(noteId);
            day.setDayNumber(plan.getDayNumber());
            day.setTitle(plan.getTitle());
            day.setDescription(plan.getDescription());
            day.setCost(plan.getCost());
            noteDayMapper.insert(day);
        }
    }

    private List<DayPlanDTO> loadDayPlans(Long noteId) {
        return noteDayMapper.selectList(new LambdaQueryWrapper<TravelNoteDay>()
                        .eq(TravelNoteDay::getNoteId, noteId).orderByAsc(TravelNoteDay::getDayNumber))
                .stream().map(d -> DayPlanDTO.builder().dayNumber(d.getDayNumber()).title(d.getTitle())
                        .description(d.getDescription()).cost(d.getCost()).build())
                .collect(Collectors.toList());
    }

    /**
     * Overwrite-style save of tags: tagNames here is free text sent by the frontend (not tagId from the
     * preset tag library). Look up existing tags by name, create one on the fly if missing, then link it
     * to travel_note_tags.
     */
    private void saveTags(Long noteId, List<String> tagNames) {
        noteTagMapper.delete(new LambdaQueryWrapper<TravelNoteTag>().eq(TravelNoteTag::getNoteId, noteId));
        if (tagNames == null || tagNames.isEmpty()) return;
        tagNames.stream().filter(name -> name != null && !name.isBlank()).map(String::trim).distinct().forEach(name -> {
            Long tagId = findOrCreateTag(name);
            TravelNoteTag link = new TravelNoteTag();
            link.setNoteId(noteId);
            link.setTagId(tagId);
            noteTagMapper.insert(link);
        });
    }

    private Long findOrCreateTag(String name) {
        Tag existing = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name));
        if (existing != null) return existing.getId();
        Tag tag = new Tag();
        tag.setName(name);
        tag.setCategory(2); // 2=interest preference; free-form UGC tags are all grouped here
        tagMapper.insert(tag);
        return tag.getId();
    }

    private List<String> loadTagNames(Long noteId) {
        List<Long> tagIds = noteTagMapper.selectList(new LambdaQueryWrapper<TravelNoteTag>().eq(TravelNoteTag::getNoteId, noteId))
                .stream().map(TravelNoteTag::getTagId).collect(Collectors.toList());
        if (tagIds.isEmpty()) return Collections.emptyList();
        return tagMapper.selectBatchIds(tagIds).stream().map(Tag::getName).collect(Collectors.toList());
    }
}
