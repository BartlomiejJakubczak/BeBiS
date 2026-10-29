package com.bebis.BeBiS.profile.event.listener;

import com.bebis.BeBiS.base.BaseAsyncListenerTest;
import com.bebis.BeBiS.engine.upgrade.UpgradeFinderService;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowTalents;
import com.bebis.BeBiS.profile.event.CharacterSelectedEvent;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;

import static com.bebis.BeBiS.profile.domain.WowCharacterBuilder.newWowCharacter;
import static com.bebis.BeBiS.profile.domain.WowTalentsBuilder.newWowTalents;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@Import(CharacterSelectedListener.class)
public class CharacterSelectedListenerTest extends BaseAsyncListenerTest {

    @MockitoSpyBean
    private CharacterSelectedListener listener;

    @MockitoBean
    private UpgradeFinderService analysisService;

    @Test
    void shouldReceiveCharacterSelectedEventAsynchronouslyAndCallAnalysisService() {
        // given
        CharacterInfo selectedCharacter = getCharacterInfo();

        // when
        transactionTemplate.executeWithoutResult((s) -> publisher.publishEvent(new CharacterSelectedEvent(selectedCharacter, Instant.now())));

        // then
        await().untilAsserted(() -> {
            verify(listener).onCharacterSelected(any(CharacterSelectedEvent.class));
            verify(analysisService).findUpgradesFor(eq(selectedCharacter));
        });
    }

    private static @NonNull CharacterInfo getCharacterInfo() {
        WowCharacter wowChar = newWowCharacter().build();
        Equipment equipment = new Equipment();
        WowTalents talents = newWowTalents().withSpec("COMBAT", 31).withSpec("ASSASSINATION", 20).build();

        return new CharacterInfo(wowChar, equipment, talents);
    }
}
