package org.ppvon.ultimateCobblemonProgression.common.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TrainerLevelComponentImpl implements TrainerLevelComponent, AutoSyncedComponent {
    private static final String BEATEN_WILD_TRAINERS = "BeatenWildTrainers";

    private int level = 1;
    private String startRegion = null;
    private String homeRegion = null;
    private String gymDifficulty = null;
    private final List<String> nextRegions = new ArrayList<>();
    /** Server only: never sent to clients (see writeSyncPacket), since the Trainer Card is worked out on the server. */
    private final Set<String> beatenWildTrainers = new LinkedHashSet<>();
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
    public Optional<String> getHomeRegion() {
        return Optional.ofNullable(homeRegion);
    }

    @Override
    public void setHomeRegion(String regionId) {
        this.homeRegion = regionId;
        TrainerLevelComponents.KEY.sync(this.player);
    }

    @Override
    public List<String> getNextRegions() {
        return List.copyOf(nextRegions);
    }

    @Override
    public void addNextRegion(String regionId) {
        if (regionId == null || nextRegions.contains(regionId)) return;
        nextRegions.add(regionId);
        TrainerLevelComponents.KEY.sync(this.player);
    }

    @Override
    public void clearNextRegions() {
        nextRegions.clear();
        TrainerLevelComponents.KEY.sync(this.player);
    }

    @Override
    public Set<String> getBeatenWildTrainers() {
        return Collections.unmodifiableSet(beatenWildTrainers);
    }

    @Override
    public boolean addBeatenWildTrainer(String trainerId) {
        // No sync: clients never see this record.
        return trainerId != null && beatenWildTrainers.add(trainerId);
    }

    @Override
    public Optional<String> getGymDifficulty() {
        return Optional.ofNullable(gymDifficulty);
    }

    @Override
    public void setGymDifficulty(String difficultyId) {
        this.gymDifficulty = difficultyId;
        TrainerLevelComponents.KEY.sync(this.player);
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        if (tag.contains("Level")) {
            this.level = tag.getInt("Level");
            this.loadedFromNbt = true;
        }
        this.startRegion = tag.contains("StartRegion") ? tag.getString("StartRegion") : null;
        this.homeRegion = tag.contains("HomeRegion") ? tag.getString("HomeRegion") : null;
        this.gymDifficulty = tag.contains("GymDifficulty") ? tag.getString("GymDifficulty") : null;
        this.nextRegions.clear();
        for (Tag t : tag.getList("NextRegions", Tag.TAG_STRING)) {
            this.nextRegions.add(t.getAsString());
        }
        this.beatenWildTrainers.clear();
        for (Tag t : tag.getList(BEATEN_WILD_TRAINERS, Tag.TAG_STRING)) {
            this.beatenWildTrainers.add(t.getAsString());
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        tag.putInt("Level", this.level);
        if (this.startRegion != null) {
            tag.putString("StartRegion", this.startRegion);
        }
        if (this.homeRegion != null) {
            tag.putString("HomeRegion", this.homeRegion);
        }
        if (this.gymDifficulty != null) {
            tag.putString("GymDifficulty", this.gymDifficulty);
        }
        if (!this.nextRegions.isEmpty()) {
            ListTag list = new ListTag();
            for (String id : this.nextRegions) list.add(StringTag.valueOf(id));
            tag.put("NextRegions", list);
        }
        if (!this.beatenWildTrainers.isEmpty()) {
            ListTag list = new ListTag();
            for (String id : this.beatenWildTrainers) list.add(StringTag.valueOf(id));
            tag.put(BEATEN_WILD_TRAINERS, list);
        }
    }

    /** Like the default sync packet (the saved data), minus the beaten wild trainers, which can be over a thousand ids. */
    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        CompoundTag tag = new CompoundTag();
        writeToNbt(tag, buf.registryAccess());
        tag.remove(BEATEN_WILD_TRAINERS);
        buf.writeNbt(tag);
    }

    public boolean loadedFromNbt() {
        return loadedFromNbt;
    }
}
