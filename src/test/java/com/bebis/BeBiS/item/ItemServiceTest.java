package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.jpa.ArmorEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntityFactory;
import com.bebis.BeBiS.item.jpa.ItemRepository;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private BlizzardServiceClient blizzardClient;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ItemEntityFactory itemEntityFactory;

    private final ItemMapper itemMapper = new ItemMapper();
    private ItemService itemService;

    private static final long BASE_ENCH = 0L;
    private static final long SUFFIX_ENCH = 37L; // "of the Tiger", etc.

    @BeforeEach
    void setup() {
        itemService = new ItemService(blizzardClient, itemRepository, itemMapper, itemEntityFactory);
    }

    @Test
    void shouldDelegateToRepoIfEntityExists() {
        // given
        ItemEntity.CompositeKey pk = new ItemEntity.CompositeKey(ItemTestData.THUNDERFURY_ID, BASE_ENCH);
        when(itemRepository.findById(pk)).thenReturn(Optional.of(new WeaponEntity()));

        // when
        itemService.getOrCreateEntity(ItemTestData.THUNDERFURY_ID, BASE_ENCH);

        // then
        verify(itemRepository, times(1)).findById(pk);
        verifyNoInteractions(blizzardClient);
        verifyNoInteractions(itemEntityFactory);
    }

    @Test
    void shouldDelegateToFactoryWhenItemIsMissing() {
        // given
        long armorId = 12345L;
        String armorName = "Plate Chest";
        ItemResponse armorResponse = ItemTestData.armorResponse(armorId, armorName, 500, 4);

        when(itemRepository.findById(any())).thenReturn(Optional.empty());
        when(blizzardClient.getBaseItem(armorId)).thenReturn(armorResponse);
        when(itemEntityFactory.createItemEntity(any())).thenReturn(new ArmorEntity());

        // when
        itemService.getOrCreateEntity(armorId, BASE_ENCH);

        // then
        verify(itemEntityFactory).createItemEntity(argThat(data ->
                data.itemId() == armorId &&
                        data.armorValue() == 500 && // Proves the Mapper + Service pipeline is intact
                        data.name().equals(armorName)
        ));
    }

    @Test
    void shouldFetchNewVariantEvenIfBaseItemExistsInRepo() {
        // given
        long itemId = 12345L;
        ItemEntity.CompositeKey requestedPk = new ItemEntity.CompositeKey(itemId, SUFFIX_ENCH);

        when(itemRepository.findById(requestedPk)).thenReturn(Optional.empty()); // 21, 37 not in db
        when(blizzardClient.getBaseItem(itemId)).thenReturn(ItemTestData.thunderfuryResponse());
        when(itemEntityFactory.createItemEntity(any())).thenReturn(new WeaponEntity());

        // when
        itemService.getOrCreateEntity(itemId, SUFFIX_ENCH);

        // then
        verify(itemRepository).findById(requestedPk);
        verify(blizzardClient).getBaseItem(itemId);
        verify(itemEntityFactory).createItemEntity(argThat(data -> data.randomEnchId() == SUFFIX_ENCH));
    }
}