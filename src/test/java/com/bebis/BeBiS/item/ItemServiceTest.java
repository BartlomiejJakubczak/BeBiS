package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private ItemService itemsService;

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
