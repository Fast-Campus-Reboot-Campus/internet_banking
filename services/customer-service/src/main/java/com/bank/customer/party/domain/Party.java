package com.bank.customer.party.domain;

import com.bank.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "party")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Party extends BaseEntity {

    public static final String TYPE_PERSONAL     = "PERSONAL";
    public static final String TYPE_ORGANIZATION = "ORGANIZATION";

    public static final String STATUS_ACTIVE    = "ACTIVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_CLOSED    = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_id")
    private Long partyId;

    @Column(name = "party_type_code", nullable = false, length = 20)
    private String partyTypeCode;

    @Column(name = "party_name", nullable = false, length = 100)
    private String partyName;

    @Column(name = "party_english_name", length = 200)
    private String partyEnglishName;

    @Column(name = "party_status_code", nullable = false, length = 20)
    private String partyStatusCode;

    public void updateName(String partyName) {
        this.partyName = partyName;
    }

    public void suspend() {
        this.partyStatusCode = STATUS_SUSPENDED;
    }

    public void close() {
        this.partyStatusCode = STATUS_CLOSED;
    }

    public boolean isClosed() {
        return STATUS_CLOSED.equals(partyStatusCode);
    }
}
