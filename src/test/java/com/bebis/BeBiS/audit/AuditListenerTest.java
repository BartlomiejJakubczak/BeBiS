package com.bebis.BeBiS.audit;

import com.bebis.BeBiS.base.BaseAsyncListenerTest;
import com.bebis.BeBiS.item.event.ItemPersistedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@Import(AuditListener.class)
public class AuditListenerTest extends BaseAsyncListenerTest {

    @MockitoSpyBean
    private AuditListener listener;

    @Test
    void shouldReceiveItemPersistedEventAsynchronously() {
        // when
        transactionTemplate.executeWithoutResult((s) -> publisher.publishEvent(new ItemPersistedEvent(1L, 1L, Instant.now())));

        // then
        await().untilAsserted(() -> verify(listener).onItemPersistedEvent(any(ItemPersistedEvent.class)));
    }

}
