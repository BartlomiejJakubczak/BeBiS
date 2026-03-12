package com.bebis.BeBiS.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.item.Item;
import com.bebis.BeBiS.item.ItemService;

@ExtendWith(MockitoExtension.class)
public class ItemsServiceTest {

    @Mock
    private BlizzardServiceClient blizzardClient;

    @InjectMocks
    private ItemService itemsService;

    @Test
    void shouldGetItemFromBlizzard() {
        // given
        Item thunderfury = ItemTestData.thunderfury();
        // when
        when(blizzardClient.getItem(thunderfury.id())).thenReturn(thunderfury);
        Item response = itemsService.getItem(thunderfury.id());
        // then
        assertEquals(thunderfury, response);
        verify(blizzardClient).getItem(thunderfury.id());
    }

}
