package org.example;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class RedisStorageMonitor {
    /*
     * Original simplified logic (Standalone-only assumption)
     *
     * This version assumed Redis always returned flat keys like:
     *   used_memory
     *   maxmemory
     *
     * It did not account for cluster mode where keys are prefixed
     * per node (e.g., node1.used_memory).
     *
     * public Integer getStoragePercentage() {
     *
     *     long usedMemory;
     *     long maxMemory;
     *
     *     try {
     *         Properties memoryInfo = redisTemplate
     *             .getRequiredConnectionFactory()
     *             .getConnection()
     *             .info("memory");
     *
     *         usedMemory = Long.parseLong(
     *             memoryInfo.getProperty("used_memory", "0"));
     *
     *         maxMemory = Long.parseLong(
     *             memoryInfo.getProperty("maxmemory", "0"));
     *
     *     } catch (Exception e) {
     *         return 0;
     *     }
     *
     *     if (maxMemory == 0) return 0;
     *     if (usedMemory > maxMemory) return 100;
     *
     *     return (int)(usedMemory * 100 / maxMemory);
     * }
     *
     * Limitation:
     * In cluster mode, this logic failed because Redis returns
     * prefixed keys per node (e.g., node1.used_memory),
     * causing threshold checks to never trigger correctly.
     */

    public int getStoragePercentage(Properties memoryInfo) {

        // Standalone mode
        String directUsedMemory = memoryInfo.getProperty("used_memory");
        if (directUsedMemory != null) {
            long usedMemory = Long.parseLong(directUsedMemory);
            long maxMemory = Long.parseLong(memoryInfo.getProperty("maxmemory", "0"));
            return calculateMemoryPercentage(usedMemory, maxMemory);
        }

        // Cluster mode
        Map<String, Long> usedMemoryMap = new HashMap<>();
        Map<String, Long> maxMemoryMap = new HashMap<>();

        for (String key : memoryInfo.stringPropertyNames()) {
            if (key.endsWith(".used_memory")) {
                String prefix = key.substring(0, key.length() - ".used_memory".length());
                usedMemoryMap.put(prefix, Long.parseLong(memoryInfo.getProperty(key, "0")));
            } else if (key.endsWith(".maxmemory")) {
                String prefix = key.substring(0, key.length() - ".maxmemory".length());
                maxMemoryMap.put(prefix, Long.parseLong(memoryInfo.getProperty(key, "0")));
            }
        }

        int maxPercentage = 0;

        for (String prefix : usedMemoryMap.keySet()) {
            long used = usedMemoryMap.get(prefix);
            long max = maxMemoryMap.getOrDefault(prefix, 0L);

            int percentage = calculateMemoryPercentage(used, max);
            maxPercentage = Math.max(maxPercentage, percentage);
        }

        return maxPercentage;
    }

    private int calculateMemoryPercentage(long usedMemory, long maxMemory) {
        if (maxMemory == 0) return 0;
        if (usedMemory > maxMemory) return 100;
        return (int) (usedMemory * 100 / maxMemory);
    }
}