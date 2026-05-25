package com.gabaritaplus.api.util;

import com.gabaritaplus.api.dto.common.PageMetadata;
import org.springframework.data.domain.Page;

public final class PageUtils {

    private PageUtils() {
    }

    public static PageMetadata metadata(Page<?> page) {
        return new PageMetadata(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
