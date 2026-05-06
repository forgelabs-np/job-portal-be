package com.jobportal.v1.util;

import com.jobportal.v1.dto.PageRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class Pages {

    /**
     * Convert Spring Data Page to PageRes
     */
    public static <T> PageRes<T> of(Page<T> page) {
        return PageRes.<T>builder()
                .content(page.getContent())
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    /**
     * Convert List to PageRes (without pagination)
     */
    public static <T> PageRes<T> of(List<T> content) {
        return PageRes.<T>builder()
                .content(content)
                .size(content != null ? content.size() : 0)
                .page(1)
                .totalPages(1)
                .totalElements(content != null ? (long) content.size() : 0L)
                .build();
    }

    /**
     * Create Pageable from page and size
     */
    public static Pageable toPageable(int page, int size) {
        return PageRequest.of(page, size);
    }

    /**
     * Create Pageable with validation
     */
    public static Pageable toPageable(Integer page, Integer size) {
        int pageNum = (page == null || page < 0) ? 0 : page;
        int pageSize = (size == null || size < 1) ? 20 : Math.min(size, 100);
        return PageRequest.of(pageNum, pageSize);
    }
}