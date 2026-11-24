package org.ppvon.ultimateCobblemonProgression.client.gui;

import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.ppvon.ultimateCobblemonProgression.client.gui.widgets.DoubleField;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;
import org.ppvon.ultimateCobblemonProgression.config.SpawnConfig;

public final class SpawnConfigScreen extends BaseConfigScreen {
    private boolean doSpeciesBlocking, blockUnknownSpecies, blockUnlistedTrainers, doLevelScaling, doWeightScaling;
    private double tierCapScaling, minLevelScaling, avgLevelScaling, maxLevelScaling;
    private double weightDecayPerTier, weightMinFactor, weightCurrentTierBuff;

    private Checkbox chkDoSpeciesBlocking, chkBlockUnknownSpecies, chkBlockUnlistedTrainers, chkDoLevelScaling,
            chkDoWeightScaling;
    private DoubleField fldTierCap, fldMin, fldAvg, fldMax, fldDecay, fldMinFactor, fldBuff;

    SpawnConfigScreen(BaseConfigScreen parent) {
        super(parent, "CJP – Spawn Config");
    }

    @Override
    protected void init() {
        super.init();

        SpawnConfig c = ConfigLoader.spawn();
        doSpeciesBlocking      = c.doSpeciesBlocking;
        blockUnknownSpecies    = c.blockUnknownSpecies;
        blockUnlistedTrainers  = c.blockUnlistedTrainers;
        doLevelScaling         = c.doLevelScaling;
        tierCapScaling         = c.tierCapScaling;
        minLevelScaling        = c.minLevelScaling;
        avgLevelScaling        = c.avgLevelScaling;
        maxLevelScaling        = c.maxLevelScaling;
        doWeightScaling        = c.doWeightScaling;
        weightDecayPerTier     = c.weightDecayPerTier;
        weightMinFactor        = c.weightMinFactor;
        weightCurrentTierBuff  = c.weightCurrentTierBuff;

        int cx = width / 2, y = 34, labelX = cx - 150, fieldX = cx + 30, rowH = 22;

        java.util.function.BiConsumer<String, Integer> label = (text, yy) ->
                this.addRenderableOnly((gfx, mx, my, d) ->
                        gfx.drawString(this.font, text, labelX, yy + 6, 0xFFA0A0A0, false));

        // --- Species blocking ---
        chkDoSpeciesBlocking = Checkbox.builder(Component.literal("Enable Species Blocking"), this.font)
                .selected(doSpeciesBlocking).pos(labelX, y).build();
        addRenderableWidget(chkDoSpeciesBlocking);

        // Same row, so the screen doesn't grow taller.
        chkBlockUnlistedTrainers = Checkbox.builder(Component.literal("Block Unlisted Trainers"), this.font)
                .selected(blockUnlistedTrainers).pos(fieldX, y)
                .tooltip(Tooltip.create(Component.literal(
                        "Region mode: Radical Cobblemon Trainers' wild trainers that no region lists (like Pokémon Unbound's) never spawn.")))
                .build();
        addRenderableWidget(chkBlockUnlistedTrainers);

        y += rowH;

        chkBlockUnknownSpecies = Checkbox.builder(Component.literal("Block Unknown Species"), this.font)
                .selected(blockUnknownSpecies).pos(labelX, y).build();
        addRenderableWidget(chkBlockUnknownSpecies);

        y += rowH + 6;

        // --- Level scaling ---
        chkDoLevelScaling = Checkbox.builder(Component.literal("Enable Level Scaling"), this.font)
                .selected(doLevelScaling).pos(labelX, y).build();
        addRenderableWidget(chkDoLevelScaling);

        y += rowH;

        label.accept("Tier Cap Scaling", y);
        fldTierCap = new DoubleField(this.font, fieldX, y, 100, 20, tierCapScaling);
        fldTierCap.setOnValidChange(v -> tierCapScaling = v);
        addRenderableWidget(fldTierCap);

        y += rowH;

        label.accept("Min Level % of Cap", y);
        fldMin = new DoubleField(this.font, fieldX, y, 100, 20, minLevelScaling);
        fldMin.setOnValidChange(v -> minLevelScaling = v);
        addRenderableWidget(fldMin);

        y += rowH;

        label.accept("Avg Level % of Cap", y);
        fldAvg = new DoubleField(this.font, fieldX, y, 100, 20, avgLevelScaling);
        fldAvg.setOnValidChange(v -> avgLevelScaling = v);
        addRenderableWidget(fldAvg);

        y += rowH;

        label.accept("Max Level % of Cap", y);
        fldMax = new DoubleField(this.font, fieldX, y, 100, 20, maxLevelScaling);
        fldMax.setOnValidChange(v -> maxLevelScaling = v);
        addRenderableWidget(fldMax);

        y += rowH + 8;

        // --- Weight scaling ---
        chkDoWeightScaling = Checkbox.builder(Component.literal("Enable Weight Scaling"), this.font)
                .selected(doWeightScaling).pos(labelX, y).build();
        addRenderableWidget(chkDoWeightScaling);

        y += rowH;

        label.accept("Weight Decay Per Tier", y);
        fldDecay = new DoubleField(this.font, fieldX, y, 100, 20, weightDecayPerTier);
        fldDecay.setOnValidChange(v -> weightDecayPerTier = v);
        addRenderableWidget(fldDecay);

        y += rowH;

        label.accept("Weight Min Factor", y);
        fldMinFactor = new DoubleField(this.font, fieldX, y, 100, 20, weightMinFactor);
        fldMinFactor.setOnValidChange(v -> weightMinFactor = v);
        addRenderableWidget(fldMinFactor);

        y += rowH;

        label.accept("Current-Tier Weight Buff", y);
        fldBuff = new DoubleField(this.font, fieldX, y, 100, 20, weightCurrentTierBuff);
        fldBuff.setOnValidChange(v -> weightCurrentTierBuff = v);
        addRenderableWidget(fldBuff);

        Runnable refresh = this::updateDoneState;
        fldTierCap.setOnValidityChange(refresh);
        fldMin.setOnValidityChange(refresh);
        fldAvg.setOnValidityChange(refresh);
        fldMax.setOnValidityChange(refresh);
        fldDecay.setOnValidityChange(refresh);
        fldMinFactor.setOnValidityChange(refresh);
        fldBuff.setOnValidityChange(refresh);

        updateDoneState();
    }

