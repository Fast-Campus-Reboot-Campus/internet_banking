package com.bank.customer.device.repository;

import com.bank.customer.device.domain.RegisteredDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegisteredDeviceRepository extends JpaRepository<RegisteredDevice, Long> {

    List<RegisteredDevice> findByCustomerIdAndDeletedAtIsNull(Long customerId);

    Optional<RegisteredDevice> findByDeviceIdAndCustomerIdAndDeletedAtIsNull(
            Long deviceId, Long customerId);

    Optional<RegisteredDevice> findByCustomerIdAndDeviceFingerprintHashAndDeletedAtIsNull(
            Long customerId, String fingerprintHash);

    boolean existsByCustomerIdAndDeviceFingerprintHashAndDeletedAtIsNull(
            Long customerId, String fingerprintHash);
}
