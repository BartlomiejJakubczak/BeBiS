package com.bebis.BeBiS.items;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemsController.class) // loads MVC layer, controllers and security filters
@AutoConfigureMockMvc(addFilters = false) // disable SecurityFilterChain for controller unit tests
public class ItemsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemsService itemsService;

    @Test
    void shouldReturnItem() throws Exception {
        // when
        int id = 2137;
        String expectedResponse = "papaj";
        when(itemsService.getItem(id)).thenReturn(expectedResponse);
        // then
        mockMvc.perform(get("/api/items/{id}", id))
            .andExpect(status().isOk())
            .andExpect(content().string(expectedResponse)); 
    }
    
}
