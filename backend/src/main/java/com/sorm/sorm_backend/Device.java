package com.sorm.sorm_backend;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String hostname;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "os_name")
    private String osName;

    @Column(name = "total_ram_mb")
    private Integer totalRamMb;

    @Column(name = "cpu_cores")
    private Integer cpuCores;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    public Integer getId() { return id; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getOsName() { return osName; }
    public void setOsName(String osName) { this.osName = osName; }
    public Integer getTotalRamMb() { return totalRamMb; }
    public void setTotalRamMb(Integer totalRamMb) { this.totalRamMb = totalRamMb; }
    public Integer getCpuCores() { return cpuCores; }
    public void setCpuCores(Integer cpuCores) { this.cpuCores = cpuCores; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}