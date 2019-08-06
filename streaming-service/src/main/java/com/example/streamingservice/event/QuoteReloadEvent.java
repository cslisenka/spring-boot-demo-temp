package com.example.streamingservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Data
@RequiredArgsConstructor
public class QuoteReloadEvent {

    private final String symbol;
}