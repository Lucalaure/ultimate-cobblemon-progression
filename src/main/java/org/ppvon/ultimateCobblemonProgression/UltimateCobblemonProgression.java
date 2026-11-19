package org.ppvon.ultimateCobblemonProgression;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponentImpl;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;

import org.ppvon.ultimateCobblemonProgression.common.progression.ProgressionManager;
import org.ppvon.ultimateCobblemonProgression.common.progression.dex.DexProgressionApi;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierRegistry;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierSpeciesApplier;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;
import org.ppvon.ultimateCobblemonProgression.common.influence.TrainerLevelInfluence;
import org.ppvon.ultimateCobblemonProgression.common.influence.TrainerLevelInfluenceRegistrar;
import org.ppvon.ultimateCobblemonProgression.common.levelcap.CandyEntityBlock;
import org.ppvon.ultimateCobblemonProgression.common.levelcap.CandyRefund;
import org.ppvon.ultimateCobblemonProgression.common.levelcap.ExpCapListener;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDataLoader;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionCommands;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionDataLoader;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgressTracker;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionSpecies;
import org.ppvon.ultimateCobblemonProgression.common.gym.GymBlocks;
import org.ppvon.ultimateCobblemonProgression.common.gym.GymTeamLimit;
import org.ppvon.ultimateCobblemonProgression.common.gym.VillageGyms;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCard;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UltimateCobblemonProgression implements ModInitializer {
    public static final String MOD_ID = "ultimate-cobblemon-progression";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        ConfigLoader.init();
        GymBlocks.register();
        VillageGyms.register();
        GymTeamLimit.register();
        TrainerCard.register();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.player;
            var comp = TrainerLevelComponents.KEY.get(player);

            if(comp instanceof TrainerLevelComponentImpl impl && !impl.loadedFromNbt()) {
                impl.setLevel(1);
            }
            TrainerLevelComponents.KEY.sync(player);
            RegionProgressTracker.refreshSilently(player);
            if (RegionRegistry.isActive()) RegionCommands.promptStartChoice(player);
            TrainerCard.onJoin(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RegionProgressTracker.forget(handler.player));

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            TrainerLevelComponents.KEY.sync(newPlayer);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            dispatcher.register(Commands.literal("trainerlevel")
                    .then(RegionCommands.regionNode())
                    .then(TrainerCard.commandNode())
                    .then(Commands.literal("get")
                            .executes(ctx -> {
                                ServerPlayer p = ctx.getSource().getPlayerOrException();

                                if (RegionRegistry.isActive()) {
                                    ctx.getSource().sendSuccess(() -> RegionCommands.describe(p), false);
                                    return 1;
                                }

                                int v = TrainerLevelComponents.KEY.get(p).getLevel();
                                var nextLevel = TierRegistry.get(v + 1);

                                if(nextLevel.isEmpty()) {
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Trainer Level: " + v + " (MAX)"),
                                            false
                                    );
                                    return 1;
                                }

                                var requirements = nextLevel.get().requirements.dex;
                                int seenRequirement = requirements.seen;
                                int caughtRequirement = requirements.caught;

                                DexProgressionApi.DexCounts counts = DexProgressionApi.get(p);
                                int seenCount = counts.seen();
                                int caughtCount = counts.caught();

                                String message = """
                                        Trainer Level: %d
                                        
                                        Next Level: %d (Level cap %d)
                                        Pokédex Progress:
                                        • Seen:   %d / %d
                                        • Caught: %d / %d""".formatted(
                                        v,
                                        v + 1,
                                        nextLevel.get().levelCap,
                                        seenCount, seenRequirement,
                                        caughtCount, caughtRequirement
                                );

                                ctx.getSource().sendSuccess(() -> Component.literal(message), false);
                                return 1;
                            })
                    )
                    .then(Commands.literal("set")
                            .requires(src -> src.hasPermission(2))
                            .then(Commands.argument("player", EntityArgument.player())
                                    .then(Commands.argument("value", IntegerArgumentType.integer(0, 999))
                                            .executes(ctx -> {
                                                if (RegionRegistry.isActive()) return RegionCommands.unavailable(ctx.getSource());
                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                int v = IntegerArgumentType.getInteger(ctx, "value");

                                                TrainerLevelComponents.KEY.get(target).setLevel(v);
                                                int newLevel = TrainerLevelComponents.KEY.get(target).getLevel();

                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal("Set " + target.getGameProfile().getName() + "'s Trainer Level to " + newLevel),
                                                        true
                                                );

                                                target.sendSystemMessage(Component.literal(
                                                        "Your Trainer Level was set to " + newLevel
                                                ));

                                                return 1;
                                            })
                                    )
                            )
                    )
                    .then(Commands.literal("promote")
                            .requires(src -> src.hasPermission(2))
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(ctx -> {
                                        CommandSourceStack src = ctx.getSource();
                                        if (RegionRegistry.isActive()) return RegionCommands.unavailable(src);
                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

                                        int current = TrainerLevelComponents.KEY.get(target).getLevel();
                                        int next = current + 1;

                                        if (TierRegistry.get(next).isEmpty()) {
                                            src.sendSuccess(
                                                    () -> Component.literal(
                                                            target.getGameProfile().getName() +
                                                                    " is already at the maximum Trainer Level (" + current + ")"
                                                    ),
                                                    false
                                            );
                                            return 1;
                                        }

                                        TrainerLevelComponents.KEY.get(target).setLevel(next);

                                        src.sendSuccess(
                                                () -> Component.literal(
                                                        "Promoted " + target.getGameProfile().getName() +
                                                                " to Trainer Level " + next
                                                ),
                                                true
                                        );

                                        target.sendSystemMessage(ProgressionManager.buildChatMessage(target, next));

                                        return 1;
                                    })
                            )
                    )
            );
        });
        LOG.info("Trainer level commands registered");
        TrainerLevelInfluenceRegistrar.registerOnce();
        TrainerLevelInfluence.registerSpawnGuard();
        LOG.info("Trainer level influence registered");
        TierDataLoader.register();
        RegionDataLoader.register();
        registerKantoLeague();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TierSpeciesApplier.applyFromRegistry();
            RegionSpecies.applyFromRegistry();
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, rm, success) -> {
            if (!success) return;
            TierSpeciesApplier.applyFromRegistry();
            RegionSpecies.applyFromRegistry();
            RegionProgressTracker.refreshAllSilently(server);
        });

        CandyEntityBlock.register();
        CandyRefund.register();
        ExpCapListener.register();
        ProgressionManager.register();
        LOG.info("Done Loading");
        /*
        if(FabricLoader.getInstance().isModLoaded("rad-gyms")) {
            RadGymsCompat.register();
        }
         */
    }

    /**
     * The built-in Kanto league: every region's species tiers, plus Kanto's gym leaders, Elite Four and Champion
     * for Radical Cobblemon Trainers. Only offered when RCT is installed, since Kanto's tiers unlock by beating its
     * trainers; it's on by default and can be turned off like any datapack.
     */
    private static void registerKantoLeague() {
        if (!FabricLoader.getInstance().isModLoaded("rctmod")) {
            LOG.info("Radical Cobblemon Trainers isn't installed, so the built-in Kanto league is off");
            return;
        }
        FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(mod -> ResourceManagerHelper.registerBuiltinResourcePack(
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "kanto_league"), mod,
                Component.literal("UCP Kanto League"), ResourcePackActivationType.DEFAULT_ENABLED));
    }
}