package com.neighborconnect.neighborconnect_backend.controller;

import com.neighborconnect.neighborconnect_backend.dto.BookingRequest;
import com.neighborconnect.neighborconnect_backend.dto.BookingResponse;
import com.neighborconnect.neighborconnect_backend.dto.StatusRequest;
import com.neighborconnect.neighborconnect_backend.model.Booking;
import com.neighborconnect.neighborconnect_backend.model.BookingStatus;
import com.neighborconnect.neighborconnect_backend.model.Listing;
import com.neighborconnect.neighborconnect_backend.model.User;
import com.neighborconnect.neighborconnect_backend.repository.BookingRepository;
import com.neighborconnect.neighborconnect_backend.repository.ListingRepository;
import com.neighborconnect.neighborconnect_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public BookingController(BookingRepository bookingRepository, ListingRepository listingRepository,
            UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<BookingResponse> getAll() {
        return bookingRepository.findAll().stream().map(BookingResponse::from).toList();
    }

    @GetMapping("/statuses")
    public List<String> getStatuses() {
        return Arrays.stream(BookingStatus.values()).map(Enum::name).toList();
    }

    @GetMapping("/requester/{userId}")
    public List<BookingResponse> byRequester(@PathVariable Long userId) {
        return bookingRepository.findByRequesterId(userId).stream().map(BookingResponse::from).toList();
    }

    @GetMapping("/owner/{userId}")
    public List<BookingResponse> byOwner(@PathVariable Long userId) {
        return bookingRepository.findByListingOwnerId(userId).stream().map(BookingResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        return bookingRepository.findById(id)
                .<ResponseEntity<?>>map(b -> ResponseEntity.ok(BookingResponse.from(b)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody BookingRequest req) {
        if (req.listingId() == null || req.requesterId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "listingId and requesterId are required"));
        }
        Listing listing = listingRepository.findById(req.listingId()).orElse(null);
        User requester = userRepository.findById(req.requesterId()).orElse(null);
        if (listing == null || requester == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Listing or user not found"));
        }
        if (listing.getOwner().getId().equals(requester.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot book your own listing"));
        }
        Booking booking = new Booking();
        booking.setListing(listing);
        booking.setRequester(requester);
        booking.setMessage(req.message());
        return ResponseEntity.ok(BookingResponse.from(bookingRepository.save(booking)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            booking.setStatus(BookingStatus.valueOf(req.status()));
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "status must be one of " + Arrays.toString(BookingStatus.values())));
        }
        return ResponseEntity.ok(BookingResponse.from(bookingRepository.save(booking)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!bookingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        bookingRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}