package com.bebis.BeBiS.audit;

import com.bebis.BeBiS.base.BaseAsyncListenerTest;
import com.bebis.BeBiS.item.event.ItemPersistedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@Import(AuditListener.class)
public class AuditListenerTest extends BaseAsyncListenerTest {

    @MockitoSpyBean
    private AuditListener listener;

    @Test
    void shouldReceiveItemPersistedEventAsynchronously() {
        // given
        long expectedBaseId = 1L;
        long expectedSuffixId = 1L;

        // when
        transactionTemplate.executeWithoutResult((s) ->
                publisher.publishEvent(new ItemPersistedEvent(expectedBaseId, expectedSuffixId, Instant.now())));

        // then
        await().untilAsserted(() -> verify(listener).onItemPersistedEvent(argThat(event ->
                event.baseId() == expectedBaseId && event.suffixId() == expectedSuffixId)));
    }

}
