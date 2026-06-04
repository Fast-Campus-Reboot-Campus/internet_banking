package com.bank.customer.pin.repository;

import com.bank.customer.pin.domain.Pin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PinRepository extends JpaRepository<Pin, Long> {

    Optional<Pin> findByCustomerIdAndDeviceIdAndDeletedAtIsNull(Long customerId, Long deviceId);

    List<Pin> findByCustomerIdAndDeletedAtIsNull(Long customerId);

    boolean existsByCustomerIdAndDeviceIdAndDeletedAtIsNull(Long customerId, Long deviceId);
}
