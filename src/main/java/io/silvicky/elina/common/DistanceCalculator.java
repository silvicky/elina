package io.silvicky.elina.common;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

import java.util.Objects;
import java.util.Optional;

/**
 * An inter-dimension distance calculator
 * <p>
 * It runs with such assumptions:
 * <ol>
 *  <li>Nether has a scale larger than overworld, and is always accessible</li>
 *  <li>In the same dimension, distance between two points is the 2D Euclidean distance</li>
 *  <li>From overworld/nether to end, the nearest stronghold is the entry of end, and player is spawned at the end spawn</li>
 *  <li>From end to overworld/nether, (0,0) is the exit of end, and player is spawned at the world spawn</li>
 *  <li>If CombinedWorld is installed, only distances of points in the same world(not group/namespace) are considered.</li>
 * </ol>
 */
public class DistanceCalculator
{
    private final ServerLevel sourceWorld;
    private final Vec3 sourcePos;
    private final Util.DimensionType sourceType;
    private final double scale;
    private final double strongholdDistance;
    private final Vec3 platform= ServerLevel.END_SPAWN_POINT.getCenter().with(Direction.Axis.Y,0);
    private final Vec3 returnGate= Vec3.ZERO;
    private final Vec3 spawn;
    public DistanceCalculator(ServerLevel sourceWorld, Vec3 sourcePos)
    {
        this.sourceWorld = sourceWorld;
        Util.DimensionType type = Util.DimensionType.getDimensionType(sourceWorld);
        if (type == Util.DimensionType.SINGLET || type == Util.DimensionType.END_TYPE)
        {
            this.sourcePos = sourcePos;
            this.sourceType = type;
            scale = 1;
            strongholdDistance = 0;
            spawn= Vec3.ZERO;
            return;
        }
        this.sourceType = Util.DimensionType.OVERWORLD_TYPE;
        ServerLevel overworld = Objects.requireNonNull(sourceWorld.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, Util.getDimensionId(sourceWorld, Util.DimensionType.OVERWORLD_TYPE))));
        ServerLevel nether = Objects.requireNonNull(sourceWorld.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, Util.getDimensionId(sourceWorld, Util.DimensionType.NETHER_TYPE))));
        scale = DimensionType.getTeleportationScale(
                overworld.dimensionType(),
                nether.dimensionType()
        );
        this.sourcePos = (type== Util.DimensionType.OVERWORLD_TYPE?sourcePos:sourcePos.scale(1/scale)).with(Direction.Axis.Y,0);
        double d=Double.MAX_VALUE;
        HolderSet<Structure> stronghold= HolderSet.direct(overworld.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.STRONGHOLD));
        Pair<BlockPos, Holder<Structure>> pair1 =
                overworld.getChunkSource().getGenerator()
                        .findNearestMapStructure(overworld, stronghold, BlockPos.containing(sourcePos), 100, false);
        if(pair1!=null)d=Math.min(d,sourcePos.distanceTo(pair1.getFirst().getBottomCenter().with(Direction.Axis.Y,0))*scale);
        Pair<BlockPos, Holder<Structure>> pair2 =
                nether.getChunkSource().getGenerator()
                        .findNearestMapStructure(nether, stronghold, BlockPos.containing(sourcePos.scale(scale)), 100, false);
        if(pair2!=null)d=Math.min(d,sourcePos.distanceTo(pair2.getFirst().getBottomCenter().with(Direction.Axis.Y,0)));
        strongholdDistance=d;
        LevelData.RespawnData spawnPoint=overworld.getRespawnData();
        if(Util.DimensionType.getDimensionType(spawnPoint.dimension().identifier().toString())== Util.DimensionType.OVERWORLD_TYPE)
        {
            spawn=spawnPoint.pos().getCenter().with(Direction.Axis.Y,0);
        }
        else
        {
            spawn=spawnPoint.pos().getCenter().with(Direction.Axis.Y,0).scale(1/scale);
        }
    }
    public Optional<Double> calculateDistance(ServerLevel targetWorld, Vec3 targetPos)
    {
        if(!Util.getDimensionId(sourceWorld, Util.DimensionType.OVERWORLD_TYPE).equals(Util.getDimensionId(targetWorld, Util.DimensionType.OVERWORLD_TYPE)))return Optional.empty();
        targetPos=targetPos.with(Direction.Axis.Y,0);
        Util.DimensionType targetType= Util.DimensionType.getDimensionType(targetWorld);
        if(sourceType== Util.DimensionType.SINGLET
                ||(sourceType== Util.DimensionType.END_TYPE&&targetType== Util.DimensionType.END_TYPE))
        {
            return Optional.of(sourcePos.distanceTo(targetPos));
        }
        if(targetType== Util.DimensionType.NETHER_TYPE)
        {
            targetType= Util.DimensionType.OVERWORLD_TYPE;
            targetPos=targetPos.scale(1/scale);
        }
        if(sourceType== Util.DimensionType.OVERWORLD_TYPE&&targetType== Util.DimensionType.OVERWORLD_TYPE)
        {
            return Optional.of(sourcePos.distanceTo(targetPos)*scale);
        }
        if(sourceType== Util.DimensionType.OVERWORLD_TYPE)
        {
            if(strongholdDistance==Double.MAX_VALUE)return Optional.empty();
            return Optional.of(strongholdDistance*scale+platform.distanceTo(targetPos));
        }
        else
        {
            return Optional.of(sourcePos.distanceTo(returnGate)+spawn.distanceTo(targetPos)*scale);
        }
    }
}
