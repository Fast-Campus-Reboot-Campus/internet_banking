package com.bank.customer.cert.domain;

import com.bank.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name = "auth_method")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuthMethod extends BaseEntity {

    public static final String STATUS_ACTIVE   = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_method_id")
    private Long authMethodId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "auth_method_type_code", nullable = false, length = 20)
    private String authMethodTypeCode;

    @Column(name = "auth_method_alias_name", length = 50)
    private String authMethodAliasName;

    @Column(name = "auth_method_status_code", nullable = false, length = 20)
    private String authMethodStatusCode;

    @Column(name = "primary_auth_method_yn", nullable = false, length = 1)
    private String primaryAuthMethodYn;

    @Column(name = "auth_method_registered_date", nullable = false, length = 8)
    private String authMethodRegisteredDate;

    @Column(name = "auth_method_expiry_date", length = 8)
    private String authMethodExpiryDate;

    @Column(name = "auth_method_last_used_at")
    private java.time.OffsetDateTime authMethodLastUsedAt;

    public boolean isActive()  { return STATUS_ACTIVE.equals(authMethodStatusCode); }
    public boolean isPrimary() { return "T".equals(primaryAuthMethodYn); }

    public void updateAlias(String alias)  { this.authMethodAliasName = alias; }
    public void deactivate()               { this.authMethodStatusCode = STATUS_INACTIVE; }
    public void activate()                 { this.authMethodStatusCode = STATUS_ACTIVE; }
    public void setPrimary(boolean primary){ this.primaryAuthMethodYn = primary ? "T" : "F"; }
    public void recordUsed()               { this.authMethodLastUsedAt = java.time.OffsetDateTime.now(); }
}
