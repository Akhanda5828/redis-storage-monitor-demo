package org.example;

import org.junit.jupiter.api.Test;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisStorageMonitorTest {

    @Test
    void shouldCalculateStandalonePercentage() {
        Properties props = new Properties();
        props.setProperty("used_memory", "50");
        props.setProperty("maxmemory", "100");

        RedisStorageMonitor monitor = new RedisStorageMonitor();
        int result = monitor.getStoragePercentage(props);

        assertEquals(50, result);
    }

    @Test
    void shouldCalculateClusterMaxPercentage() {
        Properties props = new Properties();
        props.setProperty("node1.used_memory", "80");
        props.setProperty("node1.maxmemory", "100");
        props.setProperty("node2.used_memory", "50");
        props.setProperty("node2.maxmemory", "100");

        RedisStorageMonitor monitor = new RedisStorageMonitor();
        int result = monitor.getStoragePercentage(props);

        assertEquals(80, result);
    }

    @Test
    void shouldReturnZeroWhenMaxMemoryZero() {
        Properties props = new Properties();
        props.setProperty("used_memory", "100");
        props.setProperty("maxmemory", "0");

        RedisStorageMonitor monitor = new RedisStorageMonitor();
        int result = monitor.getStoragePercentage(props);

        assertEquals(0, result);
    }
}