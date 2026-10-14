package com.neighborconnect.neighborconnect_backend.repository;

import com.neighborconnect.neighborconnect_backend.model.Booking;
import com.neighborconnect.neighborconnect_backend.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByRequesterId(Long requesterId);

    List<Booking> findByListingOwnerId(Long ownerId);

    boolean existsByListingIdAndRequesterIdAndStatusIn(Long listingId, Long requesterId,
            Collection<BookingStatus> statuses);
}
