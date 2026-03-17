package com.bebis.BeBiS.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.bebis.BeBiS.item.ItemTestData.THUNDERFURY_ID;
import static com.bebis.BeBiS.item.ItemTestData.thunderfury;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class) // loads MVC layer, controllers and security filters
@AutoConfigureMockMvc(addFilters = false) // disable SecurityFilterChain for controller unit tests
public class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnItem() throws Exception {
        // when
        when(itemsService.getItem(THUNDERFURY_ID)).thenReturn(thunderfury());
        // then
        mockMvc.perform(get("/api/items/{id}", THUNDERFURY_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(thunderfury())));
    }

}
