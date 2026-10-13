package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;


@Component
final class UpgradeResultEvaluator {

    UpgradeResult evaluateUpgradeResult(Optional<ScoredItem> equippedItem, Optional<ScoredItem> upgrade) {
        return upgrade.map((up) -> equippedItem.map((eq) -> up.score() > eq.score()
                                ? new UpgradeResult.UpgradeFoundResult(up.item(), Instant.now())
                                : new UpgradeResult.AlreadyBiSResult(eq.item(), Instant.now()))
                        .orElseGet(() -> new UpgradeResult.UpgradeFoundResult(up.item(), Instant.now()))) // orElseGet preferred over orElse (LAZY vs EAGER)
                .orElseGet(() -> new UpgradeResult.NoItemsAvailableResult(Instant.now()));
    }
}
