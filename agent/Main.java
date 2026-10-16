import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.management.OperatingSystemMXBean;

/**
 * SORM Agent - Member 1
 * Collects device information and CPU/RAM/disk usage, registers the device,
 * and sends metrics to the SORM backend every 10 seconds.
 *
 * Expected backend endpoints:
 *   POST http://localhost:8080/api/devices/register
 *   POST http://localhost:8080/api/metrics
 */
public class Main {
private static final String BACKEND_BASE_URL =
        System.getProperty("sorm.backend", "http://localhost:8080");    
        private static final String REGISTER_URL =
            BACKEND_BASE_URL + "/api/devices/register";
    private static final String METRICS_URL =
            BACKEND_BASE_URL + "/api/metrics";
    private static final long SEND_INTERVAL_MS = 10_000;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static void main(String[] args) {
        OperatingSystemMXBean os =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        String hostname = getHostname();
        String ipAddress = getIpAddress();
        String osName = System.getProperty("os.name", "Unknown");
        int cpuCores = Runtime.getRuntime().availableProcessors();
        long totalRamMb = os.getTotalMemorySize() / (1024L * 1024L);

        System.out.println("================================");
        System.out.println("        SORM AGENT v1.0");
        System.out.println("================================");
        System.out.println("Agent Status: RUNNING");
        System.out.println();
        System.out.println("------- DEVICE INFO -------");
        System.out.println("Hostname    : " + hostname);
        System.out.println("IP Address  : " + ipAddress);
        System.out.println("Operating OS: " + osName);
        System.out.println("Total RAM   : " + totalRamMb + " MB");
        System.out.println("CPU Cores   : " + cpuCores);
        System.out.println("---------------------------");
        System.out.println("Backend     : " + BACKEND_BASE_URL);
        System.out.println("Send interval: 10 seconds");
        System.out.println();

        // The first CPU reading can be invalid. Take a warm-up reading.
        os.getCpuLoad();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        int deviceId = -1;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (deviceId < 0) {
                    deviceId = registerDevice(
                            hostname, ipAddress, osName, totalRamMb, cpuCores);
                    if (deviceId < 0) {
                        System.out.println(
                                "[INFO] Backend not ready; will retry registration in 10 seconds.");
                        sleepBeforeNextCycle();
                        continue;
                    }
                    System.out.println("[OK] Device registered. deviceId=" + deviceId);
                }

                double cpuUsage = getCpuUsage(os);
                double ramUsage = getRamUsage(os);
                double diskUsage = getDiskUsage();

                System.out.println();
                System.out.println("----------- SYSTEM ------------");
                System.out.printf(Locale.US, "CPU Usage  : %.2f%%%n", cpuUsage);
                System.out.printf(Locale.US, "RAM Usage  : %.2f%%%n", ramUsage);
                System.out.printf(Locale.US, "Disk Usage : %.2f%%%n", diskUsage);
                System.out.println("Sending metrics to backend...");

                boolean sent = sendMetrics(deviceId, cpuUsage, ramUsage, diskUsage);
                if (sent) {
                    System.out.println("[OK] Metrics sent successfully.");
                } else {
                    System.out.println(
                            "[WARN] Metrics were not sent. Will try again next cycle.");
                }
            } catch (Exception e) {
                // Keep the agent alive if an unexpected cycle-level error occurs.
                System.out.println("[WARN] Monitoring cycle failed: " + e.getMessage());
            }

            sleepBeforeNextCycle();
        }
    }

    private static int registerDevice(
            String hostname, String ipAddress, String osName,
            long totalRamMb, int cpuCores) {
        String json = String.format(
                Locale.US,
                "{\"hostname\":\"%s\",\"ipAddress\":\"%s\",\"osName\":\"%s\","
                        + "\"totalRamMb\":%d,\"cpuCores\":%d}",
                jsonEscape(hostname), jsonEscape(ipAddress), jsonEscape(osName),
                totalRamMb, cpuCores);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(REGISTER_URL))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.out.println("[WARN] Registration HTTP status: "
                        + response.statusCode());
                return -1;
            }

            Matcher matcher = Pattern.compile(
                    "\"deviceId\"\\s*:\\s*(\\d+)")
                    .matcher(response.body());

            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }

            System.out.println(
                    "[WARN] Registration response did not contain deviceId: "
                            + response.body());
        } catch (IOException e) {
            System.out.println("[INFO] Cannot reach backend for registration: "
                    + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("[WARN] Registration failed: " + e.getMessage());
        }
        return -1;
    }

    private static boolean sendMetrics(
            int deviceId, double cpuUsage, double ramUsage, double diskUsage) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String json = String.format(
                Locale.US,
                "{\"deviceId\":%d,\"cpuUsage\":%.2f,\"ramUsage\":%.2f,"
                        + "\"diskUsage\":%.2f,\"timestamp\":\"%s\"}",
                deviceId, cpuUsage, ramUsage, diskUsage, timestamp);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(METRICS_URL))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }

            System.out.println("[WARN] Metrics HTTP status: "
                    + response.statusCode() + " | " + response.body());
        } catch (IOException e) {
            System.out.println("[WARN] Cannot reach backend for metrics: "
                    + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("[WARN] Sending metrics failed: " + e.getMessage());
        }
        return false;
    }

    private static double getCpuUsage(OperatingSystemMXBean os) {
        double load = os.getCpuLoad();
        if (Double.isNaN(load) || load < 0) {
            return 0.0;
        }
        return clampPercent(load * 100.0);
    }

    private static double getRamUsage(OperatingSystemMXBean os) {
        long total = os.getTotalMemorySize();
        long free = os.getFreeMemorySize();
        if (total <= 0) {
            return 0.0;
        }
        return clampPercent((1.0 - ((double) free / total)) * 100.0);
    }

    private static double getDiskUsage() {
        // Use the Windows C: drive when present; fall back to the system root.
        File disk = new File("C:\\");
        if (!disk.exists()) {
            disk = new File(System.getProperty("user.dir", "."));
        }
        long total = disk.getTotalSpace();
        long free = disk.getUsableSpace();
        if (total <= 0) {
            return 0.0;
        }
        return clampPercent((1.0 - ((double) free / total)) * 100.0);
    }

    private static double clampPercent(double value) {
        if (Double.isNaN(value) || value < 0) {
            return 0.0;
        }
        return Math.min(value, 100.0);
    }

    private static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private static String getIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static void sleepBeforeNextCycle() {
        try {
            Thread.sleep(SEND_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
