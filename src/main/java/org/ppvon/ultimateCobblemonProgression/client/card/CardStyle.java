package org.ppvon.ultimateCobblemonProgression.client.card;

import net.minecraft.resources.ResourceLocation;

/** The Trainer Card's size, layout and colours: Cobblemon's Pokédex style. */
final class CardStyle {
    private CardStyle() {}

    /** The card's size, in card coordinates. */
    static final int W = 345, H = 207;

    static final ResourceLocation FRAME = cobblemon("textures/gui/pokedex/pokedex_base_red.png");
    static final ResourceLocation PORTRAIT = cobblemon("textures/gui/summary/portrait_background.png");
    static final ResourceLocation DEX_ICON = cobblemon("textures/gui/pokedex/caught_seen_icon.png");

    // Cobblemon's Pokédex palette
    static final int SCREEN = 0xFF5BD1DA, LIGHT = 0xFF7EE7E5, MID = 0xFF44B3C8, DARK = 0xFF3A96B6;
    static final int INK = 0xFF1B4652, MUTED = 0xFF3F7584, WHITE = 0xFFFFFFFF, FRAME_DARK = 0xFF2F2F2F;
    static final int RED = 0xFFD74845, GREEN = 0xFF3E9E4F, GOLD = 0xFFE2A82C, GREY = 0xFF8AA9B3;
    /** A red button under the mouse. */
    static final int RED_HOVER = 0xFFE8574F;

    // content area, in card coordinates
    static final int CX0 = 20, CY0 = 38, CX1 = 325, CY1 = 189;

    private static ResourceLocation cobblemon(String path) {
        return ResourceLocation.fromNamespaceAndPath("cobblemon", path);
    }
}
