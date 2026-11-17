package com.mycompany.myshop.backend.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/** A customer's name and default shipping address, one row per token subject. The email is not stored. */
@Entity
@Table(name = "customer_profiles")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class CustomerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "owner", nullable = false, unique = true)
    private String owner;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "street", nullable = false)
    private String street;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CustomerProfile(String owner) {
        this.owner = owner;
    }

    /**
     * Identity is the database id, so an entity stays equal to itself before and after it is persisted
     * and across Hibernate proxies; two unsaved entities are never equal. The hash code is constant per
     * class so it does not change when the id is assigned on save.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CustomerProfile that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return CustomerProfile.class.hashCode();
    }
}
