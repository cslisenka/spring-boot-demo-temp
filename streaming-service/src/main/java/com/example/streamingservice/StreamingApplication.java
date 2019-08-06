package com.example.streamingservice;

import com.example.streamingservice.event.QuoteEvent;
import com.example.streamingservice.event.QuoteReloadEvent;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.TimedAspect;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;

@Slf4j
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
@SpringBootApplication
public class StreamingApplication {

	@Value("${quotes.url}")
	private String url;

	@Autowired
	private ApplicationEventPublisher eventBus;

	@Autowired
	private ReactiveWebSocketHandler handler;

	private final RestTemplate template = new RestTemplate();
	private final AtomicReference<Set<String>> symbols = new AtomicReference<>(new HashSet<>());

	public static void main(String[] args) {
		SpringApplication.run(StreamingApplication.class, args);
	}

	@Scheduled(fixedRate = 1000)
	public void reloadQuotes() {
		log.info("reloading quotes...");
		symbols.get().stream()
			.map(QuoteReloadEvent::new)
			.forEach(eventBus::publishEvent);
	}

	@Async("dataPushExecutor")
	@EventListener
	public void onQuoteUpdate(QuoteEvent e) {
		log.info("sending update to clients {}", e);
		handler.getConnections().forEach((id, processor) -> {
			log.debug("sending {}/{} to {}", id, e.getSymbol(), e.getPrice());
			processor.onNext(e);
		});
	}

	@Async("quoteExecutor")
	@EventListener
	public void onApplicationLoad(ApplicationReadyEvent e) {
		Set<String> setOfSymbols = new HashSet<>(Arrays.asList("AAPL", "IBM", "EPAM", "TSLA", "ORCL", "GM"));
		symbols.set(setOfSymbols);
		log.info("loaded set of sumbols {}", setOfSymbols);
	}

	@Async("quoteExecutor")
	@Timed(value = "quote_load", percentiles = {0.5, 0.95, 0.98})
	@EventListener
	public void onQuoteReloadEvent(QuoteReloadEvent e) {
		QuoteEvent quote = template.getForObject(URI.create(url + e.getSymbol()), QuoteEvent.class);
		// Show how to enable debug logging in runtime
		log.info("got quote {}", e.getSymbol());
		eventBus.publishEvent(quote);
	}

	@Bean(name = "dataPushExecutor")
	public ThreadPoolTaskExecutor dataPushExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setQueueCapacity(100);
		executor.setMaxPoolSize(1);
		return executor;
	}

	@Bean(name = "quoteExecutor")
	public ThreadPoolTaskExecutor quoteExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(10);
		executor.setQueueCapacity(10_000);
		executor.setMaxPoolSize(10);
		return executor;
	}

	@Bean
	TimedAspect timed() {
		return new TimedAspect();
	}

	@Bean
	public HandlerMapping webSocketHandlerMapping(ReactiveWebSocketHandler webSocketHandler) {
		Map<String, WebSocketHandler> map = new HashMap<>();
		map.put("/websocket", webSocketHandler);

		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(1);
		handlerMapping.setUrlMap(map);
		return handlerMapping;
	}

	@Bean
	public WebSocketHandlerAdapter handlerAdapter() {
		return new WebSocketHandlerAdapter();
	}

	@Slf4j
	@Service
	public static class ReactiveWebSocketHandler implements WebSocketHandler {

		private final ConcurrentHashMap<String, UnicastProcessor<QuoteEvent>> connections = new ConcurrentHashMap<>();

		@Override
		public Mono<Void> handle(WebSocketSession webSocketSession) {
			log.info("client connected {}", webSocketSession.getId(), webSocketSession.getHandshakeInfo().getRemoteAddress());
			UnicastProcessor<QuoteEvent> processor = UnicastProcessor.create();
			connections.put(webSocketSession.getId(), processor);

			return webSocketSession.send(processor
					.map(event -> webSocketSession.textMessage(event.toString())));
		}

		public ConcurrentHashMap<String, UnicastProcessor<QuoteEvent>> getConnections() {
			return connections;
		}
	}

	// Exposing health status to Prometheus
	@Bean
	MeterRegistryCustomizer	healthToPrometheus(HealthEndpoint healthEndpoint) {
		return registry -> registry.gauge("health", emptyList(), healthEndpoint,
			ep -> ep.health().getStatus().equals(Status.UP) ? 1 : 0);
	}
}