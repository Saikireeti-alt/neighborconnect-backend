package com.neighborconnect.neighborconnect_backend.dto;

public record BookingRequest(Long listingId, Long requesterId, String message) {
}