package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.profile.domain.CharacterInfo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Entry point of the upgrade engine: hands the character to every {@link UpgradeOrchestrator}. Each orchestrator owns
 * its slots, so this service doesn't know which slots exist.
 */
@Service
public class UpgradeFinderService {

    private final List<UpgradeOrchestrator> orchestrators;

    UpgradeFinderService(List<UpgradeOrchestrator> orchestrators) {
        this.orchestrators = List.copyOf(orchestrators);
    }

    public void findUpgradesFor(CharacterInfo info) {
        orchestrators.forEach(orchestrator -> orchestrator.findUpgradesFor(info));
    }
}
