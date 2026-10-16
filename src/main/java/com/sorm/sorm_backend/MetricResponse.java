package com.sorm.sorm_backend;

import java.time.LocalDateTime;

public record MetricResponse(
        Integer deviceId,
        Double cpuUsage,
        Double ramUsage,
        Double diskUsage,
        LocalDateTime timestamp) {}