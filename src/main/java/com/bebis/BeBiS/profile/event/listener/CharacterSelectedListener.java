package com.bebis.BeBiS.profile.event.listener;

import com.bebis.BeBiS.engine.upgrade.UpgradeFinderService;
import com.bebis.BeBiS.profile.event.CharacterSelectedEvent;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class CharacterSelectedListener {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CharacterSelectedListener.class);

    private final UpgradeFinderService analysisService;

    CharacterSelectedListener(UpgradeFinderService analysisService) {
        this.analysisService = analysisService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onCharacterSelected(CharacterSelectedEvent event) {
        log.debug("Event received: \n{}", event);
        analysisService.findUpgradesFor(event.characterInfo());
    }
}
