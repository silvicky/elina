package io.silvicky.elina.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.silvicky.elina.webmap.WebMapStorage;
import io.silvicky.elina.webmap.entities.Point;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;

import static io.silvicky.elina.StateSaver.getServerState;
import static io.silvicky.elina.command.Locate.DIMENSION;
import static io.silvicky.elina.command.Locate.POS;
import static java.lang.String.format;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static io.silvicky.elina.webmap.api.APIEntry.refresh;

public class Map
{
    public static final String LABEL = "label";
    public static final String DETAIL = "detail";
    public static final String ICON = "icon";
    public static final String ID = "id";
    public static final String SET = "set";
    public static LiteralArgumentBuilder<ServerCommandSource> mapArgumentBuilder
            = literal("map")
            .then(literal("mark")
                    .then(argument(DIMENSION,new DimensionArgumentType())
                            .then(argument(ID,StringArgumentType.string())
                                    .then(argument(SET,StringArgumentType.string())
                                            .then(argument(POS,new BlockPosArgumentType())
                                                    .then(argument(ICON, IntegerArgumentType.integer())
                                                            .then(argument(LABEL,StringArgumentType.string())
                                                                    .then(argument(DETAIL,StringArgumentType.string())
                                                                            .executes(ctx->mark(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,ID),StringArgumentType.getString(ctx,SET),BlockPosArgumentType.getBlockPos(ctx,POS),IntegerArgumentType.getInteger(ctx, ICON),StringArgumentType.getString(ctx,LABEL),StringArgumentType.getString(ctx,DETAIL)))))))))))
            .then(literal("remove")
                    .then(argument(DIMENSION,new DimensionArgumentType())
                            .then(argument(ID,StringArgumentType.string())
                                    .then(argument(SET,StringArgumentType.string())
                                            .executes(ctx->remove(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,ID),StringArgumentType.getString(ctx,SET)))))))
            .then(literal("list")
                    .then(argument(DIMENSION,new DimensionArgumentType())
                            .then(argument(SET,StringArgumentType.string())
                                    .executes(ctx->list(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,SET))))))
            .then(literal("addset")
                    .then(argument(DIMENSION,new DimensionArgumentType())
                            .then(argument(SET,StringArgumentType.string())
                                    .then(argument(LABEL,StringArgumentType.string())
                                            .executes(ctx->addSet(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,SET),StringArgumentType.getString(ctx,LABEL)))))))
            .then(literal("delset")
                    .then(argument(DIMENSION,new DimensionArgumentType())
                            .then(argument(SET,StringArgumentType.string())
                                    .executes(ctx->deleteSet(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,SET))))));
    public static HashMap<String, Point> getPointMap(ServerWorld world, String set)
    {
        HashMap<Identifier,WebMapStorage> set0=getServerState(world.getServer()).webMapStorage;
        if(!set0.containsKey(world.getRegistryKey().getValue()))set0.put(world.getRegistryKey().getValue(),new WebMapStorage());
        HashMap<String, HashMap<String, Point>> set1=set0.get(world.getRegistryKey().getValue()).points();
        if(!set1.containsKey(set))set1.put(set,new HashMap<>());
        return set1.get(set);
    }
    public static HashMap<String, String> getSetMap(ServerWorld world)
    {
        HashMap<Identifier,WebMapStorage> set0=getServerState(world.getServer()).webMapStorage;
        if(!set0.containsKey(world.getRegistryKey().getValue()))set0.put(world.getRegistryKey().getValue(),new WebMapStorage());
        return set0.get(world.getRegistryKey().getValue()).sets();
    }
    private static int mark(ServerCommandSource source, ServerWorld world, String id, String set,BlockPos pos, int icon, String label, String detail)
    {
        HashMap<String, Point> set2=getPointMap(world, set);
        set2.put(id,new Point(pos,label,detail,icon));
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int remove(ServerCommandSource source, ServerWorld world, String id, String set)
    {
        HashMap<String, Point> set2=getPointMap(world, set);
        set2.remove(id);
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int list(ServerCommandSource source, ServerWorld world, String set)
    {
        HashMap<String, Point> set2=getPointMap(world, set);
        for(java.util.Map.Entry<String,Point> entry: set2.entrySet())
        {
            source.sendFeedback(()-> Text.literal(format("%s: %s",entry.getKey(),entry.getValue())),false);
        }
        source.sendFeedback(()-> Text.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int addSet(ServerCommandSource source, ServerWorld world, String set, String label)
    {
        HashMap<String,String> set1=getSetMap(world);
        set1.put(set,label);
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int deleteSet(ServerCommandSource source, ServerWorld world, String set)
    {
        HashMap<String,String> set1=getSetMap(world);
        set1.remove(set);
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
}
