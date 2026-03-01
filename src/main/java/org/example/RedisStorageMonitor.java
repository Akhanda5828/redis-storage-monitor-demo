package org.example;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class RedisStorageMonitor {

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