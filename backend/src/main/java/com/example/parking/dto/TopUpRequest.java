package com.example.parking.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** Kept as a raw JSON node so a numeric token cannot be coerced into the required string. */
public record TopUpRequest(JsonNode amount) {
}
