package com.bebis.BeBiS.item;

public class ItemTestData {
    public static final int THUNDERFURY_ID = 19019;
    public static final String THUNDERFURY_NAME = "Thunderfury, Blessed Blade of the Windseeker";

    public static Item thunderfury() {
        return new Item(THUNDERFURY_ID, THUNDERFURY_NAME);
    }
}
