package com.neighborconnect.neighborconnect_backend.dto;

import com.neighborconnect.neighborconnect_backend.model.Listing;

import java.time.LocalDateTime;

public record ListingResponse(Long id, String title, String description, String category, String type,
        String priceInfo, String neighborhood, String status,
        Long ownerId, String ownerName, LocalDateTime createdAt) {

    public static ListingResponse from(Listing l) {
        return new ListingResponse(l.getId(), l.getTitle(), l.getDescription(), l.getCategory(),
                l.getType().name(), l.getPriceInfo(), l.getNeighborhood(), l.getStatus().name(),
                l.getOwner().getId(), l.getOwner().getName(), l.getCreatedAt());
    }
}