package com.neighborconnect.neighborconnect_backend.controller;

import com.neighborconnect.neighborconnect_backend.dto.ListingRequest;
import com.neighborconnect.neighborconnect_backend.dto.ListingResponse;
import com.neighborconnect.neighborconnect_backend.model.Listing;
import com.neighborconnect.neighborconnect_backend.model.ListingType;
import com.neighborconnect.neighborconnect_backend.model.User;
import com.neighborconnect.neighborconnect_backend.repository.ListingRepository;
import com.neighborconnect.neighborconnect_backend.repository.UserRepository;
import com.neighborconnect.neighborconnect_backend.security.CurrentUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
        return listingRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(ListingResponse::from).toList();
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
    public ResponseEntity<?> create(@RequestBody ListingRequest req, @AuthenticationPrincipal Jwt jwt) {
        String title = trim(req.title());
        String category = trim(req.category());
        if (title.isEmpty() || category.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Title and category are required"));
        }
        if (title.length() > 255 || category.length() > 255
                || trim(req.priceInfo()).length() > 255 || trim(req.neighborhood()).length() > 255
                || trim(req.description()).length() > 2000) {
            return ResponseEntity.badRequest().body(Map.of("error", "One of the fields is too long"));
        }
        ListingType type;
        try {
            type = ListingType.valueOf(req.type());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "type must be one of " + Arrays.toString(ListingType.values())));
        }
        // the owner is always the logged-in user, never something the browser sends
        User owner = userRepository.findById(CurrentUser.id(jwt)).orElse(null);
        if (owner == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Your account no longer exists"));
        }

        Listing listing = new Listing();
        listing.setTitle(title);
        listing.setDescription(blankToNull(req.description()));
        listing.setCategory(category);
        listing.setType(type);
        listing.setPriceInfo(blankToNull(req.priceInfo()));
        listing.setNeighborhood(blankToNull(req.neighborhood()));
        listing.setOwner(owner);
        return ResponseEntity.ok(ListingResponse.from(listingRepository.save(listing)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Listing listing = listingRepository.findById(id).orElse(null);
        if (listing == null) {
            return ResponseEntity.notFound().build();
        }
        boolean isOwner = listing.getOwner().getId().equals(CurrentUser.id(jwt));
        if (!isOwner && !CurrentUser.isAdmin(jwt)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only the owner can remove this listing"));
        }
        try {
            listingRepository.delete(listing);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409)
                    .body(Map.of("error", "This listing has requests and cannot be deleted"));
        }
        return ResponseEntity.noContent().build();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
