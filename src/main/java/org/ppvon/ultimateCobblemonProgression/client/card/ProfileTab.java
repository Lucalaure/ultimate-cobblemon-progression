package org.ppvon.ultimateCobblemonProgression.client.card;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.network.chat.Component;
import org.ppvon.ultimateCobblemonProgression.client.card.CardPainter.Align;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData;

import static org.ppvon.ultimateCobblemonProgression.client.card.CardStyle.*;

/** The player's portrait, region, tier, level cap and Pokédex, and their next challenge. */
final class ProfileTab extends CardTab {
    ProfileTab(TrainerCardScreen card) {
        super(card);
    }

    @Override
    boolean scrolls() {
        return false;
    }

    @Override
    void draw(GuiGraphics g, int mx, int my) {
        TrainerCardData data = data();
        // Portrait with the player's face, like a Pokémon's summary portrait
        g.blit(PORTRAIT, 24, 40, 0, 0, 66, 66, 66, 66);
        Minecraft minecraft = card.minecraft();
        if (minecraft != null && minecraft.player != null) {
            PlayerFaceRenderer.draw(g, minecraft.player.getSkin(), 35, 51, 44);
        }
        p.fit(g, Component.literal(data.player()).withStyle(ChatFormatting.BOLD), 57, 110, 70, INK, 1, Align.CENTRE);
        p.fit(g, Component.literal(data.regionMode() ? data.regionTitle() : "Trainer"), 57, 121, 70, MUTED, 1, Align.CENTRE);

        int x0 = 98, x1 = CX1;
        int y = 40;
        if (data.regionMode()) {
            String regions = data.regionTitle();
            for (TrainerCardData.LeagueView other : data.otherLeagues()) regions += " + " + other.title();
            p.row(g, x0, x1, y, "Region", Component.literal(regions).withStyle(ChatFormatting.BOLD), INK);
            y += 18;
            p.barRow(g, x0, x1, y, "Tier", data.tier(), data.maxTier(), DARK);
        } else {
            p.barRow(g, x0, x1, y, "Trainer level", data.tier(), data.maxTier(), DARK);
        }
        y += 18;
        p.row(g, x0, x1, y, "Level cap", Component.literal("Lv. " + data.levelCap()).withStyle(ChatFormatting.BOLD), RED);
        y += 18;
        if (data.regionMode() && !data.road().isEmpty()) {
            p.barRow(g, x0, x1, y, "Trainers beaten", (int) data.beaten(), data.road().size(), GREEN);
            y += 18;
        }
        p.row(g, x0, x1, y, "Pokédex", Component.literal(data.seen() + " seen  ·  " + data.caught() + " caught"), INK);

        // Next challenge
        int by0 = 136, by1 = CY1 - 2;
        g.fill(24, by0, CX1, by1, FRAME_DARK);
        g.fill(25, by0 + 1, CX1 - 1, by1 - 1, DARK);
        boolean hasNext = !data.next().getString().isEmpty();
        String label = data.allDone() ? "CHAMPION" : hasNext ? "NEXT CHALLENGE" : "NEXT STEP";
        p.text(g, Component.literal(label).withStyle(ChatFormatting.BOLD), 31, by0 + 5, LIGHT, 0.75f, false);
        int textW = CX1 - 34;
        int hintY = by0 + 15;
        if (hasNext) {
            p.fit(g, Component.literal("▶ ").append(data.next()).withStyle(ChatFormatting.BOLD), 31, by0 + 14, textW, WHITE, 1.25f, Align.LEFT);
            hintY = by0 + 28;
        }
        // Two leagues at once: the other league's next leader too.
        for (TrainerCardData.LeagueView other : data.otherLeagues()) {
            if (other.next().getString().isEmpty()) continue;
            p.fit(g, Component.literal("▶ " + other.title() + ": ").append(other.next()), 31, hintY, textW, WHITE, 1, Align.LEFT);
            hintY += 12;
        }
        // The hint can be long (e.g. the Pokémon still to catch): it shrinks to fit the box rather than being cut off.
        p.paragraph(g, data.hint(), 31, hintY, textW, by1 - 2 - hintY, LIGHT, 1, Align.LEFT);
    }
}
