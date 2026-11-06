package com.bebis.BeBiS.item;

import com.bebis.BeBiS.equipment.EquipmentResponseBuilder;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.dto.ItemSyncData;

import java.util.List;


/**
 * Converts Blizzard responses to domain Items using the real pipeline.
 * For now convert assumes Item with no player enchants on it and no random suffices.
 */
public final class BenchmarkFixturesConverter {

    private final ItemMapper mapper = new ItemMapper();
    private final ItemEntityFactory factory = new ItemEntityFactory();

    public Item convert(ItemResponse itemResponse) {
        ItemSyncData syncData = mapper.mapToSyncData(itemResponse, EquipmentResponseBuilder.newInstance(itemResponse).build());
        return mapper.mapToDomain(factory.createItemEntity(syncData), List.of()); // in this instance it doesn't matter where the items came from
    }
}
