package com.travelapp.itinerary.mq;

import com.travelapp.itinerary.service.ItineraryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link ItineraryGenerationMessage} and calls ItineraryService.generateContent to perform the actual
 * time-consuming generation work.
 * The DB writes inside generateContent run in their own independent transaction; this class is only responsible
 * for catching generation failures and calling markFailed to mark the placeholder plan as failed, so that the
 * consumer thread doesn't let an exception bounce the message back onto the queue for redelivery (under the
 * default ack mode, an uncaught exception causes the message to be requeued, which can easily lead to an
 * infinite loop).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItineraryGenerationListener {

    private final ItineraryService itineraryService;

    @RabbitListener(queues = ItineraryMqConfig.QUEUE)
    public void onMessage(ItineraryGenerationMessage message) {
        try {
            itineraryService.generateContent(message.planId(), message.sessionId(), message.userId(), message.basedOnPlanId());
        } catch (Exception e) {
            log.error("Itinerary generation task failed: planId={}, sessionId={}", message.planId(), message.sessionId(), e);
            try {
                itineraryService.markFailed(message.planId());
            } catch (Exception markException) {
                log.error("Also failed to mark itinerary plan as generation-failed: planId={}", message.planId(), markException);
            }
        }
    }
}
