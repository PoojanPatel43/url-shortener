package com.urlshortener.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String currentPassword;

    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String newPassword;
}
