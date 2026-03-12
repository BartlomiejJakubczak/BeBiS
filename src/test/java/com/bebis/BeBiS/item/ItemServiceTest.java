package com.bebis.BeBiS.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;

@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

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
