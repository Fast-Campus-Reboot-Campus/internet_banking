package com.bank.customer.fds.repository;

import com.bank.customer.fds.domain.FdsIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FdsIncidentRepository extends JpaRepository<FdsIncident, Long> {

    Optional<FdsIncident> findByFdsDetectionId(Long fdsDetectionId);

    Page<FdsIncident> findByFdsIncidentProcessStatusCodeOrderByFdsIncidentIdDesc(
            String processStatusCode, Pageable pageable);
}
