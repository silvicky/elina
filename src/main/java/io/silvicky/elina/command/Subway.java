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
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.HexColorArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

import java.util.concurrent.CompletableFuture;

import static io.silvicky.elina.StateSaver.getServerState;
import static io.silvicky.elina.command.Locate.DIMENSION;
import static io.silvicky.elina.command.Locate.POS;
import static io.silvicky.elina.command.Map.*;
import static io.silvicky.elina.webmap.api.APIEntry.refresh;
import static java.lang.String.format;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Subway
{
    public static final String COLOR="color";
    public static final String RING="ring";
    public static final String PREV = "prev";
    public static final String NEW="new";
    public static final SimpleCommandExceptionType LINE_NOT_FOUND=new SimpleCommandExceptionType(Component.literal("Line not found."));
    public static final SimpleCommandExceptionType STATION_NOT_FOUND=new SimpleCommandExceptionType(Component.literal("Station not found."));
    private static class SubwayLineSuggestionProvider implements SuggestionProvider<CommandSourceStack>
    {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException
        {
            ServerLevel world= DimensionArgument.getDimension(commandContext,DIMENSION);
            for (String id:getSubway(world).lines.keySet()) suggestionsBuilder.suggest(id);
            return suggestionsBuilder.buildFuture();
        }
    }
    private static class SubwayStationSuggestionProvider implements SuggestionProvider<CommandSourceStack>
    {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException
        {
            ServerLevel world= DimensionArgument.getDimension(commandContext,DIMENSION);
            for (String id:getSubway(world).stationDetails.keySet()) suggestionsBuilder.suggest(id);
            return suggestionsBuilder.buildFuture();
        }
    }
    private static class SubwayLineStationSuggestionProvider implements SuggestionProvider<CommandSourceStack>
    {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException
        {
            ServerLevel world= DimensionArgument.getDimension(commandContext,DIMENSION);
            String id=StringArgumentType.getString(commandContext,ID);
            SubwayLine line=getSubway(world).lines.get(id);
            if(line==null)return suggestionsBuilder.buildFuture();
            for (String station:line.stations) suggestionsBuilder.suggest(station);
            return suggestionsBuilder.buildFuture();
        }
    }
    public static LiteralArgumentBuilder<CommandSourceStack> subwayArgumentBuilder
            = literal("subway")
            .then(literal("icon")
                    .then(argument(DIMENSION, DimensionArgument.dimension())
                            .then(argument(ICON, IntegerArgumentType.integer())
                                    .executes(ctx->setSystemIcon(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),IntegerArgumentType.getInteger(ctx,ICON))))))
            .then(literal("station")
                    .then(literal("add")
                            .then(argument(DIMENSION,new DimensionArgument())
                                    .then(argument(ID,StringArgumentType.string())
                                            .suggests(new SubwayStationSuggestionProvider())
                                            .then(argument(POS,new BlockPosArgument())
                                                    .then(argument(LABEL,StringArgumentType.string())
                                                            .then(argument(DETAIL,StringArgumentType.string())
                                                                    .executes(ctx->addStation(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,ID), BlockPosArgument.getBlockPos(ctx,POS),StringArgumentType.getString(ctx,LABEL),StringArgumentType.getString(ctx,DETAIL)))))))))
                    .then(literal("del")
                            .then(argument(DIMENSION,new DimensionArgument())
                                    .then(argument(ID,StringArgumentType.string())
                                            .suggests(new SubwayStationSuggestionProvider())
                                            .executes(ctx->deleteStation(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,ID))))))
                    .then(literal("list")
                            .then(argument(DIMENSION,new DimensionArgument())
                                    .executes(ctx->listStation(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION))))))
            .then(literal("line")
                    .then(literal("add")
                            .then(argument(DIMENSION, DimensionArgument.dimension())
                                    .then(argument(ID,StringArgumentType.string())
                                            .suggests(new SubwayLineSuggestionProvider())
                                            .then(argument(LABEL,StringArgumentType.string())
                                                    .then(argument(ICON,IntegerArgumentType.integer())
                                                            .then(argument(COLOR, HexColorArgument.hexColor())
                                                                    .then(argument(RING, BoolArgumentType.bool())
                                                                            .executes(ctx->addLine(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,ID),StringArgumentType.getString(ctx,LABEL),IntegerArgumentType.getInteger(ctx,ICON), HexColorArgument.getHexColor(ctx,COLOR),BoolArgumentType.getBool(ctx,RING))))))))))
                    .then(literal("del")
                            .then(argument(DIMENSION, DimensionArgument.dimension())
                                    .then(argument(ID,StringArgumentType.string())
                                            .suggests(new SubwayLineSuggestionProvider())
                                            .executes(ctx->deleteLine(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,ID))))))
                    .then(literal("list")
                            .then(argument(DIMENSION, DimensionArgument.dimension())
                                    .executes(ctx->listLine(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION)))))
                    .then(literal("addstation")
                            .then(argument(DIMENSION, DimensionArgument.dimension())
                                    .then(argument(ID,StringArgumentType.string())
                                            .suggests(new SubwayLineSuggestionProvider())
                                            .then(argument(NEW,StringArgumentType.string())
                                                    .suggests(new SubwayStationSuggestionProvider())
                                                    .then(argument(PREV,StringArgumentType.string())
                                                            .suggests(new SubwayLineStationSuggestionProvider())
                                                            .executes(ctx->addStationToLine(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,ID),StringArgumentType.getString(ctx,NEW),StringArgumentType.getString(ctx,PREV))))))))
                    .then(literal("delstation")
                            .then(argument(DIMENSION, DimensionArgument.dimension())
                                    .then(argument(ID,StringArgumentType.string())
                                            .suggests(new SubwayLineSuggestionProvider())
                                            .then(argument(NEW,StringArgumentType.string())
                                                    .suggests(new SubwayLineStationSuggestionProvider())
                                                    .executes(ctx->deleteStationFromLine(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,ID),StringArgumentType.getString(ctx,NEW)))))))
                    .then(literal("liststation")
                            .then(argument(DIMENSION, DimensionArgument.dimension())
                                    .then(argument(ID,StringArgumentType.string())
                                            .suggests(new SubwayLineSuggestionProvider())
                                            .executes(ctx->listStationFromLine(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,ID)))))));
    public static SubwaySystem getSubway(ServerLevel world)
    {
        return getServerState(world.getServer()).webMapStorage
                .computeIfAbsent(world.dimension().identifier(), i->new WebMapStorage())
                .subwaySystem();
    }
    public static SubwayLine getSubwayLine(ServerLevel world, String id)
    {
        return getSubway(world).lines.computeIfAbsent(id,i->new SubwayLine());
    }
    private static int setSystemIcon(CommandSourceStack source, ServerLevel serverWorld, int icon)
    {
        SubwaySystem subwaySystem=getSubway(serverWorld);
        subwaySystem.icon=icon;
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int addLine(CommandSourceStack source, ServerLevel serverWorld, String id, String label, int icon, int color, boolean ring)
    {
        SubwayLine line=getSubwayLine(serverWorld,id);
        line.label=label;
        line.icon=icon;
        line.color=color;
        line.ring=ring;
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int deleteLine(CommandSourceStack source, ServerLevel serverWorld, String id)
    {
        getSubway(serverWorld).lines.remove(id);
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int listLine(CommandSourceStack source, ServerLevel serverWorld)
    {
        for(java.util.Map.Entry<String,SubwayLine> line:getSubway(serverWorld).lines.entrySet())
        {
            source.sendSuccess(()-> Component.literal(format("%s: %s",line.getKey(),line.getValue())),false);
        }
        source.sendSuccess(()-> Component.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int addStation(CommandSourceStack source, ServerLevel serverWorld, String id, BlockPos pos, String label, String detail)
    {
        getSubway(serverWorld).stationDetails.put(id,new SubwayStation(pos,label,detail));
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int deleteStation(CommandSourceStack source, ServerLevel serverWorld, String id)
    {
        getSubway(serverWorld).stationDetails.remove(id);
        for(SubwayLine line:getSubway(serverWorld).lines.values())line.remove(id);
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int listStation(CommandSourceStack source, ServerLevel serverWorld)
    {
        for(java.util.Map.Entry<String,SubwayStation> station:getSubway(serverWorld).stationDetails.entrySet())
        {
            source.sendSuccess(()-> Component.literal(format("%s: %s",station.getKey(),station.getValue())),false);
        }
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int addStationToLine(CommandSourceStack source, ServerLevel serverWorld, String id, String newStation, String prev) throws CommandSyntaxException
    {
        SubwayLine line = getSubway(serverWorld).lines.get(id);
        if(line==null)throw LINE_NOT_FOUND.create();
        if(!getSubway(serverWorld).stationDetails.containsKey(newStation))throw STATION_NOT_FOUND.create();
        line.insert(newStation, prev);
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int deleteStationFromLine(CommandSourceStack source, ServerLevel serverWorld, String id, String newStation) throws CommandSyntaxException
    {
        SubwayLine line = getSubway(serverWorld).lines.get(id);
        if(line==null)throw LINE_NOT_FOUND.create();
        line.remove(newStation);
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int listStationFromLine(CommandSourceStack source, ServerLevel serverWorld, String id) throws CommandSyntaxException
    {
        SubwayLine line = getSubway(serverWorld).lines.get(id);
        if(line==null)throw LINE_NOT_FOUND.create();
        for(String station:line.stations)
        {
            source.sendSuccess(()-> Component.literal(format("%s: %s",station,getSubway(serverWorld).stationDetails.get(station))),false);
        }
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
}
