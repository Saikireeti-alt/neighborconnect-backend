package com.neighborconnect.neighborconnect_backend.dto;

import com.neighborconnect.neighborconnect_backend.model.Booking;

import java.time.LocalDateTime;

public record BookingResponse(Long id, Long listingId, String listingTitle, Long requesterId,
        String requesterName, String message, String status, LocalDateTime createdAt) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getListing().getId(), b.getListing().getTitle(),
                b.getRequester().getId(), b.getRequester().getName(), b.getMessage(),
                b.getStatus().name(), b.getCreatedAt());
    }
}