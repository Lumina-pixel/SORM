package com.sorm.sorm_backend;

import java.time.LocalDateTime;

public record DeviceStatusResponse(
        Integer deviceId,
        String hostname,
        String ipAddress,
        String status,
        LocalDateTime lastSeenAt,
        Double cpuUsage,
        Double ramUsage,
        Double diskUsage) {}