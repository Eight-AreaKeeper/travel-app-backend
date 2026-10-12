package com.travelapp.itinerary.service;

import com.travelapp.common.PageResult;
import com.travelapp.itinerary.dto.AdjustItemRequestDTO;
import com.travelapp.itinerary.dto.AdjustItemResponseDTO;
import com.travelapp.itinerary.dto.AlternativeDTO;
import com.travelapp.itinerary.dto.ItineraryPlanDetailDTO;
import com.travelapp.itinerary.dto.ItineraryPlanSummaryDTO;
import com.travelapp.itinerary.dto.ItineraryStatusDTO;
import com.travelapp.itinerary.dto.RegenerateRequestDTO;
import com.travelapp.itinerary.dto.RegenerateResultDTO;

import java.util.List;

public interface ItineraryService {

    /**
     * Kick off an itinerary generation for a customization Q&A session: insert a "generating" placeholder plan,
     * publish an async generation message, and return planId immediately without waiting for generation to finish.
     * Called by CustomizeServiceImpl.complete() when the Q&A flow completes.
     *
     * @return the newly inserted itinerary_plans.id
     */
    Long requestGeneration(Long sessionId, Long userId);

    /**
     * The actual generation logic: load context (including multi-destination segmentation and on-demand Amap POI
     * backfill) -> attempt the AI engine (validate day by day, falling back to the rule engine for days that don't
     * pass) -> persist to the database -> update plan status. Invoked asynchronously by
     * {@link com.travelapp.itinerary.mq.ItineraryGenerationListener}, not run on the HTTP request thread.
     *
     * @param basedOnPlanId the old plan id in a regenerate scenario, used to preserve items with isUserEdited=true
     *                      as much as possible; pass null on first generation
     */
    void generateContent(Long planId, Long sessionId, Long userId, Long basedOnPlanId);

    /** When an unexpected exception occurs during generation, mark the placeholder plan as generation-failed (status=4) so the frontend can detect it via polling. */
    void markFailed(Long planId);

    /** 5.1 GET /itinerary/plans/{id} -- only accessible by the plan's owning user */
    ItineraryPlanDetailDTO getDetail(Long userId, Long planId);

    /** 5.2 GET /itinerary/plans/{id}/status */
    ItineraryStatusDTO getStatus(Long userId, Long planId);

    /** 5.3 PATCH /itinerary/plans/{id}/items/{itemId} */
    AdjustItemResponseDTO adjustItem(Long userId, Long planId, Long itemId, AdjustItemRequestDTO request);

    /** 5.4 GET /itinerary/plans/{id}/items/{itemId}/alternatives */
    List<AlternativeDTO> alternatives(Long userId, Long planId, Long itemId);

    /** 5.5 POST /itinerary/plans/{id}/regenerate -- insert a new-version placeholder plan + publish an async message, return status=generating immediately */
    RegenerateResultDTO regenerate(Long userId, Long planId, RegenerateRequestDTO request);

    /** 5.6 GET /users/me/itinerary-plans */
    PageResult<ItineraryPlanSummaryDTO> listMyPlans(Long userId, long page, long pageSize);

    /** 5.7 GET /itinerary/plans/{id}/export -- returns a PDF byte stream */
    byte[] exportPdf(Long userId, Long planId);
}
