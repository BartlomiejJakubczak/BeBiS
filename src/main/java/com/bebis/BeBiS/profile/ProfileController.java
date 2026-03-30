package com.bebis.BeBiS.profile;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
    public ResponseEntity<List<WowCharacter>> getProfileSummary(@AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(profileService.getProfileSummary(getBlizzardAccountId(principal)));
    }

    private long getBlizzardAccountId(OAuth2User principal) {
        return Long.parseLong(principal.getAttribute("id").toString());
    }

}