    private void updateDoneState() {
        boolean ok = fldTierCap.isValidNumber()
                && fldMin.isValidNumber()
                && fldAvg.isValidNumber()
                && fldMax.isValidNumber()
                && fldDecay.isValidNumber()
                && fldMinFactor.isValidNumber()
                && fldBuff.isValidNumber();
        setDoneEnabled(ok);
    }

    @Override
    protected void applyAndSave() {
        var c = ConfigLoader.spawn();
        c.doSpeciesBlocking = chkDoSpeciesBlocking.selected();
        c.blockUnknownSpecies = chkBlockUnknownSpecies.selected();
        c.blockUnlistedTrainers = chkBlockUnlistedTrainers.selected();

        c.doLevelScaling = chkDoLevelScaling.selected();
        c.tierCapScaling = fldTierCap.getOrElse(tierCapScaling);
        c.minLevelScaling = fldMin.getOrElse(minLevelScaling);
        c.avgLevelScaling = fldAvg.getOrElse(avgLevelScaling);
        c.maxLevelScaling = fldMax.getOrElse(maxLevelScaling);

        c.doWeightScaling = chkDoWeightScaling.selected();
        c.weightDecayPerTier = fldDecay.getOrElse(weightDecayPerTier);
        c.weightMinFactor = fldMinFactor.getOrElse(weightMinFactor);
        c.weightCurrentTierBuff = fldBuff.getOrElse(weightCurrentTierBuff);

        ConfigLoader.saveAll();
    }
}
