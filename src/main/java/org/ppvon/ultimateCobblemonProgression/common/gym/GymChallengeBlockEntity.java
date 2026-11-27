package org.ppvon.ultimateCobblemonProgression.common.gym;

import com.cobblemon.mod.common.battles.BattleRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Remembers the trainer a Gym Challenge block summoned and who it's for. Only one leader stands in a gym
 * at a time; it's sent away once its challenger beats it, leaves, logs out or stops battling for a while.
 */
public class GymChallengeBlockEntity extends BlockEntity {
    /** Put on every trainer a gym summons, so leftovers can be cleaned up. */
    public static final String LEADER_TAG = "ucp_gym_leader";

    /** Put on a leader a gym summons for this player (not for rematches), naming who it's for. */
    public static String challengerTag(ServerPlayer player) {
        return "ucp_gym_for_" + player.getUUID();
    }

    /** How far the challenger may wander from the gym before the leader leaves. */
    private static final double MAX_CHALLENGER_DISTANCE = 32;
    /** Ticks without a battle before an unchallenged leader leaves (3 minutes). */
    private static final long IDLE_TIMEOUT = 20 * 60 * 3;
    /** How far in front of the block a leader stands. */
    private static final int LEADER_DISTANCE = 2;
    /**
     * How far to the block's right (seen from the entrance) a leader stands: in the village gyms that's the
     * arena's side sea lantern, facing the PC and healing machines across the battlefield.
     */
    private static final int LEADER_SIDE_OFFSET = 5;

    private UUID leader;
    private UUID challenger;
    private String challengerName = "";
    private String trainerId = "";
    private String leaderName = "";
    private int defeatsAtSummon;
    private long lastActive;

    public GymChallengeBlockEntity(BlockPos pos, BlockState state) {
        super(GymBlocks.GYM_CHALLENGE_ENTITY, pos, state);
    }

