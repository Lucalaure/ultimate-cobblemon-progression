package org.ppvon.ultimateCobblemonProgression.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import org.ppvon.ultimateCobblemonProgression.config.CommonConfig;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

public final class CommonConfigScreen extends BaseConfigScreen {
    /** The highest gymVillageChance UCP uses, as a whole percent. */
    private static final int MAX_GYM_PERCENT = 95;
    private static final int GYM_PERCENT_STEP = 5;

    private Checkbox chkLevelCap, chkDex, chkTrainerCard, chkGymTeamLimit, chkCatchWholeTier;
    private GymChanceSlider sldGymChance;

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
        y += rowH + 8;

        chkGymTeamLimit = addRenderableWidget(Checkbox.builder(Component.literal("Gym Team Limit"), this.font)
                .selected(c.gymTeamLimit).pos(x, y).build());
        addNote("Bring no more Pokémon to a gym battle than the leader has", x, y + rowH);
        y += rowH * 2;

        chkCatchWholeTier = addRenderableWidget(Checkbox.builder(Component.literal("Catch Every Pokémon to Advance"), this.font)
                .selected(c.catchWholeTier).pos(x, y).build());
        addNote("Catch all of a tier's Pokémon before its next gym leader", x, y + rowH);
        y += rowH * 2;

        sldGymChance = addRenderableWidget(new GymChanceSlider(x, y, 300, 20, c.gymVillageChance));
        addNote("Applies to newly generated villages after the world restarts", x, y + rowH);
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
        c.catchWholeTier = chkCatchWholeTier.selected();
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
