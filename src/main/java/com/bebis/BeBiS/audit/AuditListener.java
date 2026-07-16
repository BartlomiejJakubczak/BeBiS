package com.bebis.BeBiS.audit;

import com.bebis.BeBiS.item.event.ItemPersistedEvent;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class AuditListener {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AuditListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onItemPersistedEvent(ItemPersistedEvent event) {
        log.debug("Event received: \n{}", event);
    }
}
