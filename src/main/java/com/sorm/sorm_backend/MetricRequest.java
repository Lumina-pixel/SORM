package com.sorm.sorm_backend;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record MetricRequest(
        @NotNull Integer deviceId,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double cpuUsage,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double ramUsage,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double diskUsage,
        LocalDateTime timestamp) {}