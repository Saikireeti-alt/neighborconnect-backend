package com.neighborconnect.neighborconnect_backend.dto;

public record ListingRequest(String title, String description, String category, String type,
        String priceInfo, String neighborhood, Long ownerId) {
}