package com.example.parking.dto;

import com.example.parking.entity.User;

public record UserResponse(Long id, String name, String balance) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), Money.format(user.getBalance()));
    }
}
