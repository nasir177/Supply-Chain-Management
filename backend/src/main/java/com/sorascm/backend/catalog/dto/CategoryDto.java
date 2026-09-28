package com.sorascm.backend.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDto {
    public record CreateRequest(

            @NotBlank @Size(max = 128) String name,
            @NotBlank @Size(max = 64) String code,
            String description,
            Long parentId
    ) {}

    public record Response(

            Long id,
            String name,
            String code,
            String description,
            Long ParentId,
            boolean active

    ) {}
}
