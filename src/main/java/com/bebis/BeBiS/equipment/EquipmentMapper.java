package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EquipmentMapper {

    public Map<Equipment.Slot, Long> map(EquipmentResponse equipmentResponse) {
        return equipmentResponse.equipment().stream().
                collect(Collectors.toMap(
                        itemDTO -> Equipment.Slot.valueOf(itemDTO.slot().type().toUpperCase()),
                        itemDTO -> itemDTO.item().id()
                ));
    }

}
