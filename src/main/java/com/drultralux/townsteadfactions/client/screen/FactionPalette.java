package com.drultralux.townsteadfactions.client.screen;

import dev.marie.MariesLib.client.MarieValueColors;

public final class FactionPalette {

    private FactionPalette() {}

    /**
     * Pushes custom mod palette values into MariesLib's global override channels.
     */
    public static void init() {

        //Bar Colours
        MarieValueColors.setOverride("power", 0xFFFF2A55);
        MarieValueColors.setOverride("ships", 0xFFD85DFF);
        MarieValueColors.setOverride("gold", 0xFFFFD65C);
        MarieValueColors.setOverride("food",0xFF55AAFF);
        MarieValueColors.setOverride("mana", 0xFF4DD9D9);

        //Text Colours
        MarieValueColors.setOverride("text_gold", 0xFFFFAA00);
        MarieValueColors.setOverride("text_white", 0xFFFFFFFF);
        MarieValueColors.setOverride("text_grey", 0xFFAAAAAA);
        MarieValueColors.setOverride("text_blue", 0xFF55FFFF);
        MarieValueColors.setOverride("text_muted", 0xFFAAAAAA);
        MarieValueColors.setOverride("text_pink", 0xFFFF55FF);
    }

    /**
     * Fetch a palette colour on demand.
     */
    public static int getBarColor(String resourceKey) {
        return MarieValueColors.baseColorArgb(resourceKey);
    }
}
