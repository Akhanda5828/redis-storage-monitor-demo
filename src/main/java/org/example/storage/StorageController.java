package org.example.storage;

import org.example.service.RedisStorageMonitorService;
import org.example.service.RedisStorageMonitorService.StorageStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/storage")
public class StorageController {

    private final RedisStorageMonitorService monitorService;

    public StorageController(RedisStorageMonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStorage() {

        try {

            StorageStatus status = monitorService.checkStorageStatus();

            if (!status.redisAvailable()) {
                return ResponseEntity
                        .status(503)
                        .body(Map.of(
                                "status", "REDIS_UNAVAILABLE",
                                "storagePercentage", 0
                        ));
            }

            if (status.thresholdExceeded()) {
                return ResponseEntity
                        .status(429)
                        .body(Map.of(
                                "status", "THRESHOLD_EXCEEDED",
                                "storagePercentage", status.percentage()
                        ));
            }

            return ResponseEntity.ok(
                    Map.of(
                            "status", "HEALTHY",
                            "storagePercentage", status.percentage()
                    )
            );

        } catch (Exception e) {
            return ResponseEntity
                    .status(503)
                    .body(Map.of(
                            "status", "REDIS_UNAVAILABLE",
                            "storagePercentage", 0
                    ));
        }
    }
}