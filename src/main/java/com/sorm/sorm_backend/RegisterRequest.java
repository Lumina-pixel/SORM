package com.sorm.sorm_backend;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String hostname,
        String ipAddress,
        String osName,
        Integer totalRamMb,
        Integer cpuCores) {}