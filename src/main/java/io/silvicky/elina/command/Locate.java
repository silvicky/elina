package io.silvicky.elina.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.SimpleStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.EndCityStructure;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureKeys;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static io.silvicky.elina.StateSaver.getServerState;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.LocateCommand.*;

public class Locate
{
    public static final String STRUCTURE = "structure";
    public static final String DIMENSION = "dimension";
    public static final String POS = "pos";
    public static LiteralArgumentBuilder<ServerCommandSource> locateArgumentBuilder
            = literal("locate")
            .then(literal("find")
                    .then(argument(STRUCTURE,RegistryPredicateArgumentType.registryPredicate(RegistryKeys.STRUCTURE))
                            .executes(ctx->find(ctx.getSource(),RegistryPredicateArgumentType.getPredicate(ctx, STRUCTURE, RegistryKeys.STRUCTURE, STRUCTURE_INVALID_EXCEPTION)))
                            .then(argument(DIMENSION,new DimensionArgumentType())
                                    .then(argument(POS,new BlockPosArgumentType())
                                            .executes(ctx->find(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),BlockPosArgumentType.getBlockPos(ctx,POS),RegistryPredicateArgumentType.getPredicate(ctx, STRUCTURE, RegistryKeys.STRUCTURE, STRUCTURE_INVALID_EXCEPTION)))))))
            .then(literal("mark")
                    .then(argument(STRUCTURE,RegistryPredicateArgumentType.registryPredicate(RegistryKeys.STRUCTURE))
                            .then(argument(DIMENSION,new DimensionArgumentType())
                                    .then(argument(POS,new BlockPosArgumentType())
                                            .executes(ctx->mark(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),BlockPosArgumentType.getBlockPos(ctx,POS),RegistryPredicateArgumentType.getPredicate(ctx, STRUCTURE, RegistryKeys.STRUCTURE, STRUCTURE_INVALID_EXCEPTION)))))))
            .then(literal("remove")
                    .then(argument(STRUCTURE,RegistryPredicateArgumentType.registryPredicate(RegistryKeys.STRUCTURE))
                            .then(argument(DIMENSION,new DimensionArgumentType())
                                    .then(argument(POS,new BlockPosArgumentType())
                                            .executes(ctx->remove(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),BlockPosArgumentType.getBlockPos(ctx,POS),RegistryPredicateArgumentType.getPredicate(ctx, STRUCTURE, RegistryKeys.STRUCTURE, STRUCTURE_INVALID_EXCEPTION)))))))
            .then(literal("list")
                    .then(argument(STRUCTURE,RegistryPredicateArgumentType.registryPredicate(RegistryKeys.STRUCTURE))
                            .then(argument(DIMENSION,new DimensionArgumentType())
                                    .executes(ctx->list(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),RegistryPredicateArgumentType.getPredicate(ctx, STRUCTURE, RegistryKeys.STRUCTURE, STRUCTURE_INVALID_EXCEPTION))))));
    private static int mark(ServerCommandSource source, ServerWorld serverWorld, BlockPos blockPos, RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate) throws CommandSyntaxException
    {
        blockPos=blockPos.withY(0);
        Pair<BlockPos, RegistryEntry<Structure>> pair = serverWorld.getChunkManager().getChunkGenerator().locateStructure(serverWorld, getRegistryEntryList(source,predicate), blockPos, 100,false);
        if(pair==null) throw STRUCTURE_NOT_FOUND_EXCEPTION.create(predicate.asString());
        if(!pair.getFirst().withY(0).equals(blockPos))
        {
            source.sendFeedback(() -> Text.literal("Nearest:"), false);
            source.sendFeedback(() -> Text.literal(pair.getFirst().withY(0).toShortString()), false);
            return Command.SINGLE_SUCCESS;
        }
        HashSet<BlockPos> set=getBlockPosSet(predicate.getKey().left().orElseThrow(),serverWorld);
        set.add(blockPos);
        source.sendFeedback(()-> Text.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int remove(ServerCommandSource source, ServerWorld serverWorld, BlockPos blockPos, RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate) throws CommandSyntaxException
    {
        blockPos=blockPos.withY(0);
        HashSet<BlockPos> set=getBlockPosSet(predicate.getKey().left().orElseThrow(),serverWorld);
        if(!set.contains(blockPos))
        {
            double d=Double.MAX_VALUE;
            BlockPos cur=null;
            for(BlockPos blockPos1:set)
            {
                if(blockPos.getSquaredDistance(blockPos1)<d)
                {
                    d=blockPos.getSquaredDistance(blockPos1);
                    cur=blockPos1;
                }
            }
            BlockPos finalCur = cur;
            if(finalCur!=null)
            {
                source.sendFeedback(() -> Text.literal("Nearest:"), false);
                source.sendFeedback(() -> Text.literal(finalCur.toShortString()), false);
            }
            else source.sendFeedback(()->Text.literal("Not found"),false);
            return Command.SINGLE_SUCCESS;
        }
        set.remove(blockPos);
        source.sendFeedback(()-> Text.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int list(ServerCommandSource source, ServerWorld serverWorld, RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate) throws CommandSyntaxException
    {
        HashSet<BlockPos> set=getBlockPosSet(predicate.getKey().left().orElseThrow(),serverWorld);
        for(BlockPos blockPos:set)
        {
            source.sendFeedback(()->Text.literal(blockPos.toShortString()),false);
        }
        source.sendFeedback(()->Text.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    public static RegistryEntryList<Structure> getRegistryEntryList(ServerCommandSource source,RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate) throws CommandSyntaxException
    {
        Registry<Structure> registry = source.getWorld().getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
        return getStructureListForPredicate(predicate, registry)
                .orElseThrow(() -> STRUCTURE_INVALID_EXCEPTION.create(predicate.asString()));
    }
    public static HashSet<BlockPos> getBlockPosSet(RegistryKey<Structure> structureRegistryKey, ServerWorld serverWorld)
    {
        Identifier structureId = structureRegistryKey.getValue();
        Identifier dimensionId = serverWorld.getRegistryKey().getValue();
        HashMap<Identifier, HashMap<Identifier, HashSet<BlockPos>>> map1=getServerState(serverWorld.getServer()).visitedStructure;
        if(!map1.containsKey(structureId))map1.put(structureId,new HashMap<>());
        HashMap<Identifier, HashSet<BlockPos>> map2=map1.get(structureId);
        if(!map2.containsKey(dimensionId))map2.put(dimensionId,new HashSet<>());
        return map2.get(dimensionId);
    }
    private static int find(ServerCommandSource source, RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate) throws CommandSyntaxException
    {
        return find(source,source.getWorld(),BlockPos.ofFloored(source.getPosition()),predicate);
    }
    private static int find(ServerCommandSource source, ServerWorld serverWorld, BlockPos blockPos, RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate) throws CommandSyntaxException
    {
        RegistryEntryList<Structure> registryEntryList = getRegistryEntryList(source,predicate);
        Pair<BlockPos, RegistryEntry<Structure>> pair = serverWorld.getChunkManager().getChunkGenerator().locateStructure(serverWorld, registryEntryList, blockPos, 100,false);
        if (pair == null) throw STRUCTURE_NOT_FOUND_EXCEPTION.create(predicate.asString());
        return sendCoordinates(source, predicate, blockPos, pair, "commands.locate.structure.success", false, Duration.ZERO);
    }
    public static boolean hasShip(ServerWorld world, ChunkPos pos)
    {
        Structure.Context context=new Structure.Context
                (
                        world.getRegistryManager(),
                        world.getChunkManager().getChunkGenerator(),
                        world.getChunkManager().getChunkGenerator().getBiomeSource(),
                        world.getChunkManager().getNoiseConfig(),
                        world.getStructureTemplateManager(),
                        world.getSeed(),
                        pos,
                        world,
                        world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE).getOrThrow(StructureKeys.END_CITY).value().getValidBiomes()::contains
                );

        Optional<Structure.StructurePosition> position=new EndCityStructure(new Structure.Config(world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE).getOrThrow(StructureKeys.END_CITY).value().getValidBiomes())).getStructurePosition(context);
        if(position.isEmpty())return false;
        List<StructurePiece> list=position.get().generate().toList().pieces();
        for(StructurePiece piece:list)
        {
            if(piece instanceof SimpleStructurePiece piece1&&piece1.templateIdString.equals("ship"))return true;
        }
        return false;
    }
}
