package org.ppvon.ultimateCobblemonProgression.client.card;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.ppvon.ultimateCobblemonProgression.client.card.CardPainter.Align;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.WildGroup;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.WildSpecies;

import java.util.ArrayList;
import java.util.List;

import static org.ppvon.ultimateCobblemonProgression.client.card.CardStyle.*;

/** The wild Pokémon the player can meet, by group, with Cobblemon's caught/seen markers and a tooltip per species. */
final class WildTab extends CardTab {
    /** The species under the mouse, for the tooltip, or null. Set while drawing. */
    WildSpecies hovered;

    WildTab(TrainerCardScreen card) {
        super(card);
    }

    @Override
    void draw(GuiGraphics g, int mx, int my) {
        int columns = 3;
        int cellW = (CX1 - CX0 - 8) / columns;
        int y = CY0 + 2;
        boolean mouseInView = my >= CY0 && my < CY1 && mx >= CX0 && mx < CX1;
        int contentMouseY = contentMouseY(my);

        for (WildGroup group : data().wild()) {
            int hx0 = CX0 + 2, hx1 = CX1 - 6;
            g.fill(hx0, y, hx1, y + 13, DARK);
            String count = group.species().size() + " species" + (group.upcoming() > 0 ? "  ·  +" + group.upcoming() + " next tier" : "");
            float countW = p.font().width(count) * 0.75f;
            p.text(g, Component.literal(count), hx1 - 5 - countW, y + 4.5f, LIGHT, 0.75f, false);
            p.fit(g, Component.literal(group.title()).withStyle(ChatFormatting.BOLD), hx0 + 5, y + 3,
                    hx1 - 11 - countW - (hx0 + 5), WHITE, 1, Align.LEFT);
            y += 15;

            if (group.species().isEmpty()) {
                p.text(g, Component.literal("None yet"), hx0 + 5, y + 2, MUTED, 1, false);
                y += 13;
                continue;
            }
            int col = 0;
            for (WildSpecies s : group.species()) {
                Species sp = PokemonSpecies.getByIdentifier(s.id());
                int cx = hx0 + col * cellW;
                boolean hover = mouseInView && mx >= cx && mx < cx + cellW - 2 && contentMouseY >= y && contentMouseY < y + 12;
                if (hover) {
                    g.fill(cx, y, cx + cellW - 2, y + 12, LIGHT);
                    hovered = sp != null ? s : null;
                }
                p.dexIcon(g, cx + 2, y + 2, s.knowledge());
                // Without the species (not loaded, as outside a world), Cobblemon's language file still has its name.
                Component name = sp != null ? sp.getTranslatedName()
                        : Component.translatableWithFallback(s.id().getNamespace() + ".species." + s.id().getPath() + ".name", s.id().getPath());
                if (!s.prefix().isEmpty()) name = Component.literal(s.prefix() + " ").append(name);
                int colour = s.knowledge() == TrainerCardData.UNKNOWN ? MUTED : INK;
                p.fit(g, name, cx + 13, y + 2, cellW - 16, colour, 1, Align.LEFT);
                col++;
                if (col == columns) {
                    col = 0;
                    y += 12;
                }
            }
            if (col != 0) y += 12;
            y += 4;
        }
        p.text(g, Component.literal("Ball: caught   ·   Outline: seen   ·   Faded: not yet seen"), CX0 + 4, y + 1, MUTED, 0.75f, false);
        y += 10;
        card.contentHeight = y - CY0 + 2;
    }

    /** The tooltip for a species: its name and number, and its types (a regional form's own). */
    static List<Component> tooltip(WildSpecies s) {
        Species sp = PokemonSpecies.getByIdentifier(s.id());
        List<Component> lines = new ArrayList<>();
        if (sp == null) return lines;
        // A regional form has its own types (Alolan Rattata is Dark/Normal).
        FormData form = s.form().isEmpty() ? null : sp.getFormByName(s.form());
        MutableComponent name = s.prefix().isEmpty() ? Component.empty() : Component.literal(s.prefix() + " ");
        lines.add(name.append(sp.getTranslatedName()).withStyle(ChatFormatting.BOLD)
                .append(Component.literal("  #" + String.format("%04d", sp.getNationalPokedexNumber())).withStyle(ChatFormatting.GRAY)));
        MutableComponent types = Component.empty();
        types.append(typeName(form != null ? form.getPrimaryType() : sp.getPrimaryType()));
        ElementalType second = form != null ? form.getSecondaryType() : sp.getSecondaryType();
        if (second != null) types.append(Component.literal(" / ").withStyle(ChatFormatting.GRAY)).append(typeName(second));
        lines.add(types);
        return lines;
    }

    private static Component typeName(ElementalType type) {
        return type.getDisplayName().copy().withStyle(s -> s.withColor(type.getHue()));
    }
}
