package com.mycompany.myshop.backend.api.controller;

import com.mycompany.myshop.backend.core.dtos.UpdateProfileRequest;
import com.mycompany.myshop.backend.core.dtos.ViewProfileResponse;
import com.mycompany.myshop.backend.core.services.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ViewProfileResponse> viewProfile(Authentication authentication,
                                                           @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(profileService.getProfile(CurrentUserResolver.resolve(authentication, jwt)));
    }

    @PutMapping
    public ResponseEntity<ViewProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                             Authentication authentication,
                                                             @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(profileService.updateProfile(request, CurrentUserResolver.resolve(authentication, jwt)));
    }
}
