package io.silvicky.elina.common;

import com.mojang.datafixers.util.Pair;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureKeys;

import java.util.Objects;

public class DistanceCalculator
{
    private final ServerWorld sourceWorld;
    private final Vec3d sourcePos;
    private final Util.DimensionType sourceType;
    private final double scale;
    private final double strongholdDistance;
    private final Vec3d platform=ServerWorld.END_SPAWN_POS.toCenterPos().withAxis(Direction.Axis.Y,0);
    private final Vec3d returnGate=Vec3d.ZERO;
    private final Vec3d spawn;
    public DistanceCalculator(ServerWorld sourceWorld,Vec3d sourcePos)
    {
        this.sourceWorld = sourceWorld;
        Util.DimensionType type = Util.DimensionType.getDimensionType(sourceWorld);
        if (type == Util.DimensionType.SINGLET || type == Util.DimensionType.END_TYPE)
        {
            this.sourcePos = sourcePos;
            this.sourceType = type;
            scale = 1;
            strongholdDistance = 0;
            spawn=Vec3d.ZERO;
            return;
        }
        this.sourceType = Util.DimensionType.OVERWORLD_TYPE;
        ServerWorld overworld = Objects.requireNonNull(sourceWorld.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, Util.getDimensionId(sourceWorld, Util.DimensionType.OVERWORLD_TYPE))));
        ServerWorld nether = Objects.requireNonNull(sourceWorld.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, Util.getDimensionId(sourceWorld, Util.DimensionType.NETHER_TYPE))));
        scale = DimensionType.getCoordinateScaleFactor(
                overworld.getDimension(),
                nether.getDimension()
        );
        this.sourcePos = (type== Util.DimensionType.OVERWORLD_TYPE?sourcePos:sourcePos.multiply(1/scale)).withAxis(Direction.Axis.Y,0);
        double d=Double.MAX_VALUE;
        RegistryEntryList<Structure> stronghold=RegistryEntryList.of(overworld.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE).getOrThrow(StructureKeys.STRONGHOLD));
        Pair<BlockPos, RegistryEntry<Structure>> pair1 =
                overworld.getChunkManager().getChunkGenerator()
                        .locateStructure(overworld, stronghold, BlockPos.ofFloored(sourcePos), 100, false);
        if(pair1!=null)d=Math.min(d,sourcePos.distanceTo(pair1.getFirst().toBottomCenterPos().withAxis(Direction.Axis.Y,0))*scale);
        Pair<BlockPos, RegistryEntry<Structure>> pair2 =
                nether.getChunkManager().getChunkGenerator()
                        .locateStructure(nether, stronghold, BlockPos.ofFloored(sourcePos.multiply(scale)), 100, false);
        if(pair2!=null)d=Math.min(d,sourcePos.distanceTo(pair2.getFirst().toBottomCenterPos().withAxis(Direction.Axis.Y,0)));
        strongholdDistance=d;
        WorldProperties.SpawnPoint spawnPoint=overworld.getSpawnPoint();
        if(Util.DimensionType.getDimensionType(spawnPoint.getDimension().getValue().toString())== Util.DimensionType.OVERWORLD_TYPE)
        {
            spawn=spawnPoint.getPos().toCenterPos().withAxis(Direction.Axis.Y,0);
        }
        else
        {
            spawn=spawnPoint.getPos().toCenterPos().withAxis(Direction.Axis.Y,0).multiply(1/scale);
        }
    }
    public double calculateDistance(ServerWorld targetWorld, Vec3d targetPos)
    {
        if(!Util.getDimensionId(sourceWorld, Util.DimensionType.OVERWORLD_TYPE).equals(Util.getDimensionId(targetWorld, Util.DimensionType.OVERWORLD_TYPE)))return Double.MAX_VALUE;
        targetPos=targetPos.withAxis(Direction.Axis.Y,0);
        Util.DimensionType targetType= Util.DimensionType.getDimensionType(targetWorld);
        if(sourceType== Util.DimensionType.SINGLET
                ||(sourceType== Util.DimensionType.END_TYPE&&targetType== Util.DimensionType.END_TYPE))
        {
            return sourcePos.distanceTo(targetPos);
        }
        if(targetType== Util.DimensionType.NETHER_TYPE)
        {
            targetType= Util.DimensionType.OVERWORLD_TYPE;
            targetPos=targetPos.multiply(1/scale);
        }
        if(sourceType== Util.DimensionType.OVERWORLD_TYPE&&targetType== Util.DimensionType.OVERWORLD_TYPE)
        {
            return sourcePos.distanceTo(targetPos)*scale;
        }
        if(sourceType== Util.DimensionType.OVERWORLD_TYPE)
        {
            return strongholdDistance*scale+platform.distanceTo(targetPos);
        }
        else
        {
            return sourcePos.distanceTo(returnGate)+spawn.distanceTo(targetPos)*scale;
        }
    }
}
