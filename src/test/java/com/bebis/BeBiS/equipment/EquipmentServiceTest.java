package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EquipmentServiceTest {

    @Mock
    private BlizzardUserClient blizzardClient;

    @Mock
    private EquipmentSynchronizer synchronizer;

    @Mock
    private EquipmentMapper mapper;

    private EquipmentService service;

    @BeforeEach
    void setUp() {
        service = new EquipmentService(blizzardClient, mapper, synchronizer);
    }

    @Test
    void shouldOrchestrateEquipmentSyncForCharacter() {
        // given
        WowCharacterEntity.CompositeKey pk = new WowCharacterEntity.CompositeKey(1, "soulseeker", 1L);
        String stubName = "Thelamar";

        WowCharacterEntity stub = mock(WowCharacterEntity.class);
        EquipmentEntity equipment = mock(EquipmentEntity.class);
        when(stub.getPk()).thenReturn(pk);
        when(stub.getName()).thenReturn(stubName);
        when(stub.getEquipment()).thenReturn(equipment);

        EquipmentResponse response = mock(EquipmentResponse.class);

        when(blizzardClient.getCharacterEquipment(stub.getPk().getRealmSlug(), stub.getName())).thenReturn(response);
        when(mapper.mapToDomain(equipment)).thenReturn(new Equipment());

        // when
        service.getEquipmentForCharacter(stub);

        // then
        verify(blizzardClient).getCharacterEquipment(stub.getPk().getRealmSlug(), stub.getName());
        verify(synchronizer).synchronize(response, stub.getEquipment());
        verify(mapper).mapToDomain(stub.getEquipment());
    }
}
