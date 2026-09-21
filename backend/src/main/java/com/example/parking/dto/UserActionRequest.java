package com.example.parking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Body for actions on a session that name the acting demo user. */
public record UserActionRequest(@NotNull @Positive Long userId) {
}
