package org.ppvon.ultimateCobblemonProgression.common.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierRegistry;

import java.util.Optional;

public class TrainerLevelComponentImpl implements TrainerLevelComponent, AutoSyncedComponent {
    private int level = 1;
    private String startRegion = null;
    private final Player player;
    private boolean loadedFromNbt = false;

    public TrainerLevelComponentImpl(Player p) {
        this.player = p;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        int max = TierRegistry.maxTier();
        int actual = Math.clamp(level, 1, max);
        this.level = actual;
        TrainerLevelComponents.KEY.sync(this.player);
    }

    @Override
    public Optional<String> getStartRegion() {
        return Optional.ofNullable(startRegion);
    }

    @Override
    public void setStartRegion(String regionId) {
        this.startRegion = regionId;
        TrainerLevelComponents.KEY.sync(this.player);
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        if (tag.contains("Level")) {
            this.level = tag.getInt("Level");
            this.loadedFromNbt = true;
        }
        this.startRegion = tag.contains("StartRegion") ? tag.getString("StartRegion") : null;
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        tag.putInt("Level", this.level);
        if (this.startRegion != null) {
            tag.putString("StartRegion", this.startRegion);
        }
    }

    public boolean loadedFromNbt() {
        return loadedFromNbt;
    }
}
