package com.travelapp.content.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travelapp.content.entity.TravelNote;
import com.travelapp.content.mapper.TravelNoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Split out into its own class because @Async relies on a Spring AOP proxy to take effect; calling a method
 * on the same instance (this.xxx()) bypasses the proxy, silently turning @Async into a synchronous call --
 * it only runs asynchronously on a genuine cross-bean call.
 * Corresponds to API Documentation 3.4: "Accessing this endpoint asynchronously triggers view_count+1 without blocking the response."
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteViewCountService {

    private final TravelNoteMapper travelNoteMapper;

    @Async
    public void increment(Long noteId) {
        try {
            travelNoteMapper.update(null, new LambdaUpdateWrapper<TravelNote>()
                    .eq(TravelNote::getId, noteId).setSql("view_count = view_count + 1"));
        } catch (Exception e) {
            log.warn("Failed to increment travel note view count, ignoring: noteId={}", noteId, e);
        }
    }
}
