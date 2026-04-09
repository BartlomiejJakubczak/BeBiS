package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import com.bebis.BeBiS.profile.jpa.WowCharacterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EquipmentServiceTest {

    @Mock
    private BlizzardUserClient blizzardClient;

    @Mock
    private WowCharacterRepository wowCharacterRepository;

    @Mock
    private EquipmentSynchronizer synchronizer;

    @Mock
    private EquipmentMapper mapper;

    private EquipmentService service;

    @BeforeEach
    void setUp() {
        service = new EquipmentService(
                blizzardClient,
                mapper,
                synchronizer,
                wowCharacterRepository
        );
    }

    @Test
    void shouldOrchestrateEquipmentSyncForCharacter() {
        // given
        long blizzAccountId = 1L;
        WowCharacterEntity.CompositeKey pk = new WowCharacterEntity.CompositeKey(1, "soulseeker", blizzAccountId);
        String stubName = "Thelamar";
        WowCharacterEntity stub = new WowCharacterEntity();
        EquipmentEntity equipment = new EquipmentEntity();
        equipment.setItems(Map.of());
        stub.setEquipment(equipment);
        stub.setPk(pk);
        stub.setName(stubName);

        when(wowCharacterRepository.findById(pk)).thenReturn(Optional.of(stub));
        EquipmentResponse response = new EquipmentResponse(List.of());
        when(blizzardClient.getCharacterEquipment(stub.getPk().getRealmSlug(), stub.getName())).thenReturn(response);
        when(mapper.mapToDomain(any(EquipmentEntity.class))).thenReturn(new Equipment());

        // when
        service.getEquipmentForCharacter(stub.getPk().getId(), stub.getPk().getRealmSlug(), blizzAccountId);

        // then
        verify(wowCharacterRepository, times(1)).findById(stub.getPk());
        verify(blizzardClient, times(1)).getCharacterEquipment(any(String.class), any(String.class));
        verify(synchronizer, times(1)).synchronize(response, stub.getEquipment());
        verify(mapper, times(1)).mapToDomain(stub.getEquipment());
    }
}
