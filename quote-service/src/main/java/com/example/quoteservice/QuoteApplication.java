package com.example.quoteservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@Slf4j
@RestController
@SpringBootApplication
public class QuoteApplication {

	@Value("${delay:500}")
	private int delay;

	public static void main(String[] args) {
		SpringApplication.run(QuoteApplication.class, args);
	}

	@GetMapping("/quote")
	public QuoteDTO getQuote(String symbol) {
		log.info("/quote {}, delay {}", symbol, delay);

		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		}

		return new QuoteDTO(symbol, Math.abs(new Random().nextInt(500)));
	}

	@AllArgsConstructor
	@Data
	class QuoteDTO {
		private String symbol;
		private int price;
	}
}