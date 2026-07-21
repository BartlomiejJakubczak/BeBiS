package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.base.BaseResilienceTest;
import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.SpecializationResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;

import static com.bebis.BeBiS.config.ResilienceConfig.ResilienceConstants.BLIZZARD_FETCHER;
import static com.bebis.BeBiS.config.ResilienceConfig.ResilienceConstants.BlizzardFetcher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class BlizzardSpecializationFetcherResilienceTest extends BaseResilienceTest {

    @Autowired
    private BlizzardSpecializationFetcher specializationFetcher;

    @MockitoBean
    private BlizzardUserClient userClient;

    @Test
    void shouldRetryWhenApiNotResponsive() {
        // given
        String realmSlug = "soulseeker";
        String charName = "Thelamar";
        when(userClient.getCharacterSpecialization(realmSlug, charName)).thenThrow(RestClientException.class);

        // when
        SpecializationResponse result = specializationFetcher.fetchSpecialization(realmSlug, charName);

        // then
        assertThat(result).isNotNull();
        verify(userClient, times(BlizzardFetcher.INSTANCE.maxRetries))
                .getCharacterSpecialization(realmSlug, charName);
    }

    @Test
    void shouldOpenCircuitBreakerWhenApiIsDown() {
        // given
        String realmSlug = "soulseeker";
        String charName = "Thelamar";
        when(userClient.getCharacterSpecialization(realmSlug, charName)).thenThrow(RestClientException.class);

        for (int i = 0; i < BlizzardFetcher.INSTANCE.windowSize; i++) {
            specializationFetcher.fetchSpecialization(realmSlug, charName);
        }

        // when
        specializationFetcher.fetchSpecialization(realmSlug, charName);
        
        // then
        verify(userClient, atMost(BlizzardFetcher.INSTANCE.windowSize * 2)).getCharacterSpecialization(realmSlug, charName);

        var state = circuitBreakerRegistry.circuitBreaker(BLIZZARD_FETCHER).getState();
        assertThat(state).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
