package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.base.BaseNonTransactionalFullstackTest;
import com.bebis.BeBiS.engine.upgrade.event.UpgradeEvent;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.domain.Equipment.Slot;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.WowCharacterBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@RecordApplicationEvents
public class UpgradeFinderServiceIT extends BaseNonTransactionalFullstackTest {

    private static final Set<Slot> ALL_SLOTS = Set.of(HEAD, NECK, SHOULDER, BACK, CHEST, WRIST, HANDS,
            WAIST, LEGS, FEET, FINGER_1, FINGER_2, TRINKET_1, TRINKET_2, MAIN_HAND, OFF_HAND, RANGED);

    @Autowired
    private UpgradeFinderService upgradeFinderService;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Test
    void shouldPublishExactlyOneEventPerSlot_EmptyEquipment() {
        // given
        CharacterInfo info = new CharacterInfo(WowCharacterBuilder.newWowCharacter().build(), new Equipment(), Optional.empty());

        // when
        upgradeFinderService.findUpgradesFor(info);

        // then
        await().during(500, TimeUnit.MILLISECONDS).atMost(2000, TimeUnit.MILLISECONDS).untilAsserted(() ->
                assertThat(collectRecordedSlots()).containsExactlyInAnyOrderElementsOf(ALL_SLOTS));
        // containsExactly...() checks for number of occurrences as well, so within the given time frame all the slots
        // have to appear at most once and all have to be accounted for.
    }

    private List<Slot> collectRecordedSlots() {
        return applicationEvents.stream(UpgradeEvent.class).map(UpgradeEvent::slot).toList(); // a list to catch duplicates if there are any
    }
}
