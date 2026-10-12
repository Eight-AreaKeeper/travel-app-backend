package com.travelapp.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/** Generic paginated response structure. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private long total;
    private long page;
    private long pageSize;
    private List<T> records;
}
