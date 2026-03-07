package io.silvicky.elina.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static io.silvicky.elina.command.Locate.getBlockPosSet;
import static io.silvicky.elina.command.Locate.hasShip;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin
{
    @ModifyVariable(method = "getStructureGeneratingAt(Ljava/util/Set;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/StructureManager;ZLnet/minecraft/world/level/levelgen/structure/placement/StructurePlacement;Lnet/minecraft/world/level/ChunkPos;)Lcom/mojang/datafixers/util/Pair;", at = @At("STORE"))
    private static StructureStart inject1(StructureStart structureStart, @Local(argsOnly = true) StructurePlacement placement, @Local(argsOnly = true) LevelReader world, @Local Holder<Structure> registryEntry) {
        if(getBlockPosSet(registryEntry.unwrapKey().orElseThrow(), (ServerLevel) world).contains(placement.getLocatePos(structureStart.getChunkPos()).atY(0))) return null;
        return structureStart;
    }
    @ModifyVariable(method = "getStructureGeneratingAt(Ljava/util/Set;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/StructureManager;ZLnet/minecraft/world/level/levelgen/structure/placement/StructurePlacement;Lnet/minecraft/world/level/ChunkPos;)Lcom/mojang/datafixers/util/Pair;", at = @At("STORE"))
    private static StructureCheckResult inject2(StructureCheckResult structurePresence, @Local(argsOnly = true) StructurePlacement placement, @Local(argsOnly = true) LevelReader world, @Local Holder<Structure> registryEntry, @Local(argsOnly = true) ChunkPos pos, @Local(argsOnly = true)boolean skipReferencedStructures) {
        if(structurePresence!= StructureCheckResult.START_NOT_PRESENT&&registryEntry.is(BuiltinStructures.END_CITY)&&!hasShip((ServerLevel) world,pos))return StructureCheckResult.START_NOT_PRESENT;
        if(skipReferencedStructures||structurePresence!= StructureCheckResult.START_PRESENT)return structurePresence;
        if(getBlockPosSet(registryEntry.unwrapKey().orElseThrow(), (ServerLevel) world).contains(placement.getLocatePos(pos).atY(0))) return StructureCheckResult.START_NOT_PRESENT;
        return structurePresence;
    }
}
