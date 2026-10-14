package com.neighborconnect.neighborconnect_backend.controller;

import com.neighborconnect.neighborconnect_backend.dto.ListingRequest;
import com.neighborconnect.neighborconnect_backend.dto.ListingResponse;
import com.neighborconnect.neighborconnect_backend.model.Listing;
import com.neighborconnect.neighborconnect_backend.model.ListingType;
import com.neighborconnect.neighborconnect_backend.model.User;
import com.neighborconnect.neighborconnect_backend.repository.ListingRepository;
import com.neighborconnect.neighborconnect_backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ListingController(ListingRepository listingRepository, UserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<ListingResponse> getAll() {
        return listingRepository.findAll().stream().map(ListingResponse::from).toList();
    }

    @GetMapping("/types")
    public List<String> getTypes() {
        return Arrays.stream(ListingType.values()).map(Enum::name).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        return listingRepository.findById(id)
                .<ResponseEntity<?>>map(l -> ResponseEntity.ok(ListingResponse.from(l)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ListingRequest req) {
        if (req.title() == null || req.title().isBlank()
                || req.category() == null || req.category().isBlank() || req.ownerId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "title, category and ownerId are required"));
        }
        User owner = userRepository.findById(req.ownerId()).orElse(null);
        if (owner == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Owner not found"));
        }
        ListingType type;
        try {
            type = ListingType.valueOf(req.type());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "type must be one of " + Arrays.toString(ListingType.values())));
        }
        Listing listing = new Listing();
        listing.setTitle(req.title());
        listing.setDescription(req.description());
        listing.setCategory(req.category());
        listing.setType(type);
        listing.setPriceInfo(req.priceInfo());
        listing.setNeighborhood(req.neighborhood());
        listing.setOwner(owner);
        return ResponseEntity.ok(ListingResponse.from(listingRepository.save(listing)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!listingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            listingRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(Map.of("error", "This listing has bookings and cannot be deleted"));
        }
        return ResponseEntity.noContent().build();
    }
}