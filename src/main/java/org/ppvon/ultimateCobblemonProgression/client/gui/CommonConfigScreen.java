package org.ppvon.ultimateCobblemonProgression.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.List;
import org.ppvon.ultimateCobblemonProgression.client.gui.widgets.ConfigList;
import org.ppvon.ultimateCobblemonProgression.config.CommonConfig;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

public final class CommonConfigScreen extends BaseConfigScreen {
    /** The highest gymVillageChance UCP uses, as a whole percent. */
    private static final int MAX_GYM_PERCENT = 95;
    private static final int GYM_PERCENT_STEP = 5;

    private Checkbox chkLevelCap, chkDex, chkTrainerCard, chkGymTeamLimit, chkCatchWholeTier, chkTwoLeagues, chkRegionStarters;
    private CycleButton<Integer> btnStarterLevel;
    private GymChanceSlider sldGymChance;
    private CycleButton<String> btnGymDifficulty, btnTwoLeagueCap, btnGymLegality;

    /** gymLegalityCheck values, in the order the button cycles through them. */
    private static final List<String> LEGALITY_MODES = List.of("off", "warn", "block");

    /** gymDifficulty values, in the order the button cycles through them. */
    private static final List<String> DIFFICULTIES = List.of("player_choice", "normal", "hard");

    public CommonConfigScreen(BaseConfigScreen parent) {
        super(parent, "CJP – General");
    }

    @Override
    protected void init() {
        super.init();
        CommonConfig c = ConfigLoader.common();
        ConfigList list = addSettingsList();

        chkLevelCap = list.add(Checkbox.builder(Component.literal("Enable Level Cap"), this.font)
                .selected(c.doLevelCap).build());
        chkDex = list.add(Checkbox.builder(Component.literal("Enable Dex Progression"), this.font)
                .selected(c.doDexProgression).build());
        chkTrainerCard = list.add(Checkbox.builder(Component.literal("Give Trainer Card on First Join"), this.font)
                .selected(c.giveTrainerCard).build());

        chkRegionStarters = list.add(Checkbox.builder(Component.literal("Starters From Your Region"), this.font)
                .selected(c.regionStarters)
                .tooltip(Tooltip.create(Component.literal(
                        "Players pick their starter after choosing where their journey starts, from that region's starters (and their second region's, if picked first).")))
                .build());
        List<Integer> levels = new java.util.ArrayList<>(List.of(0, 1, 5, 10, 15, 20));
        if (!levels.contains(c.starterLevel)) {
            levels.add(c.starterLevel);
            levels.sort(null);
        }
        btnStarterLevel = list.add(CycleButton.<Integer>builder(v -> Component.literal(v <= 0 ? "Cobblemon's" : "Lv. " + v))
                .withValues(levels)
                .withInitialValue(c.starterLevel)
                .withTooltip(v -> Tooltip.create(Component.literal(v <= 0
                        ? "Starters come at the level in Cobblemon's starter list (10)."
                        : "Starters come at level " + v + ", with the moves they know at that level.")))
                .create(0, 0, 150, 20, Component.literal("Starter Level")));

        chkTwoLeagues = list.add(Checkbox.builder(Component.literal("Allow Two Leagues"), this.font)
                .selected(c.allowTwoLeagues)
                .tooltip(Tooltip.create(Component.literal(
                        "Players may pick a second league with gym leaders as their second region, and work through both side by side.")))
                .build());
        String capMode = "lowest".equalsIgnoreCase(c.twoLeagueCap) ? "lowest" : "average";
        btnTwoLeagueCap = list.add(CycleButton.<String>builder(v -> Component.literal(v.equals("lowest") ? "Lowest" : "Average"))
                .withValues(List.of("average", "lowest"))
                .withInitialValue(capMode)
                .withTooltip(v -> Tooltip.create(Component.literal(v.equals("lowest")
                        ? "With two leagues, the level cap is the lower of the two leagues' caps: keep both leagues level."
                        : "With two leagues, the level cap is the average of the two leagues' caps, so it rises whichever you play.")))
                .create(0, 0, 150, 20, Component.literal("Two-League Level Cap")));

        chkGymTeamLimit = list.add(Checkbox.builder(Component.literal("Gym Team Limit"), this.font)
                .selected(c.gymTeamLimit).build());
        list.addNote("Bring no more Pokémon to a gym battle than the leader has");
        String legality = LEGALITY_MODES.contains(c.gymLegalityCheck) ? c.gymLegalityCheck : "off";
        btnGymLegality = list.add(CycleButton.<String>builder(CommonConfigScreen::legalityName)
                .withValues(LEGALITY_MODES)
                .withInitialValue(legality)
                .withTooltip(v -> Tooltip.create(Component.literal(switch (v) {
                    case "warn" -> "Gym leaders name party Pokémon from tiers you haven't unlocked, but battle anyway.";
                    case "block" -> "Gym leaders won't battle while your party has Pokémon from tiers you haven't unlocked. Your starter is always allowed.";
                    default -> "No check: bring any Pokémon to a gym battle.";
                })))
                .create(0, 0, 150, 20, Component.literal("Unlocked Pokémon Only")));
        String current = DIFFICULTIES.contains(c.gymDifficulty) ? c.gymDifficulty : "player_choice";
        btnGymDifficulty = list.add(CycleButton.<String>builder(CommonConfigScreen::difficultyName)
                .withValues(DIFFICULTIES)
                .withInitialValue(current)
                .withTooltip(v -> Tooltip.create(Component.literal(
                        "Gym leaders' teams. Player's choice: each player picks Normal or Hard on their Trainer Card and can change it any time.")))
                .create(0, 0, 150, 20, Component.literal("Gym Leaders")));

        chkCatchWholeTier = list.add(Checkbox.builder(Component.literal("Catch Every Pokémon to Advance"), this.font)
                .selected(c.catchWholeTier).build());
        list.addNote("Catch all of a tier's Pokémon before its next gym leader");

        sldGymChance = list.add(new GymChanceSlider(0, 0, 150, 20, c.gymVillageChance));
        list.addNote("Applies to newly generated villages after the world restarts");
    }

    private static Component legalityName(String value) {
        return Component.literal(switch (value) {
            case "warn" -> "Warn";
            case "block" -> "Block";
            default -> "Off";
        });
    }

    private static Component difficultyName(String value) {
        return Component.literal(switch (value) {
            case "normal" -> "Normal";
            case "hard" -> "Hard";
            default -> "Player's choice";
        });
    }

    @Override
    protected void applyAndSave() {
        CommonConfig c = ConfigLoader.common();
        c.doLevelCap = chkLevelCap.selected();
        c.doDexProgression = chkDex.selected();
        c.giveTrainerCard = chkTrainerCard.selected();
        c.regionStarters = chkRegionStarters.selected();
        c.starterLevel = btnStarterLevel.getValue();
        c.gymTeamLimit = chkGymTeamLimit.selected();
        c.gymLegalityCheck = btnGymLegality.getValue();
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
