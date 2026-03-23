package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.bebis.BeBiS.item.ItemTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @Mock
    private BlizzardServiceClient blizzardClient;

    private final ItemMapper itemMapper = new ItemMapper();

    private ItemService itemsService;

    @BeforeEach
    void setup() {
        itemsService = new ItemService(blizzardClient, itemMapper);
    }

    @Test
    void shouldGetItemFromBlizzard() {
        // when
        when(blizzardClient.getItem(THUNDERFURY_ID)).thenReturn(thunderfuryResponse());
        Item response = itemsService.getItem(THUNDERFURY_ID);
        // then
        assertEquals(thunderfury(), response);
        verify(blizzardClient).getItem(THUNDERFURY_ID);
    }

}
