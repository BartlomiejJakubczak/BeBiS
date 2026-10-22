package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.item.ItemQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EquipmentServiceTest {

    @Mock
    private BlizzardUserClient blizzardClient;

    @Mock
    private EquipmentSynchronizer synchronizer;

    @Mock
    private EquipmentMapper mapper;

    @Mock
    private ItemQueryService itemQueryService;

    private EquipmentService service;

    @BeforeEach
    void setUp() {
        service = new EquipmentService(blizzardClient, mapper, synchronizer, itemQueryService);
    }

    @Test
    void shouldReplaceStoredEquipmentWithBlizzardSnapshot_WhenSyncing() {
        // given
        String realmSlug = "soulseeker";
        String characterName = "Thelamar";
        EquipmentEntity equipment = mock(EquipmentEntity.class);
        EquipmentResponse response = mock(EquipmentResponse.class);

        when(blizzardClient.getCharacterEquipment(realmSlug, characterName)).thenReturn(response);

        // when
        service.syncEquipment(characterName, realmSlug, equipment);

        // then
        verify(blizzardClient).getCharacterEquipment(realmSlug, characterName);
        verify(synchronizer).synchronize(response, equipment);
        verifyNoInteractions(itemQueryService, mapper);
    }

    @Test
    void shouldAssembleStoredEquipmentWithItemsFromQueryService_WithoutCallingBlizzard() {
        // given
        EquipmentEntity equipment = mock(EquipmentEntity.class);
        Equipment assembled = new Equipment();

        when(equipment.getItems()).thenReturn(Map.of()); // nothing equipped, so no keys to look up
        when(itemQueryService.findAllByKeys(Set.of())).thenReturn(Map.of());
        when(mapper.mapToDomain(equipment, Map.of())).thenReturn(assembled);

        // when
        Equipment result = service.getEquipmentForCharacter(equipment);

        // then
        assertThat(result).isSameAs(assembled);
        verifyNoInteractions(blizzardClient, synchronizer);
    }
}
