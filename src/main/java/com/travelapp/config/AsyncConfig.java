package com.travelapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables @Async support; currently the only use case is asynchronously incrementing view_count on the travel note detail page (see NoteViewCountService) without blocking the response. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
