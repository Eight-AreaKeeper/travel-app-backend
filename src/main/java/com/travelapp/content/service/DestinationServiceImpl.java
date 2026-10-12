package com.travelapp.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelapp.content.dto.DestinationSummaryDTO;
import com.travelapp.content.entity.Destination;
import com.travelapp.content.mapper.DestinationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DestinationServiceImpl implements DestinationService {

    private final DestinationMapper destinationMapper;

    @Override
    public List<DestinationSummaryDTO> search(String keyword, int limit) {
        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<Destination>()
                .like(Destination::getName, keyword)
                .or()
                .like(Destination::getNamePinyin, keyword)
                .orderByDesc(Destination::getPopularityScore);

        Page<Destination> page = destinationMapper.selectPage(new Page<>(1, limit), wrapper);
        return page.getRecords().stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Override
    public List<DestinationSummaryDTO> hot(int limit) {
        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<Destination>()
                .orderByDesc(Destination::getPopularityScore);

        Page<Destination> page = destinationMapper.selectPage(new Page<>(1, limit), wrapper);
        return page.getRecords().stream().map(this::toSummary).collect(Collectors.toList());
    }

    private DestinationSummaryDTO toSummary(Destination destination) {
        return DestinationSummaryDTO.builder()
                .id(destination.getId())
                .name(destination.getName())
                .type(destination.getType())
                .coverImage(destination.getCoverImage())
                .build();
    }
}
