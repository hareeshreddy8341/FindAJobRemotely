package com.findajobremotely.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public final class CommonDtos {

    private CommonDtos() {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

        public static <T> PageResponse<T> of(Page<T> p) {
            return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }

    public record MessageResponse(String message) {
    }
}
