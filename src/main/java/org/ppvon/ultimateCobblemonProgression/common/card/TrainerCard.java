package org.ppvon.ultimateCobblemonProgression.common.card;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionCommands;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import static org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression.MOD_ID;

/**
 * The Trainer Card: every player gets one on their first join. Using it opens a book with their progress,
 * level cap, next challenge and the wild Pokémon they can meet.
 */
public final class TrainerCard {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "trainer_card");
    /** Marks players who already got their card, so it's only handed out once. */
    private static final String GIVEN_TAG = "ucp_trainer_card_given";

    public static final Item ITEM = new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)) {
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (player instanceof ServerPlayer sp) open(sp);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    };

    private TrainerCard() {}

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, ID, ITEM);
        PayloadTypeRegistry.playS2C().register(TrainerCardPayload.TYPE, TrainerCardPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PickRegionPayload.TYPE, PickRegionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PickRegionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            RegionCommands.Choice result = payload.home()
                    ? RegionCommands.chooseHomeRegion(player, payload.regionId())
                    : RegionCommands.chooseStartRegion(player, payload.regionId());
            player.displayClientMessage(result.ok() ? result.message()
                    : result.message().copy().withStyle(ChatFormatting.RED), false);
            open(player); // show the card again with the new region open
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(ITEM));
    }

    /** Shows the player their card. */
    public static void open(ServerPlayer player) {
        try {
            ServerPlayNetworking.send(player, new TrainerCardPayload(TrainerCardDataBuilder.build(player)));
        } catch (RuntimeException e) {
            UltimateCobblemonProgression.LOG.error("Couldn't build {}'s Trainer Card", player.getGameProfile().getName(), e);
            player.displayClientMessage(Component.literal("Your Trainer Card couldn't be read. Check the server log.")
                    .withStyle(ChatFormatting.RED), false);
        }
    }

    /** Gives the card on a player's first join. */
    public static void onJoin(ServerPlayer player) {
        if (!ConfigLoader.GIVE_TRAINER_CARD.get() || player.getTags().contains(GIVEN_TAG)) return;
        player.addTag(GIVEN_TAG);
        give(player);
        player.displayClientMessage(Component.literal("You received a Trainer Card. Use it to check your progress.")
                .withStyle(ChatFormatting.GREEN), false);
    }

    /** /trainerlevel card: a replacement card, if the player doesn't have one. */
    public static LiteralArgumentBuilder<CommandSourceStack> commandNode() {
        return Commands.literal("card").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            if (player.getInventory().contains(new ItemStack(ITEM))) {
                ctx.getSource().sendFailure(Component.literal("You already have a Trainer Card."));
                return 0;
            }
            give(player);
            ctx.getSource().sendSuccess(() -> Component.literal("Here's a new Trainer Card."), false);
            return 1;
        });
    }

    private static void give(ServerPlayer player) {
        ItemStack card = new ItemStack(ITEM);
        if (!player.getInventory().add(card)) player.drop(card, false);
    }
}
