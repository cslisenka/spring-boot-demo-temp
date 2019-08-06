package com.example.streamingservice.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class QuoteExecutorHealthIndicator implements HealthIndicator {

    @Autowired
    @Qualifier("quoteExecutor")
    private ThreadPoolTaskExecutor executor;

    @Override
    public Health health() {
        int queueSize = executor.getThreadPoolExecutor().getQueue().size();
        if (queueSize < 20) {
            return Health.up().withDetail("queueSize", queueSize).build();
        } else {
            return Health.down().withDetail("queueSize", queueSize).build();
        }
    }
}