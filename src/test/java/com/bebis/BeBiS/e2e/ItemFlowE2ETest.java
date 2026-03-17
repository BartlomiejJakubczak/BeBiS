package com.bebis.BeBiS.e2e;

import com.bebis.BeBiS.BaseIntegrationTest;
import com.bebis.BeBiS.item.Item;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static com.bebis.BeBiS.item.ItemTestData.THUNDERFURY_ID;
import static com.bebis.BeBiS.item.ItemTestData.thunderfury;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class ItemFlowE2ETest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate; // "browser in a box"

    @Test
    void shouldFetchThunderfuryFromRealBlizzard() {
        // when
        Item response =
                restTemplate.getForObject("/api/items/" + THUNDERFURY_ID, Item.class);
        // then
        assertEquals(thunderfury(), response);
    }

}
