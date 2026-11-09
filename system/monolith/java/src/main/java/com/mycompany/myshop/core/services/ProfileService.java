package com.mycompany.myshop.core.services;

import com.mycompany.myshop.core.dtos.CurrentUser;
import com.mycompany.myshop.core.dtos.UpdateProfileRequest;
import com.mycompany.myshop.core.dtos.ViewProfileResponse;
import com.mycompany.myshop.core.entities.CustomerProfile;
import com.mycompany.myshop.core.repositories.CustomerProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * The signed-in customer's profile (UC-010), keyed by the token subject. The email always comes from
 * the token; until a profile is saved, the name comes from the token and the address is empty.
 */
@Service
public class ProfileService {

    private final CustomerProfileRepository customerProfileRepository;
    private final Clock clock;

    @Autowired
    public ProfileService(CustomerProfileRepository customerProfileRepository) {
        this(customerProfileRepository, Clock.systemUTC());
    }

    ProfileService(CustomerProfileRepository customerProfileRepository, Clock clock) {
        this.customerProfileRepository = customerProfileRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ViewProfileResponse getProfile(CurrentUser user) {
        return customerProfileRepository.findByOwner(user.subject())
                .map(profile -> toResponse(profile, user))
                .orElseGet(() -> defaultProfile(user));
    }

    /** Inserts or updates the caller's profile. The request is already validated. */
    @Transactional
    public ViewProfileResponse updateProfile(UpdateProfileRequest request, CurrentUser user) {
        var profile = customerProfileRepository.findByOwner(user.subject())
                .orElseGet(() -> new CustomerProfile(user.subject()));
        profile.setFirstName(trim(request.getFirstName()));
        profile.setLastName(trim(request.getLastName()));
        profile.setStreet(trim(request.getStreet()));
        profile.setPostalCode(trim(request.getPostalCode()));
        profile.setCity(trim(request.getCity()));
        profile.setCountry(trim(request.getCountry()));
        profile.setUpdatedAt(clock.instant());
        return toResponse(customerProfileRepository.save(profile), user);
    }

    private static ViewProfileResponse toResponse(CustomerProfile profile, CurrentUser user) {
        var response = new ViewProfileResponse();
        response.setFirstName(profile.getFirstName());
        response.setLastName(profile.getLastName());
        response.setEmail(user.email());
        response.setStreet(profile.getStreet());
        response.setPostalCode(profile.getPostalCode());
        response.setCity(profile.getCity());
        response.setCountry(profile.getCountry());
        return response;
    }

    private static ViewProfileResponse defaultProfile(CurrentUser user) {
        var response = new ViewProfileResponse();
        response.setFirstName(user.givenName());
        response.setLastName(user.familyName());
        response.setEmail(user.email());
        return response;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
