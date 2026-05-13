package com.bebis.BeBiS.item;

import com.bebis.BeBiS.base.BaseResilienceTest;
import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;

import static com.bebis.BeBiS.config.ResilienceConfig.ResilienceConstants.BLIZZARD_FETCHER;
import static com.bebis.BeBiS.config.ResilienceConfig.ResilienceConstants.BlizzardFetcher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class BlizzardItemFetcherResilienceTest extends BaseResilienceTest {

    @Autowired
    private BlizzardItemFetcher itemFetcher;

    @MockitoBean
    private BlizzardServiceClient serviceClient;

    @Test
    void shouldRetryWhenApiNotResponsive() {
        // given
        long itemId = 1;
        when(serviceClient.getBaseItem(itemId)).thenThrow(RestClientException.class);

        // when
        ItemResponse result = itemFetcher.fetchItem(itemId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(itemId);
        verify(serviceClient, times(BlizzardFetcher.INSTANCE.maxRetries)).getBaseItem(itemId);
    }

    @Test
    void shouldOpenCircuitBreakerWhenApiIsDown() {
        // given
        when(serviceClient.getBaseItem(anyLong())).thenThrow(RestClientException.class);

        for (int i = 0; i < BlizzardFetcher.INSTANCE.windowSize; i++) {
            itemFetcher.fetchItem(i);
        }

        // when
        itemFetcher.fetchItem(999L);

        // then
        verify(serviceClient, atMost(BlizzardFetcher.INSTANCE.windowSize * 2)).getBaseItem(anyLong());

        // Pro-Tip: You can also verify the state directly via the registry
        var state = circuitBreakerRegistry.circuitBreaker(BLIZZARD_FETCHER).getState();
        assertThat(state).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
