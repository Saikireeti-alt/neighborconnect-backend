package com.neighborconnect.neighborconnect_backend.controller;

import com.neighborconnect.neighborconnect_backend.dto.BookingRequest;
import com.neighborconnect.neighborconnect_backend.dto.BookingResponse;
import com.neighborconnect.neighborconnect_backend.dto.StatusRequest;
import com.neighborconnect.neighborconnect_backend.model.Booking;
import com.neighborconnect.neighborconnect_backend.model.BookingStatus;
import com.neighborconnect.neighborconnect_backend.model.Listing;
import com.neighborconnect.neighborconnect_backend.model.ListingStatus;
import com.neighborconnect.neighborconnect_backend.model.User;
import com.neighborconnect.neighborconnect_backend.repository.BookingRepository;
import com.neighborconnect.neighborconnect_backend.repository.ListingRepository;
import com.neighborconnect.neighborconnect_backend.repository.UserRepository;
import com.neighborconnect.neighborconnect_backend.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    /** A request that is still "alive": the same person cannot open a second one for the same listing. */
    private static final List<BookingStatus> OPEN = List.of(BookingStatus.PENDING, BookingStatus.ACCEPTED);

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final Object createLock = new Object();

    public BookingController(BookingRepository bookingRepository, ListingRepository listingRepository,
            UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    /** Every booking in the system: admins only. */
    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal Jwt jwt) {
        if (!CurrentUser.isAdmin(jwt)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admins only"));
        }
        return ResponseEntity.ok(newestFirst(bookingRepository.findAll()));
    }

    @GetMapping("/statuses")
    public List<String> getStatuses() {
        return Arrays.stream(BookingStatus.values()).map(Enum::name).toList();
    }

    /** Requests I have made. */
    @GetMapping("/mine")
    public List<BookingResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return newestFirst(bookingRepository.findByRequesterId(CurrentUser.id(jwt)));
    }

    /** Requests other people made on my listings. */
    @GetMapping("/incoming")
    public List<BookingResponse> incoming(@AuthenticationPrincipal Jwt jwt) {
        return newestFirst(bookingRepository.findByListingOwnerId(CurrentUser.id(jwt)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }
        Long me = CurrentUser.id(jwt);
        boolean involved = booking.getRequester().getId().equals(me)
                || booking.getListing().getOwner().getId().equals(me);
        if (!involved && !CurrentUser.isAdmin(jwt)) {
            return ResponseEntity.status(403).body(Map.of("error", "This request is not yours"));
        }
        return ResponseEntity.ok(BookingResponse.from(booking));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody BookingRequest req, @AuthenticationPrincipal Jwt jwt) {
        if (req.listingId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "listingId is required"));
        }
        if (req.message() != null && req.message().length() > 1000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is too long (1000 characters max)"));
        }
        Long me = CurrentUser.id(jwt);
        Listing listing = listingRepository.findById(req.listingId()).orElse(null);
        if (listing == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Listing not found"));
        }
        User requester = userRepository.findById(me).orElse(null);
        if (requester == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Your account no longer exists"));
        }
        if (listing.getOwner().getId().equals(me)) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot request your own listing"));
        }
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            return ResponseEntity.status(409).body(Map.of("error", "This listing is no longer available"));
        }

        // check + save under one lock so two quick clicks cannot both get through
        synchronized (createLock) {
            if (bookingRepository.existsByListingIdAndRequesterIdAndStatusIn(listing.getId(), me, OPEN)) {
                return ResponseEntity.status(409)
                        .body(Map.of("error", "You already have an open request for this listing"));
            }
            Booking booking = new Booking();
            booking.setListing(listing);
            booking.setRequester(requester);
            booking.setMessage(req.message() == null || req.message().isBlank() ? null : req.message().trim());
            return ResponseEntity.ok(BookingResponse.from(bookingRepository.save(booking)));
        }
    }

    /**
     * Owner of the listing: PENDING -> ACCEPTED or REJECTED, ACCEPTED -> COMPLETED.
     * The person who made the request may only cancel it (PENDING or ACCEPTED -> CANCELLED).
     * Anyone else gets 403.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }
        BookingStatus next;
        try {
            next = BookingStatus.valueOf(req.status());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "status must be one of " + Arrays.toString(BookingStatus.values())));
        }

        Long me = CurrentUser.id(jwt);
        boolean isOwner = booking.getListing().getOwner().getId().equals(me);
        boolean isRequester = booking.getRequester().getId().equals(me);
        BookingStatus current = booking.getStatus();

        if (isOwner || CurrentUser.isAdmin(jwt)) {
            boolean allowed = (current == BookingStatus.PENDING
                    && (next == BookingStatus.ACCEPTED || next == BookingStatus.REJECTED))
                    || (current == BookingStatus.ACCEPTED && next == BookingStatus.COMPLETED);
            if (!allowed) {
                return ResponseEntity.status(409)
                        .body(Map.of("error", "A " + current + " request cannot be changed to " + next));
            }
        } else if (isRequester) {
            if (next != BookingStatus.CANCELLED) {
                return ResponseEntity.status(403).body(Map.of("error",
                        "Only the listing owner can accept, decline or complete a request"));
            }
            if (!OPEN.contains(current)) {
                return ResponseEntity.status(409)
                        .body(Map.of("error", "A " + current + " request cannot be cancelled"));
            }
        } else {
            return ResponseEntity.status(403).body(Map.of("error", "This request is not yours"));
        }

        booking.setStatus(next);
        return ResponseEntity.ok(BookingResponse.from(bookingRepository.save(booking)));
    }

    /** Hard delete: admins only. Normal users cancel instead. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        if (!CurrentUser.isAdmin(jwt)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admins only"));
        }
        if (!bookingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        bookingRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private static List<BookingResponse> newestFirst(List<Booking> bookings) {
        return bookings.stream()
                .sorted(Comparator.comparing(Booking::getCreatedAt).reversed())
                .map(BookingResponse::from)
                .toList();
    }
}
