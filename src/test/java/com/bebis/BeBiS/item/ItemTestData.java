package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;

public class ItemTestData {
    public static final long THUNDERFURY_ID = 19019;
    public static final String THUNDERFURY_NAME = "Thunderfury, Blessed Blade of the Windseeker";

    public static ItemResponse thunderfuryResponse() {
        return new ItemResponse(THUNDERFURY_ID, THUNDERFURY_NAME);
    }

    public static Item thunderfury() {
        return new Item(THUNDERFURY_ID, THUNDERFURY_NAME);
    }
}
