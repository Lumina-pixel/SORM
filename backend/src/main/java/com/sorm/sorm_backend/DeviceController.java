package com.sorm.sorm_backend;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/devices")
@CrossOrigin(origins = "*")
public class DeviceController {

    private static final long ONLINE_WINDOW_SECONDS = 60;

    private final DeviceRepository deviceRepository;
    private final MetricRepository metricRepository;

    public DeviceController(DeviceRepository deviceRepository, MetricRepository metricRepository) {
        this.deviceRepository = deviceRepository;
        this.metricRepository = metricRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
        Optional<Device> existing = deviceRepository.findByHostname(req.hostname());
        if (existing.isPresent()) {
            return ResponseEntity.ok(new RegisterResponse(existing.get().getId(), "already registered"));
        }

        Device d = new Device();
        d.setHostname(req.hostname());
        d.setIpAddress(req.ipAddress());
        d.setOsName(req.osName());
        d.setTotalRamMb(req.totalRamMb());
        d.setCpuCores(req.cpuCores());
        d.setRegisteredAt(LocalDateTime.now());
        d.setLastSeenAt(LocalDateTime.now());
        Device saved = deviceRepository.save(d);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(saved.getId(), "registered"));
    }

    @GetMapping
    public List<DeviceStatusResponse> listDevices() {
        LocalDateTime now = LocalDateTime.now();

        return deviceRepository.findAll().stream().map(d -> {
            boolean online = d.getLastSeenAt() != null
                    && Duration.between(d.getLastSeenAt(), now).getSeconds() <= ONLINE_WINDOW_SECONDS;

            Optional<Metric> latest = metricRepository.findFirstByDeviceIdOrderByRecordedAtDesc(d.getId());

            return new DeviceStatusResponse(
                    d.getId(),
                    d.getHostname(),
                    d.getIpAddress(),
                    online ? "ONLINE" : "OFFLINE",
                    d.getLastSeenAt(),
                    latest.map(Metric::getCpuUsage).orElse(null),
                    latest.map(Metric::getRamUsage).orElse(null),
                    latest.map(Metric::getDiskUsage).orElse(null));
        }).toList();
    }

    @GetMapping("/{deviceId}/metrics")
    public List<MetricResponse> history(@PathVariable Integer deviceId,
                                        @RequestParam(defaultValue = "50") int limit) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown deviceId");
        }
        int safeLimit = Math.max(1, Math.min(limit, 500));

        return metricRepository
                .findByDeviceIdOrderByRecordedAtDesc(deviceId, PageRequest.of(0, safeLimit))
                .stream()
                .map(m -> new MetricResponse(m.getDeviceId(), m.getCpuUsage(),
                        m.getRamUsage(), m.getDiskUsage(), m.getRecordedAt()))
                .toList();
    }
}