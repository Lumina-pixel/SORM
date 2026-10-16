package com.sorm.sorm_backend;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class MetricController {

    private final MetricRepository metricRepository;
    private final DeviceRepository deviceRepository;

    public MetricController(MetricRepository metricRepository, DeviceRepository deviceRepository) {
        this.metricRepository = metricRepository;
        this.deviceRepository = deviceRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> receive(@Valid @RequestBody MetricRequest req) {
        Device device = deviceRepository.findById(req.deviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown deviceId"));

        Metric m = new Metric();
        m.setDeviceId(req.deviceId());
        m.setCpuUsage(req.cpuUsage());
        m.setRamUsage(req.ramUsage());
        m.setDiskUsage(req.diskUsage());
        m.setRecordedAt(req.timestamp() != null ? req.timestamp() : LocalDateTime.now());
        metricRepository.save(m);

        device.setLastSeenAt(LocalDateTime.now());
        deviceRepository.save(device);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "saved"));
    }
}