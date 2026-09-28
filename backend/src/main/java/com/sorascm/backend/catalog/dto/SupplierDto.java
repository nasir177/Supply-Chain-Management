package com.sorascm.backend.catalog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class SupplierDto {
    public record CreateRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            String contactName,
            @NotBlank @Email @Size(max = 128) String email,
            String phone,
            @NotBlank String addressLine1,
            String addressLine2,
            @NotBlank @Size(max = 64) String city,
            @NotBlank @Size(min = 2, max = 2) String countryCode
    ) {}

    public record Response(
            UUID id,
            String code,
            String name,
            String contactName,
            String email,
            String phone,
            String city,
            String countryCode,
            boolean active
    ) {}
}