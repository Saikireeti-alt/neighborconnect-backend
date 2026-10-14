package com.neighborconnect.neighborconnect_backend.repository;

import com.neighborconnect.neighborconnect_backend.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<Listing, Long> {
}