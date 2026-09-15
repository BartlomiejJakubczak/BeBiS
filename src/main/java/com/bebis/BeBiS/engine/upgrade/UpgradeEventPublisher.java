package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.engine.upgrade.event.UpgradeEvent;
import com.bebis.BeBiS.equipment.domain.Equipment;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
final class UpgradeEventPublisher {

    private final ApplicationEventPublisher publisher;

    UpgradeEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    void publish(Equipment.Slot slot, UpgradeResult result) {
        switch (result) {
            case UpgradeResult.UpgradeFoundResult r ->
                    publisher.publishEvent(new UpgradeEvent.UpgradeFoundEvent(slot, r.item(), r.timestamp()));
            case UpgradeResult.AlreadyBiSResult r ->
                    publisher.publishEvent(new UpgradeEvent.BiSAlreadyEquippedEvent(slot, r.item(), r.timestamp()));
            case UpgradeResult.NoItemsAvailableResult r ->
                    publisher.publishEvent(new UpgradeEvent.UpgradeNotAvailableEvent(slot, r.timestamp()));
        }
    }
}