    /**
     * Handles a player using the block: summons their next gym leader. With two leagues in progress and a leader
     * ready in each, it opens a menu to pick one instead (see {@link GymChoice}).
     */
    public void use(ServerPlayer player) {
        ServerLevel level = (ServerLevel) this.level;
        if (level == null) return;
        if (isTaken(level, player)) return;

        GymLeaderPicker.Result result = GymLeaderPicker.pickAll(player);
        if (!result.found()) {
            tell(player, result.problem().copy().withStyle(ChatFormatting.GRAY));
            if (!GymRematch.rematchable(player).isEmpty()) {
                tell(player, Component.literal("Sneak and use the block with an empty hand to rematch a leader you've beaten.")
                        .withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        if (result.ready().size() > 1) {
            ServerPlayNetworking.send(player, new GymChoice.Open(worldPosition, GymChoice.options(player, result.ready())));
            return;
        }
        // One leader ready: summon it, and say why the other league's leader isn't.
        for (Component why : result.waiting()) tell(player, why.copy().withStyle(ChatFormatting.GRAY));
        GymLeaderPicker.Pick pick = result.ready().get(0);
        summon(level, player, pick.trainerId(), pick.region().title, false);
    }

    /** Summons the leader the player picked from the menu, if it's still one of the leaders ready for them. */
    public void challenge(ServerPlayer player, String pickedId) {
        ServerLevel level = (ServerLevel) this.level;
        if (level == null) return;
        if (isTaken(level, player)) return;
        for (GymLeaderPicker.Pick pick : GymLeaderPicker.pickAll(player).ready()) {
            if (pick.trainerId().equals(pickedId)) {
                summon(level, player, pick.trainerId(), pick.region().title, false);
                return;
            }
        }
        tell(player, Component.literal("That leader isn't ready for you any more. Use the block again.").withStyle(ChatFormatting.RED));
    }

    /** Handles a player sneaking and using the block: opens the menu of leaders they can rematch. */
    public void openRematch(ServerPlayer player) {
        ServerLevel level = (ServerLevel) this.level;
        if (level == null) return;
        if (isTaken(level, player)) return;
        if (!RctBridge.isLoaded()) {
            tell(player, Component.literal("Gyms need Radical Cobblemon Trainers to be installed.").withStyle(ChatFormatting.GRAY));
            return;
        }
        var entries = GymRematch.rematchable(player);
        if (entries.isEmpty()) {
            tell(player, GymRematch.nothingToRematch());
            return;
        }
        ServerPlayNetworking.send(player, new RematchPayloads.Open(worldPosition, entries));
    }

    /** Summons a leader the player already beat, picked from the rematch menu. */
    public void rematch(ServerPlayer player, String pickedId) {
        ServerLevel level = (ServerLevel) this.level;
        if (level == null) return;
        if (isTaken(level, player)) return;
        if (!GymRematch.canRematch(player, pickedId)) {
            tell(player, Component.literal("You can only rematch leaders you've already beaten.").withStyle(ChatFormatting.RED));
            return;
        }
        var region = GymRematch.regionOf(pickedId);
        summon(level, player, pickedId, region != null ? region.title : "Rematch", true);
    }

    /** Whether a leader is already standing in this gym; tells the player so. */
    private boolean isTaken(ServerLevel level, ServerPlayer player) {
        checkLeader(level);
        if (leader == null) return false;
        if (player.getUUID().equals(challenger)) {
            tell(player, Component.literal(leaderName + " is waiting for you.").withStyle(ChatFormatting.YELLOW));
        } else {
            tell(player, Component.literal(challengerName + " is challenging " + leaderName
                    + ". Wait for them to finish.").withStyle(ChatFormatting.RED));
        }
        return true;
    }

    private void summon(ServerLevel level, ServerPlayer player, String id, String regionTitle, boolean rematch) {
        if (!rematch) {
            // Two leagues at once: the leader won't battle while a party Pokémon is above the level cap, so say so now.
            var over = LeagueLevelCap.aboveCap(player);
            if (over != null) {
                tell(player, Component.literal(over.getDisplayName(false).getString() + " is level " + over.getLevel()
                        + ", above your level cap of " + TrainerLevelComponents.KEY.get(player).getLevelCapOverride()
                        + ". Gym leaders won't battle you until every Pokémon in your party is at or below it.")
                        .withStyle(ChatFormatting.RED));
                return;
            }
        }
        clearLeftovers(level);
        Entity trainer = rematch
                ? RctBridge.summon(level, leaderSpot(level), id, GymRematch.REMATCH_TAG)
                : RctBridge.summon(level, leaderSpot(level), id, challengerTag(player));
        if (trainer == null) {
            tell(player, Component.literal("The gym leader couldn't be summoned. Check the server log.").withStyle(ChatFormatting.RED));
            return;
        }
        holdInPlace(trainer);

        leader = trainer.getUUID();
        challenger = player.getUUID();
        challengerName = player.getGameProfile().getName();
        trainerId = id;
        leaderName = trainer.getDisplayName().getString();
        defeatsAtSummon = Math.max(0, RctBridge.defeats(player, trainerId));
        lastActive = level.getGameTime();
        setChanged();

        var accepted = Component.literal("[" + regionTitle + "] ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(leaderName + (rematch ? " accepts your rematch!" : " accepts your challenge!"))
                        .withStyle(ChatFormatting.GOLD));
        if (GymDifficulty.isHard(trainerId)) accepted.append(Component.literal(" (Hard)").withStyle(ChatFormatting.RED));
        tell(player, accepted);
        if (rematch) {
            tell(player, Component.literal("A rematch doesn't change your progress, and your level cap doesn't apply.")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (GymTeamLimit.isOn()) {
            Component hint = GymTeamLimit.hint(leaderName, RctBridge.teamSize(level.getServer(), trainerId));
            if (hint != null) tell(player, hint);
        }
        if (!rematch) {
            Component legality = GymLegality.hint(player, leaderName);
            if (legality != null) tell(player, legality);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GymChallengeBlockEntity gym) {
        if (gym.leader == null || level.getGameTime() % 20 != 0) return;
        gym.checkLeader((ServerLevel) level);
    }

    /** Sends the leader away if its challenge is over. */
    private void checkLeader(ServerLevel level) {
        if (leader == null) return;

        Entity trainer = level.getEntity(leader);
        if (trainer == null || !trainer.isAlive()) {
            clear();
            return;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(challenger);
        if (player == null || player.level() != level
                || player.distanceToSqr(worldPosition.getCenter()) > MAX_CHALLENGER_DISTANCE * MAX_CHALLENGER_DISTANCE) {
            dismiss(trainer, player, null);
            return;
        }

        if (BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(player) != null) {
            lastActive = level.getGameTime();
            return;
        }

        if (RctBridge.defeats(player, trainerId) > defeatsAtSummon) {
            // RCT counts the win after CJP's advancement fires, so the two-league level cap catches up here.
            LeagueLevelCap.update(player);
            dismiss(trainer, player, Component.literal(leaderName + " heads back to the league. The gym is free again.")
                    .withStyle(ChatFormatting.GREEN));
            Component next = GymLeaderPicker.afterWin(player, trainerId);
            if (next != null) tell(player, next.copy().withStyle(ChatFormatting.YELLOW));
        } else if (level.getGameTime() - lastActive > IDLE_TIMEOUT) {
            dismiss(trainer, player, Component.literal(leaderName + " got tired of waiting and left the gym.")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private void dismiss(Entity trainer, ServerPlayer player, Component message) {
        trainer.discard();
        if (player != null && message != null) tell(player, message);
        clear();
    }

    /** Called when the block is broken. */
    void removeLeader() {
        if (leader != null && level instanceof ServerLevel server) {
            Entity trainer = server.getEntity(leader);
            if (trainer != null) trainer.discard();
        }
        clear();
    }

    private void clear() {
        leader = null;
        challenger = null;
        setChanged();
    }

    /** Removes trainers this gym summoned earlier but lost track of, e.g. across a chunk reload. */
    private void clearLeftovers(ServerLevel level) {
        level.getEntities((Entity) null, new AABB(worldPosition).inflate(8),
                e -> e.getTags().contains(LEADER_TAG)).forEach(Entity::discard);
    }

    /**
     * Keeps the leader standing where it was summoned, facing the PC and healing machines: permanent, hidden
     * Slowness at the highest level (as Radical Gyms & Structures did), which stops walking but not battling.
     */
    private void holdInPlace(Entity trainer) {
        if (!(trainer instanceof LivingEntity living)) return;
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MobEffectInstance.INFINITE_DURATION,
                255, false, false, false));
        // The block faces the gym's entrance; the PC and healing machines are a quarter turn to its left.
        float yaw = getBlockState().getValue(GymChallengeBlock.FACING).getCounterClockWise().toYRot();
        living.setYRot(yaw);
        living.setYHeadRot(yaw);
        living.setYBodyRot(yaw);
    }

    /**
     * Where the leader stands: the arena's side spot (in the village gyms, the sea lantern 2 ahead and 5 to the
     * right of the block). Gyms built by hand may not have room there, so fall back to straight in front of the
     * block, then on top of it.
     */
    private BlockPos leaderSpot(ServerLevel level) {
        Direction facing = getBlockState().getValue(GymChallengeBlock.FACING);
        BlockPos ahead = worldPosition.relative(facing, LEADER_DISTANCE);
        BlockPos side = ahead.relative(facing.getClockWise(), LEADER_SIDE_OFFSET);
        if (canStand(level, side)) return side;
        for (int d = LEADER_DISTANCE; d >= 1; d--) {
            BlockPos spot = worldPosition.relative(facing, d);
            if (canStand(level, spot)) return spot;
        }
        return worldPosition.above();
    }

    /** Room for a trainer: nothing solid at feet or head height (carpets and slabs are fine), and a floor below. */
    private static boolean canStand(ServerLevel level, BlockPos feet) {
        return !level.getBlockState(feet).isCollisionShapeFullBlock(level, feet)
                && !level.getBlockState(feet.above()).isCollisionShapeFullBlock(level, feet.above())
                && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty();
    }

    private static void tell(ServerPlayer player, Component message) {
        player.displayClientMessage(message, false);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (leader == null) return;
        tag.putUUID("Leader", leader);
        tag.putUUID("Challenger", challenger);
        tag.putString("ChallengerName", challengerName);
        tag.putString("TrainerId", trainerId);
        tag.putString("LeaderName", leaderName);
        tag.putInt("DefeatsAtSummon", defeatsAtSummon);
        tag.putLong("LastActive", lastActive);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!tag.hasUUID("Leader") || !tag.hasUUID("Challenger")) {
            leader = null;
            challenger = null;
            return;
        }
        leader = tag.getUUID("Leader");
        challenger = tag.getUUID("Challenger");
        challengerName = tag.getString("ChallengerName");
        trainerId = tag.getString("TrainerId");
        leaderName = tag.getString("LeaderName");
        defeatsAtSummon = tag.getInt("DefeatsAtSummon");
        lastActive = tag.getLong("LastActive");
    }
}
