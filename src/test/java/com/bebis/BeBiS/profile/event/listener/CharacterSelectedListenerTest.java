package com.bebis.BeBiS.profile.event.listener;

import com.bebis.BeBiS.base.BaseAsyncListenerTest;
import com.bebis.BeBiS.engine.upgrade.UpgradeFinderService;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowRealm;
import com.bebis.BeBiS.profile.domain.WowTalents;
import com.bebis.BeBiS.profile.event.CharacterSelectedEvent;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.bebis.BeBiS.profile.domain.WowTalents.Specialization;
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
        WowCharacter wowChar = new WowCharacter(new WowCharacter.Id(1L, "soulseeker"), "Thelamar", 60,
                WowCharacter.Race.NIGHT_ELF, WowCharacter.WowClass.ROGUE, new WowRealm("Soulseeker", "soulseeker"));

        Equipment equipment = new Equipment();

        WowTalents talents = new WowTalents(List.of(
                new Specialization("COMBAT", 31),
                new Specialization("ASSASSINATION", 20),
                new Specialization("SUBTLETY", 0)
        ));

        return new CharacterInfo(wowChar, equipment, Optional.of(talents));
    }
}
