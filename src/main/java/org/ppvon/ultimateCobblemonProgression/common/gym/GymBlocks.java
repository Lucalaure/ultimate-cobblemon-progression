package org.ppvon.ultimateCobblemonProgression.common.gym;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import static org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression.MOD_ID;

public final class GymBlocks {
    public static final ResourceLocation GYM_CHALLENGE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "gym_challenge");

    public static final GymChallengeBlock GYM_CHALLENGE = new GymChallengeBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .strength(-1.0F, 3600000.0F)
            .noLootTable()
            .sound(SoundType.METAL)
            .lightLevel(state -> 7)
            .pushReaction(PushReaction.BLOCK));

    public static final BlockEntityType<GymChallengeBlockEntity> GYM_CHALLENGE_ENTITY =
            BlockEntityType.Builder.of(GymChallengeBlockEntity::new, GYM_CHALLENGE).build(null);

    private GymBlocks() {}

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, GYM_CHALLENGE_ID, GYM_CHALLENGE);
        BlockItem item = Registry.register(BuiltInRegistries.ITEM, GYM_CHALLENGE_ID,
                new BlockItem(GYM_CHALLENGE, new Item.Properties().rarity(Rarity.EPIC)));
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, GYM_CHALLENGE_ID, GYM_CHALLENGE_ENTITY);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(item));
    }
}
