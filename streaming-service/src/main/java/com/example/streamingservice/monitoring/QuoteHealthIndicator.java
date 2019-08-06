package com.example.streamingservice.monitoring;

import com.example.streamingservice.event.QuoteEvent;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Service
public class QuoteHealthIndicator implements HealthIndicator {

    private RestTemplate template = new RestTemplate();

    @Value("${quotes.url}")
    private String url;

    @Timed(value = "quote_health_check", percentiles = {0.5, 0.95, 0.98})
    @Override
    public Health health() {
        try {
            QuoteEvent quote = template.getForObject(URI.create(url + "AAPL"), QuoteEvent.class);
            return Health.up().withDetail("response", quote).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}