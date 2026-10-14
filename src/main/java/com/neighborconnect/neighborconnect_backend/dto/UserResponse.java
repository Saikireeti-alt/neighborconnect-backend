package com.neighborconnect.neighborconnect_backend.dto;

import com.neighborconnect.neighborconnect_backend.model.User;

public record UserResponse(Long id, String name, String email, String phone, String neighborhood, String role) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                u.getNeighborhood(), u.getRole().name());
    }
}