package org.ppvon.ultimateCobblemonProgression.client.gui;

import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.ppvon.ultimateCobblemonProgression.client.gui.widgets.ConfigList;
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

    public SpawnConfigScreen(BaseConfigScreen parent) {
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

        ConfigList list = addSettingsList();

        // --- Species blocking ---
        chkDoSpeciesBlocking = list.add(Checkbox.builder(Component.literal("Enable Species Blocking"), this.font)
                .selected(doSpeciesBlocking).build());
        chkBlockUnknownSpecies = list.add(Checkbox.builder(Component.literal("Block Unknown Species"), this.font)
                .selected(blockUnknownSpecies).build());
        chkBlockUnlistedTrainers = list.add(Checkbox.builder(Component.literal("Block Unlisted Trainers"), this.font)
                .selected(blockUnlistedTrainers)
                .tooltip(Tooltip.create(Component.literal(
                        "Region mode: Radical Cobblemon Trainers' wild trainers that no region lists (like Pokémon Unbound's) never spawn.")))
                .build());

        // --- Level scaling ---
        chkDoLevelScaling = list.add(Checkbox.builder(Component.literal("Enable Level Scaling"), this.font)
                .selected(doLevelScaling).build());
        fldTierCap = list.addLabelled("Tier Cap Scaling", new DoubleField(this.font, 0, 0, 100, 20, tierCapScaling));
        fldTierCap.setOnValidChange(v -> tierCapScaling = v);
        fldMin = list.addLabelled("Min Level % of Cap", new DoubleField(this.font, 0, 0, 100, 20, minLevelScaling));
        fldMin.setOnValidChange(v -> minLevelScaling = v);
        fldAvg = list.addLabelled("Avg Level % of Cap", new DoubleField(this.font, 0, 0, 100, 20, avgLevelScaling));
        fldAvg.setOnValidChange(v -> avgLevelScaling = v);
        fldMax = list.addLabelled("Max Level % of Cap", new DoubleField(this.font, 0, 0, 100, 20, maxLevelScaling));
        fldMax.setOnValidChange(v -> maxLevelScaling = v);

        // --- Weight scaling ---
        chkDoWeightScaling = list.add(Checkbox.builder(Component.literal("Enable Weight Scaling"), this.font)
                .selected(doWeightScaling).build());
        fldDecay = list.addLabelled("Weight Decay Per Tier", new DoubleField(this.font, 0, 0, 100, 20, weightDecayPerTier));
        fldDecay.setOnValidChange(v -> weightDecayPerTier = v);
        fldMinFactor = list.addLabelled("Weight Min Factor", new DoubleField(this.font, 0, 0, 100, 20, weightMinFactor));
        fldMinFactor.setOnValidChange(v -> weightMinFactor = v);
        fldBuff = list.addLabelled("Current-Tier Weight Buff", new DoubleField(this.font, 0, 0, 100, 20, weightCurrentTierBuff));
        fldBuff.setOnValidChange(v -> weightCurrentTierBuff = v);

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
