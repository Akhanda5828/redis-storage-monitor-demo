package org.example.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
public class RedisStorageMonitorService {

    private static final int THRESHOLD_PERCENT = 10;

    private final RedisDatabaseService databaseService;

    public RedisStorageMonitorService(RedisDatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public StorageStatus checkStorageStatus() {

        Properties memoryInfo = databaseService.getMemoryInfo();

        if (memoryInfo == null || memoryInfo.isEmpty()) {
            return new StorageStatus(0, false, false);
        }

        /*
         * -------------------------------
         * STANDALONE MODE LOGIC
         * -------------------------------
         *
         * In standalone Redis deployment,
         * INFO memory returns flat keys:
         *
         * used_memory
         * maxmemory
         *
         * Example:
         * used_memory=2097152
         * maxmemory=52428800
         *
         * If these keys exist directly,
         * we compute percentage normally.
         */

        if (memoryInfo.containsKey("used_memory") &&
                memoryInfo.containsKey("maxmemory")) {

            long usedMemory = Long.parseLong(
                    memoryInfo.getProperty("used_memory", "0"));

            long maxMemory = Long.parseLong(
                    memoryInfo.getProperty("maxmemory", "0"));

            int percentage = calculateMemoryPercentage(usedMemory, maxMemory);

            boolean thresholdExceeded = percentage >= THRESHOLD_PERCENT;

            return new StorageStatus(percentage, true, thresholdExceeded);
        }

        /*
         * ----------------------------------
         * CLUSTER MODE LOGIC
         * ----------------------------------
         *
         * In Redis Cluster deployments,
         * INFO memory may prefix keys per node.
         *
         * Example:
         *
         * 10.0.0.1:6379.used_memory=2097152
         * 10.0.0.1:6379.maxmemory=52428800
         * 10.0.0.2:6379.used_memory=1048576
         * 10.0.0.2:6379.maxmemory=52428800
         *
         * In this case:
         * - Each node has independent memory stats
         * - We must compute percentage per node
         * - We return the MAX utilization across nodes
         *
         * Rationale:
         * The most saturated shard is the bottleneck.
         * Averaging would hide hotspots.
         */

        Map<String, Long> usedMemoryMap = new HashMap<>();
        Map<String, Long> maxMemoryMap = new HashMap<>();

        for (String key : memoryInfo.stringPropertyNames()) {

            if (key.endsWith(".used_memory")) {

                String prefix = key.substring(
                        0, key.length() - ".used_memory".length());

                usedMemoryMap.put(
                        prefix,
                        Long.parseLong(memoryInfo.getProperty(key, "0"))
                );

            } else if (key.endsWith(".maxmemory")) {

                String prefix = key.substring(
                        0, key.length() - ".maxmemory".length());

                maxMemoryMap.put(
                        prefix,
                        Long.parseLong(memoryInfo.getProperty(key, "0"))
                );
            }
        }

        int maxClusterPercentage = 0;

        for (String node : usedMemoryMap.keySet()) {

            long used = usedMemoryMap.get(node);
            long max = maxMemoryMap.getOrDefault(node, 0L);

            int percentage = calculateMemoryPercentage(used, max);

            maxClusterPercentage = Math.max(maxClusterPercentage, percentage);
        }

        boolean thresholdExceeded =
                maxClusterPercentage >= THRESHOLD_PERCENT;

        return new StorageStatus(
                maxClusterPercentage,
                true,
                thresholdExceeded
        );
    }

    private int calculateMemoryPercentage(long usedMemory, long maxMemory) {
        if (maxMemory == 0) return 0;
        if (usedMemory > maxMemory) return 100;
        return (int) ((usedMemory * 100) / maxMemory);
    }

    public record StorageStatus(
            int percentage,
            boolean redisAvailable,
            boolean thresholdExceeded
    ) {}
}