package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.base.BaseFullStackTest;
import com.bebis.BeBiS.equipment.EquipmentResponseBuilder;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.BlizzardFixtures;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.integration.blizzard.dto.SpecializationResponse;
import com.bebis.BeBiS.item.ItemResponseBuilder;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.event.CharacterSelectedEvent;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.List;

import static com.bebis.BeBiS.profile.domain.WowTalentsBuilder.newWowTalents;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RecordApplicationEvents
public class CharacterProfileOrchestratorIT extends BaseFullStackTest {

    @Autowired
    private CharacterProfileOrchestrator orchestrator;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldOrchestrateSelectionOfCharacter() {
        // given
        long charId = 21L;
        long blizzAccountId = 37L;
        String realmSlug = "soulseeker";
        String charName = "Thelamar";
        WowCharacter.WowClass wowClass = WowCharacter.WowClass.ROGUE;
        WowCharacter.Race race = WowCharacter.Race.NIGHT_ELF;

        setupWowCharacter(charId, blizzAccountId, realmSlug, charName, wowClass, race);

        SpecializationResponse specResponse = BlizzardFixtures.loadSpecialization("spent_points_above_10.json");
        when(blizzardUserClient.getCharacterSpecialization(realmSlug, charName)).thenReturn(specResponse);

        ItemResponse response = ItemResponseBuilder.newWeaponInstance().build();
        when(blizzardServiceClient.getBaseItem(response.id())).thenReturn(response);

        EquipmentResponse eqResponse = new EquipmentResponse(List.of(EquipmentResponseBuilder.newInstance(response).withSlot("MAIN_HAND").build()));
        when(blizzardUserClient.getCharacterEquipment(realmSlug, charName)).thenReturn(eqResponse);

        // when
        CharacterInfo charInfo = orchestrator.selectCharacter(new WowCharacter.Id(charId, realmSlug, blizzAccountId));

        // then

        WowCharacter characterFromSelected = charInfo.wowCharacter();
        assertThat(characterFromSelected.id().id()).isEqualTo(charId);
        assertThat(characterFromSelected.id().realmSlug()).isEqualTo(realmSlug);
        assertThat(characterFromSelected.name()).isEqualTo(charName);
        assertThat(characterFromSelected.race()).isEqualTo(race);
        assertThat(characterFromSelected.wowClass()).isEqualTo(wowClass);

        assertThat(charInfo.talents())
                .isEqualTo(newWowTalents().withSpec("COMBAT", 32).withSpec("ASSASSINATION", 12).build());

        Equipment equipmentFromSelected = charInfo.equipment();
        EquippedItem equippedItem = equipmentFromSelected.getEquipment().get(Equipment.Slot.MAIN_HAND);
        assertThat(equippedItem).isNotNull();
        assertThat(equippedItem.item().getMetadata().key().baseId()).isEqualTo(response.id());
        assertThat(equippedItem.item().getMetadata().name()).isEqualTo(response.name());

        List<CharacterSelectedEvent> eventsFired = applicationEvents.stream(CharacterSelectedEvent.class).toList();
        assertThat(eventsFired.size()).isEqualTo(1);
        assertThat(eventsFired.getFirst().characterInfo()).isEqualTo(charInfo);
    }

    private void setupWowCharacter(long charId, long blizzAccountId, String realmSlug,
                                   String charName, WowCharacter.WowClass wowClass, WowCharacter.Race race) {
        WowCharacterEntity charEntity = new WowCharacterEntity();
        charEntity.setPk(new WowCharacterEntity.CompositeKey(charId, realmSlug, blizzAccountId));
        EquipmentEntity eqEntity = new EquipmentEntity();
        eqEntity.setCharacter(charEntity);
        charEntity.setEquipment(eqEntity);
        charEntity.setName(charName);
        charEntity.setLevel(60);
        charEntity.setRace(race);
        charEntity.setWowClass(wowClass);
        charEntity.setRealmName("Soulseeker");
        entityManager.persist(charEntity);
        entityManager.flush();
        entityManager.clear();
    }
}
