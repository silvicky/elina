package io.silvicky.elina.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.silvicky.elina.webmap.WebMapStorage;
import io.silvicky.elina.webmap.subway.SubwayLine;
import io.silvicky.elina.webmap.subway.SubwayStation;
import io.silvicky.elina.webmap.subway.SubwaySystem;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.HexColorArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.CompletableFuture;

import static io.silvicky.elina.StateSaver.getServerState;
import static io.silvicky.elina.command.Locate.DIMENSION;
import static io.silvicky.elina.command.Locate.POS;
import static io.silvicky.elina.command.Map.*;
import static io.silvicky.elina.webmap.api.APIEntry.refresh;
import static java.lang.String.format;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Subway
{
    public static final String COLOR="color";
    public static final String RING="ring";
    public static final String PREV = "prev";
    public static final String NEW="new";
    public static final SimpleCommandExceptionType LINE_NOT_FOUND=new SimpleCommandExceptionType(Text.literal("Line not found."));
    public static final SimpleCommandExceptionType STATION_NOT_FOUND=new SimpleCommandExceptionType(Text.literal("Station not found."));
    public static class SubwayLineSuggestionProvider implements SuggestionProvider<ServerCommandSource>
    {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException
        {
            ServerWorld world=DimensionArgumentType.getDimensionArgument(commandContext,DIMENSION);
            for (String id:getSubway(world).lines.keySet()) suggestionsBuilder.suggest(id);
            return suggestionsBuilder.buildFuture();
        }
    }
    public static LiteralArgumentBuilder<ServerCommandSource> subwayArgumentBuilder
            = literal("subway")
            .then(literal("icon")
                    .then(argument(DIMENSION, DimensionArgumentType.dimension())
                            .then(argument(ICON, IntegerArgumentType.integer())
                                    .executes(ctx->setSystemIcon(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),IntegerArgumentType.getInteger(ctx,ICON))))))
            .then(literal("station")
                    .then(literal("add")
                            .then(argument(DIMENSION,new DimensionArgumentType())
                                    .then(argument(ID,StringArgumentType.string())
                                            .then(argument(POS,new BlockPosArgumentType())
                                                    .then(argument(LABEL,StringArgumentType.string())
                                                            .then(argument(DETAIL,StringArgumentType.string())
                                                                    .executes(ctx->addStation(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,ID),BlockPosArgumentType.getBlockPos(ctx,POS),StringArgumentType.getString(ctx,LABEL),StringArgumentType.getString(ctx,DETAIL)))))))))
                    .then(literal("del")
                            .then(argument(DIMENSION,new DimensionArgumentType())
                                    .then(argument(ID,StringArgumentType.string())
                                            .executes(ctx->deleteStation(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,ID))))))
                    .then(literal("list")
                            .then(argument(DIMENSION,new DimensionArgumentType())
                                    .executes(ctx->listStation(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION))))))
            .then(literal("line")
                    .then(literal("add")
                            .then(argument(DIMENSION, DimensionArgumentType.dimension())
                                    .then(argument(ID,StringArgumentType.string())
                                            .suggests(new SubwayLineSuggestionProvider())
                                            .then(argument(LABEL,StringArgumentType.string())
                                                    .then(argument(ICON,IntegerArgumentType.integer())
                                                            .then(argument(COLOR, HexColorArgumentType.hexColor())
                                                                    .then(argument(RING, BoolArgumentType.bool())
                                                                            .executes(ctx->addLine(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,ID),StringArgumentType.getString(ctx,LABEL),IntegerArgumentType.getInteger(ctx,ICON),HexColorArgumentType.getArgbColor(ctx,COLOR),BoolArgumentType.getBool(ctx,RING))))))))))
                    .then(literal("del")
                            .then(argument(DIMENSION, DimensionArgumentType.dimension())
                                    .then(argument(ID,StringArgumentType.string())
                                            .suggests(new SubwayLineSuggestionProvider())
                                            .executes(ctx->deleteLine(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,ID))))))
                    .then(literal("list")
                            .then(argument(DIMENSION, DimensionArgumentType.dimension())
                                    .executes(ctx->listLine(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION)))))
                    .then(literal("addstation")
                            .then(argument(DIMENSION, DimensionArgumentType.dimension())
                                    .then(argument(ID,StringArgumentType.string())
                                            .suggests(new SubwayLineSuggestionProvider())
                                            .then(argument(NEW,StringArgumentType.string())
                                                    .then(argument(PREV,StringArgumentType.string())
                                                            .executes(ctx->addStationToLine(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,ID),StringArgumentType.getString(ctx,NEW),StringArgumentType.getString(ctx,PREV))))))))
                    .then(literal("delstation")
                            .then(argument(DIMENSION, DimensionArgumentType.dimension())
                                    .then(argument(ID,StringArgumentType.string())
                                            .suggests(new SubwayLineSuggestionProvider())
                                            .then(argument(NEW,StringArgumentType.string())
                                                    .executes(ctx->deleteStationFromLine(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,ID),StringArgumentType.getString(ctx,NEW)))))))
                    .then(literal("liststation")
                            .then(argument(DIMENSION, DimensionArgumentType.dimension())
                                    .then(argument(ID,StringArgumentType.string())
                                            .suggests(new SubwayLineSuggestionProvider())
                                            .executes(ctx->listStationFromLine(ctx.getSource(),DimensionArgumentType.getDimensionArgument(ctx,DIMENSION),StringArgumentType.getString(ctx,ID)))))));
    public static SubwaySystem getSubway(ServerWorld world)
    {
        return getServerState(world.getServer()).webMapStorage
                .computeIfAbsent(world.getRegistryKey().getValue(),i->new WebMapStorage())
                .subwaySystem();
    }
    public static SubwayLine getSubwayLine(ServerWorld world, String id)
    {
        return getSubway(world).lines.computeIfAbsent(id,i->new SubwayLine());
    }
    private static int setSystemIcon(ServerCommandSource source, ServerWorld serverWorld, int icon)
    {
        SubwaySystem subwaySystem=getSubway(serverWorld);
        subwaySystem.icon=icon;
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int addLine(ServerCommandSource source, ServerWorld serverWorld, String id, String label, int icon, int color,boolean ring)
    {
        SubwayLine line=getSubwayLine(serverWorld,id);
        line.label=label;
        line.icon=icon;
        line.color=color;
        line.ring=ring;
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int deleteLine(ServerCommandSource source, ServerWorld serverWorld, String id)
    {
        getSubway(serverWorld).lines.remove(id);
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int listLine(ServerCommandSource source, ServerWorld serverWorld)
    {
        for(java.util.Map.Entry<String,SubwayLine> line:getSubway(serverWorld).lines.entrySet())
        {
            source.sendFeedback(()-> Text.literal(format("%s: %s",line.getKey(),line.getValue())),false);
        }
        source.sendFeedback(()-> Text.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int addStation(ServerCommandSource source, ServerWorld serverWorld, String id, BlockPos pos, String label, String detail)
    {
        getSubway(serverWorld).stationDetails.put(id,new SubwayStation(pos,label,detail));
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int deleteStation(ServerCommandSource source, ServerWorld serverWorld, String id)
    {
        getSubway(serverWorld).stationDetails.remove(id);
        for(SubwayLine line:getSubway(serverWorld).lines.values())line.remove(id);
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int listStation(ServerCommandSource source, ServerWorld serverWorld)
    {
        for(java.util.Map.Entry<String,SubwayStation> station:getSubway(serverWorld).stationDetails.entrySet())
        {
            source.sendFeedback(()-> Text.literal(format("%s: %s",station.getKey(),station.getValue())),false);
        }
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int addStationToLine(ServerCommandSource source, ServerWorld serverWorld, String id, String newStation, String prev) throws CommandSyntaxException
    {
        SubwayLine line = getSubway(serverWorld).lines.get(id);
        if(line==null)throw LINE_NOT_FOUND.create();
        if(!getSubway(serverWorld).stationDetails.containsKey(newStation))throw STATION_NOT_FOUND.create();
        line.insert(newStation, prev);
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int deleteStationFromLine(ServerCommandSource source, ServerWorld serverWorld, String id, String newStation) throws CommandSyntaxException
    {
        SubwayLine line = getSubway(serverWorld).lines.get(id);
        if(line==null)throw LINE_NOT_FOUND.create();
        line.remove(newStation);
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int listStationFromLine(ServerCommandSource source, ServerWorld serverWorld, String id) throws CommandSyntaxException
    {
        SubwayLine line = getSubway(serverWorld).lines.get(id);
        if(line==null)throw LINE_NOT_FOUND.create();
        for(String station:line.stations)
        {
            source.sendFeedback(()-> Text.literal(format("%s: %s",station,getSubway(serverWorld).stationDetails.get(station))),false);
        }
        source.sendFeedback(()-> Text.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
}
