package io.silvicky.elina.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.silvicky.elina.common.DistanceCalculator;
import io.silvicky.elina.webmap.WebMapStorage;
import io.silvicky.elina.webmap.farm.FarmInfo;
import io.silvicky.elina.webmap.farm.FarmLookup;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static io.silvicky.elina.StateSaver.getServerState;
import static io.silvicky.elina.command.Locate.DIMENSION;
import static io.silvicky.elina.command.Locate.POS;
import static io.silvicky.elina.command.Map.*;
import static io.silvicky.elina.common.Util.collectionToString;
import static io.silvicky.elina.common.Util.getPlayerUuid;
import static io.silvicky.elina.webmap.api.APIEntry.refresh;
import static java.lang.String.format;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Farm
{
    public static final String ITEM = "item";
    public static final SimpleCommandExceptionType FARM_NOT_FOUND=new SimpleCommandExceptionType(Text.literal("Farm not found."));
    public static final CommandRegistryAccess commandRegistryAccess=CommandManager.createRegistryAccess(BuiltinRegistries.createWrapperLookup());
    public static LiteralArgumentBuilder<ServerCommandSource> farmArgumentBuilder
            = literal("farm")
            .then(literal("add")
                    .then(argument(DIMENSION,new DimensionArgumentType())
                            .then(argument(ID, StringArgumentType.string())
                                    .then(argument(POS,new BlockPosArgumentType())
                                            .then(argument(LABEL,StringArgumentType.string())
                                                    .then(argument(DETAIL,StringArgumentType.string())
                                                            .executes(ctx->add(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,ID),BlockPosArgumentType.getBlockPos(ctx,POS),StringArgumentType.getString(ctx,LABEL),StringArgumentType.getString(ctx,DETAIL)))))))))
            .then(literal("del")
                    .then(argument(DIMENSION,new DimensionArgumentType())
                            .then(argument(ID,StringArgumentType.string())
                                    .executes(ctx-> delete(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,ID))))))
            .then(literal("list")
                    .then(argument(DIMENSION,new DimensionArgumentType())
                            .executes(ctx->list(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION)))))
            .then(literal("additem")
                    .then(argument(DIMENSION,new DimensionArgumentType())
                            .then(argument(ID,StringArgumentType.string())
                                    .then(argument(ITEM, ItemStackArgumentType.itemStack(commandRegistryAccess))
                                            .executes(ctx-> addItem(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,ID),ItemStackArgumentType.getItemStackArgument(ctx,ITEM)))))))
            .then(literal("delitem")
                    .then(argument(DIMENSION,new DimensionArgumentType())
                            .then(argument(ID,StringArgumentType.string())
                                    .then(argument(ITEM, ItemStackArgumentType.itemStack(commandRegistryAccess))
                                            .executes(ctx-> deleteItem(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,ID),ItemStackArgumentType.getItemStackArgument(ctx,ITEM)))))))
            .then(literal("listitem")
                    .then(argument(DIMENSION,new DimensionArgumentType())
                            .then(argument(ID,StringArgumentType.string())
                                    .executes(ctx-> listItem(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,ID))))))
            .then(literal("find")
                    .then(argument(ITEM, ItemStackArgumentType.itemStack(commandRegistryAccess))
                            .executes(ctx-> find(ctx.getSource(),ItemStackArgumentType.getItemStackArgument(ctx,ITEM)))
                            .then(argument(DIMENSION,new DimensionArgumentType())
                                    .then(argument(POS,new BlockPosArgumentType())
                                            .executes(ctx->find(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION), Vec3d.of(BlockPosArgumentType.getBlockPos(ctx,POS)),ItemStackArgumentType.getItemStackArgument(ctx,ITEM)))))))
            .then(literal("build")
                            .executes(ctx-> build(ctx.getSource()))
                            .then(argument(DIMENSION,new DimensionArgumentType())
                                    .then(argument(POS,new BlockPosArgumentType())
                                            .executes(ctx->build(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION), Vec3d.of(BlockPosArgumentType.getBlockPos(ctx,POS)))))))
            .then(literal("findadv")
                    .then(argument(ITEM, ItemStackArgumentType.itemStack(commandRegistryAccess))
                            .executes(ctx-> findAdvanced(ctx.getSource(),ItemStackArgumentType.getItemStackArgument(ctx,ITEM)))));
    public static HashMap<String, FarmInfo> getFarmMap(ServerWorld world)
    {
        return getServerState(world.getServer()).webMapStorage
                .computeIfAbsent(world.getRegistryKey().getValue(),i->new WebMapStorage())
                .farms();
    }
    private static int add(ServerCommandSource source, ServerWorld world, String id, BlockPos pos, String label, String detail)
    {
        getFarmMap(world).compute(id,(k,v)->v==null?new FarmInfo(pos,label,detail):new FarmInfo(v.items(),pos,label,detail));
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int delete(ServerCommandSource source, ServerWorld world, String id)
    {
        getFarmMap(world).remove(id);
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int list(ServerCommandSource source, ServerWorld world)
    {
        for(java.util.Map.Entry<String, FarmInfo> entry: getFarmMap(world).entrySet())
        {
            source.sendFeedback(()-> Text.literal(format("%s: %s",entry.getKey(),entry.getValue())),false);
        }
        source.sendFeedback(()-> Text.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int addItem(ServerCommandSource source, ServerWorld world, String id, ItemStackArgument item) throws CommandSyntaxException
    {
        FarmInfo info=getFarmMap(world).get(id);
        if(info==null)throw FARM_NOT_FOUND.create();
        info.items().add(Registries.ITEM.getId(item.getItem()));
        source.sendFeedback(()-> Text.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int deleteItem(ServerCommandSource source, ServerWorld world, String id, ItemStackArgument item) throws CommandSyntaxException
    {
        FarmInfo info=getFarmMap(world).get(id);
        if(info==null)throw FARM_NOT_FOUND.create();
        info.items().remove(Registries.ITEM.getId(item.getItem()));
        source.sendFeedback(()-> Text.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int listItem(ServerCommandSource source, ServerWorld world, String id) throws CommandSyntaxException
    {
        FarmInfo info=getFarmMap(world).get(id);
        if(info==null)throw FARM_NOT_FOUND.create();
        source.sendFeedback(()-> Text.literal(collectionToString(info.items())),false);
        source.sendFeedback(()-> Text.literal("Done."),false);
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
    private static int find(ServerCommandSource source, ItemStackArgument item)
    {
        return find(source, source.getWorld(), source.getPosition(), item);
    }
    private static int find(ServerCommandSource source, ServerWorld serverWorld, Vec3d pos, ItemStackArgument item)
    {
        List<FindResult> farms=new ArrayList<>();
        DistanceCalculator distanceCalculator=new DistanceCalculator(serverWorld,pos);
        for(java.util.Map.Entry<Identifier, WebMapStorage> i:getServerState(source.getServer()).webMapStorage.entrySet())
        {
            for(java.util.Map.Entry<String, FarmInfo> j:i.getValue().farms().entrySet())
            {
                if(!j.getValue().items().contains(Registries.ITEM.getId(item.getItem())))continue;
                Optional<Double> dis=distanceCalculator.calculateDistance(
                        source.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD,i.getKey())),
                        j.getValue().pos().toCenterPos());
                if(dis.isEmpty())continue;
                farms.add(new FindResult(
                        dis.get(),
                        i.getKey(),
                        j.getKey(),
                        j.getValue()));
            }
        }
        if(farms.isEmpty())source.sendFeedback(()-> Text.literal("Not found."),false);
        else
        {
            farms.sort(Comparator.naturalOrder());
            FindResult i=farms.getFirst();
            source.sendFeedback(() -> Text.literal(i.toString()), false);
        }
        return Command.SINGLE_SUCCESS;
    }
    private static int build(ServerCommandSource source)
    {
        return build(source, source.getWorld(), source.getPosition());
    }
    private static int build(ServerCommandSource source, ServerWorld serverWorld, Vec3d pos)
    {
        List<FindResult> farms=new ArrayList<>();
        DistanceCalculator distanceCalculator=new DistanceCalculator(serverWorld,pos);
        for(java.util.Map.Entry<Identifier, WebMapStorage> i:getServerState(source.getServer()).webMapStorage.entrySet())
        {
            for(java.util.Map.Entry<String, FarmInfo> j:i.getValue().farms().entrySet())
            {
                Optional<Double> dis=distanceCalculator.calculateDistance(
                        source.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD,i.getKey())),
                        j.getValue().pos().toCenterPos());
                if(dis.isEmpty())continue;
                farms.add(new FindResult(
                        dis.get(),
                        i.getKey(),
                        j.getKey(),
                        j.getValue()));
            }
        }
        FarmLookup.build(getPlayerUuid(source.getPlayer()),farms);
        source.sendFeedback(()-> Text.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int findAdvanced(ServerCommandSource source, ItemStackArgument item) throws CommandSyntaxException
    {
        FarmLookup.lookup(source,Registries.ITEM.getEntry(item.getItem()));
        source.sendFeedback(()-> Text.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
}
