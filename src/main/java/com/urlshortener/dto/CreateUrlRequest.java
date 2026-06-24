package com.urlshortener.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUrlRequest {

    @NotBlank(message = "URL is required")
    @URL(message = "Invalid URL format")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String url;

    @Size(min = 3, max = 20, message = "Custom alias must be between 3 and 20 characters")
    private String customAlias;

    @Positive(message = "Expiration days must be a positive number")
    @Max(value = 3650, message = "Expiration days must not exceed 3650 (10 years)")
    private Integer expirationDays;
}
