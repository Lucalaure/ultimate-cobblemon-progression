package org.ppvon.ultimateCobblemonProgression.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.phys.AABB;
import org.ppvon.ultimateCobblemonProgression.common.gym.VillageGyms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Village streets follow the terrain. When the space for them is checked, a street measured on lower ground
 * can sit entirely below a gym's box and pass, then get laid on the gym's roof or through its floor.
 * So when a village's centre is a gym, reserve its whole column, bottom of the world to top.
 */
@Mixin(JigsawPlacement.class)
public abstract class JigsawPlacementMixin {
    private static final double COLUMN_BOTTOM = -4096, COLUMN_TOP = 4096;

    /** The start piece's box, subtracted from the space the other pieces may use (in addPieces' generation lambda). */
    @ModifyExpressionValue(
            method = "method_39824",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;of(Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)Lnet/minecraft/world/phys/AABB;",
                    ordinal = 0))
    private static AABB ucp$reserveGymColumn(AABB startBox, @Local(argsOnly = true) PoolElementStructurePiece start) {
        if (!VillageGyms.isGym(start.getElement())) return startBox;
        return new AABB(startBox.minX, COLUMN_BOTTOM, startBox.minZ, startBox.maxX, COLUMN_TOP, startBox.maxZ);
    }
}
