package org.ppvon.ultimateCobblemonProgression.mixin;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Lets UCP add its gym to the vanilla village centre pools. */
@Mixin(StructureTemplatePool.class)
public interface StructureTemplatePoolAccessor {
    @Accessor("rawTemplates")
    List<Pair<StructurePoolElement, Integer>> ucp$getRawTemplates();

    @Mutable
    @Accessor("rawTemplates")
    void ucp$setRawTemplates(List<Pair<StructurePoolElement, Integer>> rawTemplates);

    @Accessor("templates")
    ObjectArrayList<StructurePoolElement> ucp$getTemplates();
}
