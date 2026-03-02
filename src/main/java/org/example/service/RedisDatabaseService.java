package org.example.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class RedisDatabaseService {

    private final StringRedisTemplate redisTemplate;

    public RedisDatabaseService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Properties getMemoryInfo() {
        return redisTemplate
                .getConnectionFactory()
                .getConnection()
                .info("memory");
    }
}