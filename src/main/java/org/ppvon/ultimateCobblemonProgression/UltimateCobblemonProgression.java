package org.ppvon.ultimateCobblemonProgression;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
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

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.player;
            var comp = TrainerLevelComponents.KEY.get(player);

            if(comp instanceof TrainerLevelComponentImpl impl && !impl.loadedFromNbt()) {
                impl.setLevel(1);
            }
            TrainerLevelComponents.KEY.sync(player);
            RegionProgressTracker.refreshSilently(player);
            if (RegionRegistry.isActive()) RegionCommands.promptStartChoice(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RegionProgressTracker.forget(handler.player));

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            TrainerLevelComponents.KEY.sync(newPlayer);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            dispatcher.register(Commands.literal("trainerlevel")
                    .then(RegionCommands.regionNode())
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
}