package io.silvicky.elina.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructurePresence;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static io.silvicky.elina.command.Locate.getBlockPosSet;
import static io.silvicky.elina.command.Locate.hasShip;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin
{
    @ModifyVariable(method = "locateStructure(Ljava/util/Set;Lnet/minecraft/world/WorldView;Lnet/minecraft/world/gen/StructureAccessor;ZLnet/minecraft/world/gen/chunk/placement/StructurePlacement;Lnet/minecraft/util/math/ChunkPos;)Lcom/mojang/datafixers/util/Pair;", at = @At("STORE"))
    private static StructureStart inject1(StructureStart structureStart, @Local(argsOnly = true) StructurePlacement placement, @Local(argsOnly = true) WorldView world, @Local RegistryEntry<Structure> registryEntry) {
        if(getBlockPosSet(registryEntry.getKey().orElseThrow(), (ServerWorld) world).contains(placement.getLocatePos(structureStart.getPos()).withY(0))) return null;
        return structureStart;
    }
    @ModifyVariable(method = "locateStructure(Ljava/util/Set;Lnet/minecraft/world/WorldView;Lnet/minecraft/world/gen/StructureAccessor;ZLnet/minecraft/world/gen/chunk/placement/StructurePlacement;Lnet/minecraft/util/math/ChunkPos;)Lcom/mojang/datafixers/util/Pair;", at = @At("STORE"))
    private static StructurePresence inject2(StructurePresence structurePresence, @Local(argsOnly = true) StructurePlacement placement, @Local(argsOnly = true) WorldView world, @Local RegistryEntry<Structure> registryEntry, @Local(argsOnly = true)ChunkPos pos, @Local(argsOnly = true)boolean skipReferencedStructures) {
        if(structurePresence!=StructurePresence.START_NOT_PRESENT&&registryEntry.matchesKey(StructureKeys.END_CITY)&&!hasShip((ServerWorld) world,pos))return StructurePresence.START_NOT_PRESENT;
        if(skipReferencedStructures||structurePresence!=StructurePresence.START_PRESENT)return structurePresence;
        if(getBlockPosSet(registryEntry.getKey().orElseThrow(), (ServerWorld) world).contains(placement.getLocatePos(pos).withY(0))) return StructurePresence.START_NOT_PRESENT;
        return structurePresence;
    }
}
