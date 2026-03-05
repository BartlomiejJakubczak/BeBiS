package com.bebis.BeBiS.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bebis.BeBiS.integration.blizzard.BlizzardClient;

@ExtendWith(MockitoExtension.class)
public class ItemsServiceTest {

    @Mock
    private BlizzardClient blizzardClient;

    @InjectMocks
    private ItemsService itemsService;

    @Test
    void shouldGetItemFromBlizzard() {
        // given
        int id = 2137;
        String expectedResponse = "papaj";
        // when
        when(blizzardClient.getItem(id)).thenReturn(expectedResponse);
        String response = itemsService.getItem(id);
        // then
        assertEquals(expectedResponse, response);
        verify(blizzardClient).getItem(id);
    }

}
