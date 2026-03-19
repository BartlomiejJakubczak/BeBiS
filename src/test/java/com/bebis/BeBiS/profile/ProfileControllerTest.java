package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class) // loads MVC layer, controllers and security filters, doesn't scan Service layer
@AutoConfigureMockMvc(addFilters = false) // disable SecurityFilterChain for controller unit tests
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
        List<WowCharacter> allCharacters = profileMapper.mapToDomain(expectedSummary);
        // when
        when(profileService.getProfileSummary()).thenReturn(allCharacters);
        // then
        mockMvc.perform(get("/api/profile/summary"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(allCharacters)));
    }

}
