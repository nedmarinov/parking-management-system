package com.example.parking.service;

import com.example.parking.dto.UserResponse;
import com.example.parking.exception.ApiException;
import com.example.parking.exception.ErrorCode;
import com.example.parking.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return users.findAllByOrderByNameAscIdAsc().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return users.findById(userId).map(UserResponse::from).orElseThrow(UserService::userNotFound);
    }

    static ApiException userNotFound() {
        return new ApiException(ErrorCode.USER_NOT_FOUND, "User not found");
    }
}
