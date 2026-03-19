package com.bebis.BeBiS.profile;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping(value = "/summary")
    public ResponseEntity<List<WowCharacter>> getProfileSummary() {
        return ResponseEntity.ok(profileService.getProfileSummary());
    }

}
