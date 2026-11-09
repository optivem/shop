package com.mycompany.myshop.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myshop.core.dtos.CurrentUser;
import com.mycompany.myshop.core.dtos.UpdateProfileRequest;
import com.mycompany.myshop.core.entities.CustomerProfile;
import com.mycompany.myshop.core.repositories.CustomerProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");
    private static final CurrentUser CUSTOMER =
            new CurrentUser("customer-sub", "customer1", false, "customer1@example.com", "Token", "Name");

    @Mock
    private CustomerProfileRepository repository;

    private ProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void profileWithoutSavedRowTakesTheNameFromTheTokenAndHasNoAddress() {
        when(repository.findByOwner("customer-sub")).thenReturn(Optional.empty());

        var profile = service.getProfile(CUSTOMER);

        assertThat(profile.getFirstName()).isEqualTo("Token");
        assertThat(profile.getLastName()).isEqualTo("Name");
        assertThat(profile.getEmail()).isEqualTo("customer1@example.com");
        assertThat(profile.getStreet()).isNull();
        assertThat(profile.getPostalCode()).isNull();
        assertThat(profile.getCity()).isNull();
        assertThat(profile.getCountry()).isNull();
    }

    @Test
    void profileWithoutSavedRowOrTokenNamesHasNullNames() {
        when(repository.findByOwner("anon-sub")).thenReturn(Optional.empty());

        var profile = service.getProfile(new CurrentUser("anon-sub", "u", false));

        assertThat(profile.getFirstName()).isNull();
        assertThat(profile.getLastName()).isNull();
        assertThat(profile.getEmail()).isNull();
    }

    @Test
    void savedProfileIsReturnedWithTheTokenEmail() {
        when(repository.findByOwner("customer-sub")).thenReturn(Optional.of(savedProfile()));

        var profile = service.getProfile(CUSTOMER);

        assertThat(profile.getFirstName()).isEqualTo("Ana");
        assertThat(profile.getStreet()).isEqualTo("Main St 1");
        assertThat(profile.getCountry()).isEqualTo("DE");
        assertThat(profile.getEmail()).isEqualTo("customer1@example.com");
    }

    @Test
    void updateInsertsANewProfileWithTrimmedValues() {
        when(repository.findByOwner("customer-sub")).thenReturn(Optional.empty());
        when(repository.save(any(CustomerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateProfile(request(" Ana ", "Novak ", " Main St 1", "10115", "Berlin", "DE"), CUSTOMER);

        var captor = ArgumentCaptor.forClass(CustomerProfile.class);
        verify(repository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getOwner()).isEqualTo("customer-sub");
        assertThat(saved.getFirstName()).isEqualTo("Ana");
        assertThat(saved.getLastName()).isEqualTo("Novak");
        assertThat(saved.getStreet()).isEqualTo("Main St 1");
        assertThat(saved.getUpdatedAt()).isEqualTo(NOW);
        assertThat(response.getFirstName()).isEqualTo("Ana");
        assertThat(response.getEmail()).isEqualTo("customer1@example.com");
    }

    @Test
    void updateChangesTheExistingProfile() {
        var existing = savedProfile();
        existing.setId(7L);
        when(repository.findByOwner("customer-sub")).thenReturn(Optional.of(existing));
        when(repository.save(any(CustomerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateProfile(request("Ana", "Novak", "1 High St", "SW1A 1AA", "London", "GB"), CUSTOMER);

        assertThat(existing.getId()).isEqualTo(7L);
        assertThat(existing.getCity()).isEqualTo("London");
        assertThat(existing.getCountry()).isEqualTo("GB");
        assertThat(response.getPostalCode()).isEqualTo("SW1A 1AA");
    }

    private static CustomerProfile savedProfile() {
        var profile = new CustomerProfile("customer-sub");
        profile.setFirstName("Ana");
        profile.setLastName("Novak");
        profile.setStreet("Main St 1");
        profile.setPostalCode("10115");
        profile.setCity("Berlin");
        profile.setCountry("DE");
        profile.setUpdatedAt(NOW);
        return profile;
    }

    private static UpdateProfileRequest request(String firstName, String lastName, String street,
                                                String postalCode, String city, String country) {
        var request = new UpdateProfileRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setStreet(street);
        request.setPostalCode(postalCode);
        request.setCity(city);
        request.setCountry(country);
        return request;
    }
}
