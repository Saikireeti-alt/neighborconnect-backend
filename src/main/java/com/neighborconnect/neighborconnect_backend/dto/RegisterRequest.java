package com.neighborconnect.neighborconnect_backend.dto;

public record RegisterRequest(String name, String email, String password, String phone, String neighborhood) {
}