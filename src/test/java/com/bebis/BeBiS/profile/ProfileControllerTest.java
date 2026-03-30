package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class) // loads MVC layer, controllers and security filters, doesn't scan Service layer
@AutoConfigureMockMvc
public class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @Autowired
    private ObjectMapper objectMapper;

    private final ProfileMapper profileMapper = new ProfileMapper();

    @Test
    void shouldReturnCharactersFromProfileSummary() throws Exception {
        // given
        ProfileSummaryResponse expectedSummary = ProfileTestData.generateProfileSummaryResponse();
        long blizzardAccountId = expectedSummary.blizzardAccountId();
        List<WowCharacterEntity> entities = profileMapper.mapToEntity(expectedSummary, blizzardAccountId);
        List<WowCharacter> allCharacters = profileMapper.mapToDomain(entities);
        // when
        when(profileService.getProfileSummary(blizzardAccountId)).thenReturn(allCharacters);
        // then
        mockMvc.perform(get("/api/profile/summary")
                        .with(oauth2Login() // creates the mock OAuth2User
                                .attributes(attrs -> attrs.put("id", blizzardAccountId))) // supply the needed accountId
                )
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(allCharacters)));
    }
}
