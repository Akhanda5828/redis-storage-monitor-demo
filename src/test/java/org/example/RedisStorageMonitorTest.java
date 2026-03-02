package org.example;

import org.example.service.RedisDatabaseService;
import org.example.service.RedisStorageMonitorService;
import org.example.service.RedisStorageMonitorService.StorageStatus;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RedisStorageMonitorTest {

    @Test
    void shouldReturnHealthyWhenBelowThreshold() {

        RedisDatabaseService mockDb = mock(RedisDatabaseService.class);

        Properties props = new Properties();
        props.setProperty("used_memory", "2000000");     // ~2MB
        props.setProperty("maxmemory", "50000000");      // 50MB

        when(mockDb.getMemoryInfo()).thenReturn(props);

        RedisStorageMonitorService service =
                new RedisStorageMonitorService(mockDb);

        StorageStatus status = service.checkStorageStatus();

        assertTrue(status.redisAvailable());
        assertFalse(status.thresholdExceeded());
        assertTrue(status.percentage() > 0);
    }

    @Test
    void shouldReturnThresholdExceeded() {

        RedisDatabaseService mockDb = mock(RedisDatabaseService.class);

        Properties props = new Properties();
        props.setProperty("used_memory", "6000000");     // 6MB
        props.setProperty("maxmemory", "50000000");      // 50MB

        when(mockDb.getMemoryInfo()).thenReturn(props);

        RedisStorageMonitorService service =
                new RedisStorageMonitorService(mockDb);

        StorageStatus status = service.checkStorageStatus();

        assertTrue(status.thresholdExceeded());
    }

    /*
     * -------------------------------------------------------
     * CLUSTER MODE TEST
     * -------------------------------------------------------
     *
     * In Redis Cluster deployments, INFO memory may return
     * prefixed keys per node instead of flat keys.
     *
     * Example:
     *
     * 10.0.0.1:6379.used_memory
     * 10.0.0.1:6379.maxmemory
     * 10.0.0.2:6379.used_memory
     * 10.0.0.2:6379.maxmemory
     *
     * The service logic:
     *  - Detects suffix ".used_memory" and ".maxmemory"
     *  - Groups metrics by node prefix
     *  - Calculates per-node percentage
     *  - Returns the MAX utilization across nodes
     *
     * This test simulates two nodes:
     *
     * Node1: 20MB / 50MB  = 40%
     * Node2: 1MB  / 50MB  = 2%
     *
     * Expected result: 40% (max node utilization)
     */

    @Test
    void shouldHandleClusterPrefixedKeysAndReturnMaxNodeUsage() {

        RedisDatabaseService mockDb = mock(RedisDatabaseService.class);

        Properties props = new Properties();

        // Node 1 (high usage - should dominate)
        props.setProperty("10.0.0.1:6379.used_memory", "20000000");  // 20MB
        props.setProperty("10.0.0.1:6379.maxmemory", "50000000");    // 50MB

        // Node 2 (low usage)
        props.setProperty("10.0.0.2:6379.used_memory", "1000000");   // 1MB
        props.setProperty("10.0.0.2:6379.maxmemory", "50000000");    // 50MB

        when(mockDb.getMemoryInfo()).thenReturn(props);

        RedisStorageMonitorService service =
                new RedisStorageMonitorService(mockDb);

        StorageStatus status = service.checkStorageStatus();

        // Expect max node usage (~40%)
        assertTrue(status.redisAvailable());
        assertTrue(status.percentage() >= 40);
        assertTrue(status.thresholdExceeded());  // because threshold = 10%
    }
}