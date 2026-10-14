package com.neighborconnect.neighborconnect_backend.repository;

import com.neighborconnect.neighborconnect_backend.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByRequesterId(Long requesterId);

    List<Booking> findByListingOwnerId(Long ownerId);
}