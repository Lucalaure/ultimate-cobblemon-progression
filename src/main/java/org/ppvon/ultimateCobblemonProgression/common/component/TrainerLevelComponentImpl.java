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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TrainerLevelComponentImpl implements TrainerLevelComponent, AutoSyncedComponent {
    private static final String BEATEN_WILD_TRAINERS = "BeatenWildTrainers";
    private static final String REACHED_TIERS = "ReachedTiers";

    private int level = 1;
    private String startRegion = null;
    private String homeRegion = null;
    private String gymDifficulty = null;
    private int levelCapOverride = -1;
    /** Server only, like the beaten wild trainers. */
    private int levelCapFloor = -1;
    private final List<String> nextRegions = new ArrayList<>();
    /** Server only: never sent to clients (see writeSyncPacket), since the Trainer Card is worked out on the server. */
    private final Set<String> beatenWildTrainers = new LinkedHashSet<>();
    /** Server only, like the beaten wild trainers: the highest tier reached per region id. */
    private final Map<String, Integer> reachedTiers = new HashMap<>();
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
    public int getReachedTier(String regionId) {
        return reachedTiers.getOrDefault(regionId, -1);
    }

    @Override
    public void setReachedTier(String regionId, int tier) {
        // No sync: clients never need it.
        if (tier >= 0) reachedTiers.put(regionId, tier);
        else reachedTiers.remove(regionId);
    }

    @Override
    public int getLevelCapOverride() {
        return levelCapOverride;
    }

    @Override
    public void setLevelCapOverride(int cap) {
        int value = cap > 0 ? cap : -1;
        if (value == levelCapOverride) return;
        levelCapOverride = value;
        TrainerLevelComponents.KEY.sync(this.player);
    }

    @Override
    public int getLevelCapFloor() {
        return levelCapFloor;
    }

    @Override
    public void setLevelCapFloor(int cap) {
        // No sync: clients never need it.
        levelCapFloor = cap;
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
        this.levelCapOverride = tag.contains("LevelCapOverride") ? tag.getInt("LevelCapOverride") : -1;
        this.levelCapFloor = tag.contains("LevelCapFloor") ? tag.getInt("LevelCapFloor") : -1;
        this.nextRegions.clear();
        for (Tag t : tag.getList("NextRegions", Tag.TAG_STRING)) {
            this.nextRegions.add(t.getAsString());
        }
        this.beatenWildTrainers.clear();
        for (Tag t : tag.getList(BEATEN_WILD_TRAINERS, Tag.TAG_STRING)) {
            this.beatenWildTrainers.add(t.getAsString());
        }
        this.reachedTiers.clear();
        CompoundTag reached = tag.getCompound(REACHED_TIERS);
        for (String regionId : reached.getAllKeys()) this.reachedTiers.put(regionId, reached.getInt(regionId));
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
        if (this.levelCapOverride > 0) {
            tag.putInt("LevelCapOverride", this.levelCapOverride);
        }
        if (this.levelCapFloor >= 0) {
            tag.putInt("LevelCapFloor", this.levelCapFloor);
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
        if (!this.reachedTiers.isEmpty()) {
            CompoundTag reached = new CompoundTag();
            this.reachedTiers.forEach(reached::putInt);
            tag.put(REACHED_TIERS, reached);
        }
    }

    /** Like the default sync packet (the saved data), minus the beaten wild trainers, which can be over a thousand ids. */
    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        CompoundTag tag = new CompoundTag();
        writeToNbt(tag, buf.registryAccess());
        tag.remove(BEATEN_WILD_TRAINERS);
        tag.remove(REACHED_TIERS);
        tag.remove("LevelCapFloor");
        buf.writeNbt(tag);
    }

    public boolean loadedFromNbt() {
        return loadedFromNbt;
    }
}
