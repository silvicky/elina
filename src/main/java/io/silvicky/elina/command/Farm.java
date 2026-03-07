package io.silvicky.elina.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.silvicky.elina.common.DistanceCalculator;
import io.silvicky.elina.webmap.WebMapStorage;
import io.silvicky.elina.webmap.farm.FarmInfo;
import io.silvicky.elina.webmap.farm.FarmLookup;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static io.silvicky.elina.StateSaver.getServerState;
import static io.silvicky.elina.command.Locate.DIMENSION;
import static io.silvicky.elina.command.Locate.POS;
import static io.silvicky.elina.command.Map.*;
import static io.silvicky.elina.common.Util.collectionToString;
import static io.silvicky.elina.common.Util.getPlayerUuid;
import static io.silvicky.elina.webmap.api.APIEntry.refresh;
import static java.lang.String.format;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Farm
{
    public static final String ITEM = "item";
    public static final SimpleCommandExceptionType FARM_NOT_FOUND=new SimpleCommandExceptionType(Component.literal("Farm not found."));
    public static final CommandBuildContext commandRegistryAccess= Commands.createValidationContext(VanillaRegistries.createLookup());
    private static class FarmSuggestionProvider implements SuggestionProvider<CommandSourceStack>
    {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException
        {
            ServerLevel world= DimensionArgument.getDimension(commandContext,DIMENSION);
            for (String id:getFarmMap(world).keySet()) suggestionsBuilder.suggest(id);
            return suggestionsBuilder.buildFuture();
        }
    }
    private static class FarmItemSuggestionProvider implements SuggestionProvider<CommandSourceStack>
    {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException
        {
            ServerLevel world= DimensionArgument.getDimension(commandContext,DIMENSION);
            FarmInfo info=getFarmMap(world).get(StringArgumentType.getString(commandContext,ID));
            if(info==null)return suggestionsBuilder.buildFuture();
            for (Identifier id:info.items()) suggestionsBuilder.suggest(id.toString());
            return suggestionsBuilder.buildFuture();
        }
    }
    public static LiteralArgumentBuilder<CommandSourceStack> farmArgumentBuilder
            = literal("farm")
            .then(literal("add")
                    .then(argument(DIMENSION,new DimensionArgument())
                            .then(argument(ID, StringArgumentType.string())
                                    .suggests(new FarmSuggestionProvider())
                                    .then(argument(POS,new BlockPosArgument())
                                            .then(argument(LABEL,StringArgumentType.string())
                                                    .then(argument(DETAIL,StringArgumentType.string())
                                                            .executes(ctx->add(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,ID), BlockPosArgument.getBlockPos(ctx,POS),StringArgumentType.getString(ctx,LABEL),StringArgumentType.getString(ctx,DETAIL)))))))))
            .then(literal("del")
                    .then(argument(DIMENSION,new DimensionArgument())
                            .then(argument(ID,StringArgumentType.string())
                                    .suggests(new FarmSuggestionProvider())
                                    .executes(ctx-> delete(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,ID))))))
            .then(literal("list")
                    .then(argument(DIMENSION,new DimensionArgument())
                            .executes(ctx->list(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION)))))
            .then(literal("additem")
                    .then(argument(DIMENSION,new DimensionArgument())
                            .then(argument(ID,StringArgumentType.string())
                                    .suggests(new FarmSuggestionProvider())
                                    .then(argument(ITEM, ItemArgument.item(commandRegistryAccess))
                                            .executes(ctx-> addItem(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,ID), ItemArgument.getItem(ctx,ITEM)))))))
            .then(literal("delitem")
                    .then(argument(DIMENSION,new DimensionArgument())
                            .then(argument(ID,StringArgumentType.string())
                                    .suggests(new FarmSuggestionProvider())
                                    .then(argument(ITEM, ItemArgument.item(commandRegistryAccess))
                                            .suggests(new FarmItemSuggestionProvider())
                                            .executes(ctx-> deleteItem(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,ID), ItemArgument.getItem(ctx,ITEM)))))))
            .then(literal("listitem")
                    .then(argument(DIMENSION,new DimensionArgument())
                            .then(argument(ID,StringArgumentType.string())
                                    .suggests(new FarmSuggestionProvider())
                                    .executes(ctx-> listItem(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,ID))))))
            .then(literal("find")
                    .then(argument(ITEM, ItemArgument.item(commandRegistryAccess))
                            .executes(ctx-> find(ctx.getSource(), ItemArgument.getItem(ctx,ITEM)))
                            .then(argument(DIMENSION,new DimensionArgument())
                                    .then(argument(POS,new BlockPosArgument())
                                            .executes(ctx->find(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION), Vec3.atLowerCornerOf(BlockPosArgument.getBlockPos(ctx,POS)), ItemArgument.getItem(ctx,ITEM)))))))
            .then(literal("build")
                            .executes(ctx-> build(ctx.getSource()))
                            .then(argument(DIMENSION,new DimensionArgument())
                                    .then(argument(POS,new BlockPosArgument())
                                            .executes(ctx->build(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION), Vec3.atLowerCornerOf(BlockPosArgument.getBlockPos(ctx,POS)))))))
            .then(literal("findadv")
                    .then(argument(ITEM, ItemArgument.item(commandRegistryAccess))
                            .executes(ctx-> findAdvanced(ctx.getSource(), ItemArgument.getItem(ctx,ITEM)))));
    public static HashMap<String, FarmInfo> getFarmMap(ServerLevel world)
    {
        return getServerState(world.getServer()).webMapStorage
                .computeIfAbsent(world.dimension().identifier(), i->new WebMapStorage())
                .farms();
    }
    private static int add(CommandSourceStack source, ServerLevel world, String id, BlockPos pos, String label, String detail)
    {
        getFarmMap(world).compute(id,(k,v)->v==null?new FarmInfo(pos,label,detail):new FarmInfo(v.items(),pos,label,detail));
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int delete(CommandSourceStack source, ServerLevel world, String id)
    {
        getFarmMap(world).remove(id);
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int list(CommandSourceStack source, ServerLevel world)
    {
        for(java.util.Map.Entry<String, FarmInfo> entry: getFarmMap(world).entrySet())
        {
            source.sendSuccess(()-> Component.literal(format("%s: %s",entry.getKey(),entry.getValue())),false);
        }
        source.sendSuccess(()-> Component.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int addItem(CommandSourceStack source, ServerLevel world, String id, ItemInput item) throws CommandSyntaxException
    {
        FarmInfo info=getFarmMap(world).get(id);
        if(info==null)throw FARM_NOT_FOUND.create();
        info.items().add(BuiltInRegistries.ITEM.getKey(item.getItem()));
        source.sendSuccess(()-> Component.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int deleteItem(CommandSourceStack source, ServerLevel world, String id, ItemInput item) throws CommandSyntaxException
    {
        FarmInfo info=getFarmMap(world).get(id);
        if(info==null)throw FARM_NOT_FOUND.create();
        info.items().remove(BuiltInRegistries.ITEM.getKey(item.getItem()));
        source.sendSuccess(()-> Component.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int listItem(CommandSourceStack source, ServerLevel world, String id) throws CommandSyntaxException
    {
        FarmInfo info=getFarmMap(world).get(id);
        if(info==null)throw FARM_NOT_FOUND.create();
        source.sendSuccess(()-> Component.literal(collectionToString(info.items())),false);
        source.sendSuccess(()-> Component.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    public record FindResult(double distance, Identifier dimension, String id, FarmInfo info) implements Comparable<FindResult>
    {
        @Override
        public String toString()
        {
            return format("%s: %s, %s, %dm",id,info,dimension,(long)distance);
        }
        @Override
        public int compareTo(@NotNull Farm.FindResult o)
        {
            return Double.compare(distance,o.distance);
        }
    }
    private static int find(CommandSourceStack source, ItemInput item)
    {
        return find(source, source.getLevel(), source.getPosition(), item);
    }
    private static int find(CommandSourceStack source, ServerLevel serverWorld, Vec3 pos, ItemInput item)
    {
        List<FindResult> farms=new ArrayList<>();
        DistanceCalculator distanceCalculator=new DistanceCalculator(serverWorld,pos);
        for(java.util.Map.Entry<Identifier, WebMapStorage> i:getServerState(source.getServer()).webMapStorage.entrySet())
        {
            for(java.util.Map.Entry<String, FarmInfo> j:i.getValue().farms().entrySet())
            {
                if(!j.getValue().items().contains(BuiltInRegistries.ITEM.getKey(item.getItem())))continue;
                Optional<Double> dis=distanceCalculator.calculateDistance(
                        source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,i.getKey())),
                        j.getValue().pos().getCenter());
                if(dis.isEmpty())continue;
                farms.add(new FindResult(
                        dis.get(),
                        i.getKey(),
                        j.getKey(),
                        j.getValue()));
            }
        }
        if(farms.isEmpty())source.sendSuccess(()-> Component.literal("Not found."),false);
        else
        {
            farms.sort(Comparator.naturalOrder());
            FindResult i=farms.getFirst();
            source.sendSuccess(() -> Component.literal(i.toString()), false);
        }
        return Command.SINGLE_SUCCESS;
    }
    private static int build(CommandSourceStack source)
    {
        return build(source, source.getLevel(), source.getPosition());
    }
    private static int build(CommandSourceStack source, ServerLevel serverWorld, Vec3 pos)
    {
        List<FindResult> farms=new ArrayList<>();
        DistanceCalculator distanceCalculator=new DistanceCalculator(serverWorld,pos);
        for(java.util.Map.Entry<Identifier, WebMapStorage> i:getServerState(source.getServer()).webMapStorage.entrySet())
        {
            for(java.util.Map.Entry<String, FarmInfo> j:i.getValue().farms().entrySet())
            {
                Optional<Double> dis=distanceCalculator.calculateDistance(
                        source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,i.getKey())),
                        j.getValue().pos().getCenter());
                if(dis.isEmpty())continue;
                farms.add(new FindResult(
                        dis.get(),
                        i.getKey(),
                        j.getKey(),
                        j.getValue()));
            }
        }
        FarmLookup.build(getPlayerUuid(source.getPlayer()),farms);
        source.sendSuccess(()-> Component.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int findAdvanced(CommandSourceStack source, ItemInput item) throws CommandSyntaxException
    {
        FarmLookup.lookup(source, BuiltInRegistries.ITEM.wrapAsHolder(item.getItem()));
        source.sendSuccess(()-> Component.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
}
