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

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(ItemsController.class) // loads MVC layer, controllers and security filters
@AutoConfigureMockMvc(addFilters = false) // disable SecurityFilterChain for controller unit tests
public class ItemsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemsService itemsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnItem() throws Exception {
        // given
        Item thunderfury = ItemTestData.thunderfury();
        // when
        when(itemsService.getItem(thunderfury.id())).thenReturn(thunderfury);
        // then
        mockMvc.perform(get("/api/items/{id}", thunderfury.id()))
            .andExpect(status().isOk())
            .andExpect(content().json(objectMapper.writeValueAsString(thunderfury))); 
    }
    
}
