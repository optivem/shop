package com.mycompany.myshop.api.controller;

import com.mycompany.myshop.config.CurrentUserResolver;
import com.mycompany.myshop.core.dtos.UpdateProfileRequest;
import com.mycompany.myshop.core.dtos.ViewProfileResponse;
import com.mycompany.myshop.core.services.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account/profile")
public class ProfileApiController {

    private final ProfileService profileService;

    public ProfileApiController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ViewProfileResponse> viewProfile(Authentication authentication) {
        return ResponseEntity.ok(profileService.getProfile(CurrentUserResolver.resolve(authentication)));
    }

    @PutMapping
    public ResponseEntity<ViewProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(profileService.updateProfile(request, CurrentUserResolver.resolve(authentication)));
    }
}
