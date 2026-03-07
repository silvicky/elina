package io.silvicky.elina.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.structures.EndCityStructure;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static io.silvicky.elina.StateSaver.getServerState;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.server.commands.LocateCommand.*;

public class Locate
{
    public static final String STRUCTURE = "structure";
    public static final String DIMENSION = "dimension";
    public static final String POS = "pos";
    public static LiteralArgumentBuilder<CommandSourceStack> locateArgumentBuilder
            = literal("locate")
            .then(literal("find")
                    .then(argument(STRUCTURE, ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                            .executes(ctx->find(ctx.getSource(), ResourceOrTagKeyArgument.getResourceOrTagKey(ctx, STRUCTURE, Registries.STRUCTURE, ERROR_STRUCTURE_INVALID)))
                            .then(argument(DIMENSION,new DimensionArgument())
                                    .then(argument(POS,new BlockPosArgument())
                                            .executes(ctx->find(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION), BlockPosArgument.getBlockPos(ctx,POS), ResourceOrTagKeyArgument.getResourceOrTagKey(ctx, STRUCTURE, Registries.STRUCTURE, ERROR_STRUCTURE_INVALID)))))))
            .then(literal("mark")
                    .then(argument(STRUCTURE, ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                            .executes(ctx->mark(ctx.getSource(), ResourceOrTagKeyArgument.getResourceOrTagKey(ctx,STRUCTURE, Registries.STRUCTURE, ERROR_STRUCTURE_INVALID)))
                            .then(argument(DIMENSION,new DimensionArgument())
                                    .then(argument(POS,new BlockPosArgument())
                                            .executes(ctx->mark(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION), BlockPosArgument.getBlockPos(ctx,POS), ResourceOrTagKeyArgument.getResourceOrTagKey(ctx, STRUCTURE, Registries.STRUCTURE, ERROR_STRUCTURE_INVALID)))))))
            .then(literal("remove")
                    .then(argument(STRUCTURE, ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                            .executes(ctx->remove(ctx.getSource(), ResourceOrTagKeyArgument.getResourceOrTagKey(ctx,STRUCTURE, Registries.STRUCTURE, ERROR_STRUCTURE_INVALID)))
                            .then(argument(DIMENSION,new DimensionArgument())
                                    .then(argument(POS,new BlockPosArgument())
                                            .executes(ctx->remove(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION), BlockPosArgument.getBlockPos(ctx,POS), ResourceOrTagKeyArgument.getResourceOrTagKey(ctx, STRUCTURE, Registries.STRUCTURE, ERROR_STRUCTURE_INVALID)))))))
            .then(literal("list")
                    .then(argument(STRUCTURE, ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                            .executes(ctx->list(ctx.getSource(), ResourceOrTagKeyArgument.getResourceOrTagKey(ctx,STRUCTURE, Registries.STRUCTURE, ERROR_STRUCTURE_INVALID)))
                            .then(argument(DIMENSION,new DimensionArgument())
                                    .executes(ctx->list(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION), ResourceOrTagKeyArgument.getResourceOrTagKey(ctx, STRUCTURE, Registries.STRUCTURE, ERROR_STRUCTURE_INVALID))))));
    private static int mark(CommandSourceStack source, ResourceOrTagKeyArgument.Result<Structure> predicate) throws CommandSyntaxException
    {
        return mark(source,source.getLevel(),source.getPlayerOrException().blockPosition(), predicate);
    }
    private static int mark(CommandSourceStack source, ServerLevel serverWorld, BlockPos blockPos, ResourceOrTagKeyArgument.Result<Structure> predicate) throws CommandSyntaxException
    {
        blockPos=blockPos.atY(0);
        Pair<BlockPos, Holder<Structure>> pair = serverWorld.getChunkSource().getGenerator().findNearestMapStructure(serverWorld, getRegistryEntryList(source,predicate), blockPos, 100,false);
        if(pair==null) throw ERROR_STRUCTURE_NOT_FOUND.create(predicate.asPrintable());
        if(!pair.getFirst().atY(0).equals(blockPos))
        {
            source.sendSuccess(() -> Component.literal("Nearest:"), false);
            source.sendSuccess(() -> Component.literal(pair.getFirst().atY(0).toShortString()), false);
            return Command.SINGLE_SUCCESS;
        }
        HashSet<BlockPos> set=getBlockPosSet(predicate.unwrap().left().orElseThrow(),serverWorld);
        set.add(blockPos);
        source.sendSuccess(()-> Component.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int remove(CommandSourceStack source, ResourceOrTagKeyArgument.Result<Structure> predicate) throws CommandSyntaxException
    {
        return remove(source,source.getLevel(),source.getPlayerOrException().blockPosition(), predicate);
    }
    private static int remove(CommandSourceStack source, ServerLevel serverWorld, BlockPos blockPos, ResourceOrTagKeyArgument.Result<Structure> predicate)
    {
        blockPos=blockPos.atY(0);
        HashSet<BlockPos> set=getBlockPosSet(predicate.unwrap().left().orElseThrow(),serverWorld);
        if(!set.contains(blockPos))
        {
            double d=Double.MAX_VALUE;
            BlockPos cur=null;
            for(BlockPos blockPos1:set)
            {
                if(blockPos.distSqr(blockPos1)<d)
                {
                    d=blockPos.distSqr(blockPos1);
                    cur=blockPos1;
                }
            }
            BlockPos finalCur = cur;
            if(finalCur!=null)
            {
                source.sendSuccess(() -> Component.literal("Nearest:"), false);
                source.sendSuccess(() -> Component.literal(finalCur.toShortString()), false);
            }
            else source.sendSuccess(()-> Component.literal("Not found"),false);
            return Command.SINGLE_SUCCESS;
        }
        set.remove(blockPos);
        source.sendSuccess(()-> Component.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int list(CommandSourceStack source, ResourceOrTagKeyArgument.Result<Structure> predicate)
    {
        return list(source,source.getLevel(),predicate);
    }
    private static int list(CommandSourceStack source, ServerLevel serverWorld, ResourceOrTagKeyArgument.Result<Structure> predicate)
    {
        HashSet<BlockPos> set=getBlockPosSet(predicate.unwrap().left().orElseThrow(),serverWorld);
        for(BlockPos blockPos:set)
        {
            source.sendSuccess(()-> Component.literal(blockPos.toShortString()),false);
        }
        source.sendSuccess(()-> Component.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    public static HolderSet<Structure> getRegistryEntryList(CommandSourceStack source, ResourceOrTagKeyArgument.Result<Structure> predicate) throws CommandSyntaxException
    {
        Registry<Structure> registry = source.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        return getHolders(predicate, registry)
                .orElseThrow(() -> ERROR_STRUCTURE_INVALID.create(predicate.asPrintable()));
    }
    public static HashSet<BlockPos> getBlockPosSet(ResourceKey<Structure> structureRegistryKey, ServerLevel serverWorld)
    {
        Identifier structureId = structureRegistryKey.identifier();
        Identifier dimensionId = serverWorld.dimension().identifier();
        return getServerState(serverWorld.getServer()).visitedStructure
                .computeIfAbsent(structureId,i->new HashMap<>())
                .computeIfAbsent(dimensionId,i->new HashSet<>());
    }
    private static int find(CommandSourceStack source, ResourceOrTagKeyArgument.Result<Structure> predicate) throws CommandSyntaxException
    {
        return find(source,source.getLevel(), BlockPos.containing(source.getPosition()),predicate);
    }
    private static int find(CommandSourceStack source, ServerLevel serverWorld, BlockPos blockPos, ResourceOrTagKeyArgument.Result<Structure> predicate) throws CommandSyntaxException
    {
        HolderSet<Structure> registryEntryList = getRegistryEntryList(source,predicate);
        Pair<BlockPos, Holder<Structure>> pair = serverWorld.getChunkSource().getGenerator().findNearestMapStructure(serverWorld, registryEntryList, blockPos, 100,false);
        if (pair == null) throw ERROR_STRUCTURE_NOT_FOUND.create(predicate.asPrintable());
        return showLocateResult(source, predicate, blockPos, pair, "commands.locate.structure.success", false, Duration.ZERO);
    }
    public static boolean hasShip(ServerLevel world, ChunkPos pos)
    {
        Structure.GenerationContext context=new Structure.GenerationContext
                (
                        world.registryAccess(),
                        world.getChunkSource().getGenerator(),
                        world.getChunkSource().getGenerator().getBiomeSource(),
                        world.getChunkSource().randomState(),
                        world.getStructureManager(),
                        world.getSeed(),
                        pos,
                        world,
                        world.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.END_CITY).value().biomes()::contains
                );

        Optional<Structure.GenerationStub> position=new EndCityStructure(new Structure.StructureSettings(world.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.END_CITY).value().biomes())).findGenerationPoint(context);
        if(position.isEmpty())return false;
        List<StructurePiece> list=position.get().getPiecesBuilder().build().pieces();
        for(StructurePiece piece:list)
        {
            if(piece instanceof TemplateStructurePiece piece1&&piece1.templateName.equals("ship"))return true;
        }
        return false;
    }
}
