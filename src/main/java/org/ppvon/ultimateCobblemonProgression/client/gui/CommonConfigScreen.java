package org.ppvon.ultimateCobblemonProgression.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.List;
import org.ppvon.ultimateCobblemonProgression.config.CommonConfig;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

public final class CommonConfigScreen extends BaseConfigScreen {
    /** The highest gymVillageChance UCP uses, as a whole percent. */
    private static final int MAX_GYM_PERCENT = 95;
    private static final int GYM_PERCENT_STEP = 5;

    private Checkbox chkLevelCap, chkDex, chkTrainerCard, chkGymTeamLimit, chkCatchWholeTier, chkTwoLeagues;
    private GymChanceSlider sldGymChance;
    private CycleButton<String> btnGymDifficulty, btnTwoLeagueCap;

    /** gymDifficulty values, in the order the button cycles through them. */
    private static final List<String> DIFFICULTIES = List.of("player_choice", "normal", "hard");

    public CommonConfigScreen(BaseConfigScreen parent) {
        super(parent, "CJP – General");
    }

    @Override
    protected void init() {
        super.init();
        CommonConfig c = ConfigLoader.common();

        int cx = width / 2, x = cx - 150, y = 40, rowH = 22;

        chkLevelCap = addRenderableWidget(Checkbox.builder(Component.literal("Enable Level Cap"), this.font)
                .selected(c.doLevelCap).pos(x, y).build());
        y += rowH;

        chkDex = addRenderableWidget(Checkbox.builder(Component.literal("Enable Dex Progression"), this.font)
                .selected(c.doDexProgression).pos(x, y).build());
        y += rowH;

        chkTrainerCard = addRenderableWidget(Checkbox.builder(Component.literal("Give Trainer Card on First Join"), this.font)
                .selected(c.giveTrainerCard).pos(x, y).build());
        // Same row, so the screen doesn't grow taller.
        chkTwoLeagues = addRenderableWidget(Checkbox.builder(Component.literal("Allow Two Leagues"), this.font)
                .selected(c.allowTwoLeagues).pos(cx + 10, y)
                .tooltip(Tooltip.create(Component.literal(
                        "Players may pick a second league with gym leaders as their second region, and work through both side by side.")))
                .build());
        y += rowH + 8;

        chkGymTeamLimit = addRenderableWidget(Checkbox.builder(Component.literal("Gym Team Limit"), this.font)
                .selected(c.gymTeamLimit).pos(x, y).build());
        // Same row, so the screen doesn't grow taller.
        String current = DIFFICULTIES.contains(c.gymDifficulty) ? c.gymDifficulty : "player_choice";
        btnGymDifficulty = addRenderableWidget(CycleButton.<String>builder(CommonConfigScreen::difficultyName)
                .withValues(DIFFICULTIES)
                .withInitialValue(current)
                .withTooltip(v -> Tooltip.create(Component.literal(
                        "Gym leaders' teams. Player's choice: each player picks Normal or Hard on their Trainer Card and can change it any time.")))
                .create(cx + 10, y, 140, 20, Component.literal("Leaders")));
        addNote("Bring no more Pokémon to a gym battle than the leader has", x, y + rowH);
        y += rowH * 2;

        chkCatchWholeTier = addRenderableWidget(Checkbox.builder(Component.literal("Catch Every Pokémon to Advance"), this.font)
                .selected(c.catchWholeTier).pos(x, y).build());
        // Same row, so the screen doesn't grow taller: the two-league level cap rule.
        String capMode = "lowest".equalsIgnoreCase(c.twoLeagueCap) ? "lowest" : "average";
        btnTwoLeagueCap = addRenderableWidget(CycleButton.<String>builder(v -> Component.literal(v.equals("lowest") ? "Lowest" : "Average"))
                .withValues(List.of("average", "lowest"))
                .withInitialValue(capMode)
                .withTooltip(v -> Tooltip.create(Component.literal(v.equals("lowest")
                        ? "With two leagues, the level cap is the lower of the two leagues' caps: keep both leagues level."
                        : "With two leagues, the level cap is the average of the two leagues' caps, so it rises whichever you play.")))
                .create(cx + 45, y, 105, 20, Component.literal("Cap")));
        addNote("Catch all of a tier's Pokémon before its next gym leader", x, y + rowH);
        y += rowH * 2;

        sldGymChance = addRenderableWidget(new GymChanceSlider(x, y, 300, 20, c.gymVillageChance));
        addNote("Applies to newly generated villages after the world restarts", x, y + rowH);
    }

    private static Component difficultyName(String value) {
        return Component.literal(switch (value) {
            case "normal" -> "Normal";
            case "hard" -> "Hard";
            default -> "Player's choice";
        });
    }

    private void addNote(String text, int x, int y) {
        addRenderableOnly((gfx, mx, my, d) -> gfx.drawString(this.font, text, x + 2, y + 2, 0xFFA0A0A0, false));
    }

    @Override
    protected void applyAndSave() {
        CommonConfig c = ConfigLoader.common();
        c.doLevelCap = chkLevelCap.selected();
        c.doDexProgression = chkDex.selected();
        c.giveTrainerCard = chkTrainerCard.selected();
        c.gymTeamLimit = chkGymTeamLimit.selected();
        c.gymDifficulty = btnGymDifficulty.getValue();
        c.catchWholeTier = chkCatchWholeTier.selected();
        c.allowTwoLeagues = chkTwoLeagues.selected();
        c.twoLeagueCap = btnTwoLeagueCap.getValue();
        c.gymVillageChance = sldGymChance.percent() / 100.0;
        ConfigLoader.saveAll();
    }

    /** Share of new villages with a gym, from 0% (off) to 95% in steps of 5%. */
    private static final class GymChanceSlider extends AbstractSliderButton {
        GymChanceSlider(int x, int y, int width, int height, double chance) {
            super(x, y, width, height, Component.empty(),
                    Math.max(0, Math.min(MAX_GYM_PERCENT, chance * 100)) / MAX_GYM_PERCENT);
            updateMessage();
        }

        int percent() {
            return (int) Math.round(value * MAX_GYM_PERCENT / GYM_PERCENT_STEP) * GYM_PERCENT_STEP;
        }

        @Override
        protected void updateMessage() {
            int p = percent();
            setMessage(Component.literal("Villages with a Gym: " + (p == 0 ? "Off" : p + "%")));
        }

        @Override
        protected void applyValue() {}
    }
}
