package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;


@Component
final class UpgradeResultEvaluator {

    UpgradeResult evaluateUpgradeResult(Optional<ScoredItem> equippedItem, Optional<ScoredItem> upgrade) {
        if (upgrade.isEmpty()) {
            return new UpgradeResult.NoItemsAvailableResult(Instant.now());
        }
        if (equippedItem.isPresent()) {
            ScoredItem upgradeScoredItem = upgrade.get();
            ScoredItem equippedScoredItem = equippedItem.get();
            return upgradeScoredItem.score() > equippedScoredItem.score()
                    ? new UpgradeResult.UpgradeFoundResult(upgradeScoredItem.item(), Instant.now())
                    : new UpgradeResult.AlreadyBiSResult(equippedScoredItem.item(), Instant.now());
        } else {
            return new UpgradeResult.UpgradeFoundResult(upgrade.get().item(), Instant.now());
        }
    }
}
