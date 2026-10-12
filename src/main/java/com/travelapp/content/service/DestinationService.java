package com.travelapp.content.service;

import com.travelapp.content.dto.DestinationSummaryDTO;

import java.util.List;

public interface DestinationService {

    List<DestinationSummaryDTO> search(String keyword, int limit);

    List<DestinationSummaryDTO> hot(int limit);
}
