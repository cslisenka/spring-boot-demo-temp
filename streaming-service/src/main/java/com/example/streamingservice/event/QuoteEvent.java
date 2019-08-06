package com.example.streamingservice.event;

import lombok.Data;

@Data
public class QuoteEvent {

    private String symbol;
    private int price;
}