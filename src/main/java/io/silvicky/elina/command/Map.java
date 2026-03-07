package io.silvicky.elina.command;

import com.mojang.brigadier.Command;
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
import io.silvicky.elina.webmap.entities.Point;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static io.silvicky.elina.StateSaver.getServerState;
import static io.silvicky.elina.command.Locate.DIMENSION;
import static io.silvicky.elina.command.Locate.POS;
import static io.silvicky.elina.webmap.api.APIEntry.refresh;
import static java.lang.String.format;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Map
{
    public static final String LABEL = "label";
    public static final String DETAIL = "detail";
    public static final String ICON = "icon";
    public static final String ID = "id";
    public static final String SET = "set";
    public static final SimpleCommandExceptionType SET_NOT_FOUND=new SimpleCommandExceptionType(Component.literal("Set not found."));
    private static class SetSuggestionProvider implements SuggestionProvider<CommandSourceStack>
    {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException
        {
            ServerLevel world= DimensionArgument.getDimension(commandContext,DIMENSION);
            for (String id:getSetMap(world).keySet()) suggestionsBuilder.suggest(id);
            return suggestionsBuilder.buildFuture();
        }
    }
    private static class PointSuggestionProvider implements SuggestionProvider<CommandSourceStack>
    {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException
        {
            ServerLevel world= DimensionArgument.getDimension(commandContext,DIMENSION);
            String set=StringArgumentType.getString(commandContext,SET);
            HashMap<String,Point> pointMap=getPointMap(world,set);
            if(pointMap==null)return suggestionsBuilder.buildFuture();
            for (String id:pointMap.keySet()) suggestionsBuilder.suggest(id);
            return suggestionsBuilder.buildFuture();
        }
    }
    public static LiteralArgumentBuilder<CommandSourceStack> mapArgumentBuilder
            = literal("map")
            .then(literal("add")
                    .then(argument(DIMENSION,new DimensionArgument())
                            .then(argument(SET,StringArgumentType.string())
                                    .suggests(new SetSuggestionProvider())
                                    .then(argument(ID,StringArgumentType.string())
                                            .suggests(new PointSuggestionProvider())
                                            .then(argument(POS,new BlockPosArgument())
                                                    .then(argument(ICON, IntegerArgumentType.integer())
                                                            .then(argument(LABEL,StringArgumentType.string())
                                                                    .then(argument(DETAIL,StringArgumentType.string())
                                                                            .executes(ctx-> add(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,ID),StringArgumentType.getString(ctx,SET), BlockPosArgument.getBlockPos(ctx,POS),IntegerArgumentType.getInteger(ctx, ICON),StringArgumentType.getString(ctx,LABEL),StringArgumentType.getString(ctx,DETAIL)))))))))))
            .then(literal("del")
                    .then(argument(DIMENSION,new DimensionArgument())
                            .then(argument(SET,StringArgumentType.string())
                                    .suggests(new SetSuggestionProvider())
                                    .then(argument(ID,StringArgumentType.string())
                                            .suggests(new PointSuggestionProvider())
                                            .executes(ctx-> delete(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,ID),StringArgumentType.getString(ctx,SET)))))))
            .then(literal("list")
                    .then(argument(DIMENSION,new DimensionArgument())
                            .then(argument(SET,StringArgumentType.string())
                                    .suggests(new SetSuggestionProvider())
                                    .executes(ctx->list(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,SET))))))
            .then(literal("addset")
                    .then(argument(DIMENSION,new DimensionArgument())
                            .then(argument(SET,StringArgumentType.string())
                                    .suggests(new SetSuggestionProvider())
                                    .then(argument(LABEL,StringArgumentType.string())
                                            .executes(ctx->addSet(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,SET),StringArgumentType.getString(ctx,LABEL)))))))
            .then(literal("delset")
                    .then(argument(DIMENSION,new DimensionArgument())
                            .then(argument(SET,StringArgumentType.string())
                                    .suggests(new SetSuggestionProvider())
                                    .executes(ctx->deleteSet(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION),StringArgumentType.getString(ctx,SET))))))
            .then(literal("listset")
                    .then(argument(DIMENSION,new DimensionArgument())
                            .executes(ctx->listSet(ctx.getSource(), DimensionArgument.getDimension(ctx,DIMENSION)))));
    public static HashMap<String, Point> getPointMap(ServerLevel world, String set)
    {
        if(!getSetMap(world).containsKey(set))return null;
        return getServerState(world.getServer()).webMapStorage
                .computeIfAbsent(world.dimension().identifier(), i->new WebMapStorage())
                .points()
                .computeIfAbsent(set,i->new HashMap<>());
    }
    public static HashMap<String, String> getSetMap(ServerLevel world)
    {
        return getServerState(world.getServer()).webMapStorage
                .computeIfAbsent(world.dimension().identifier(), i->new WebMapStorage())
                .sets();
    }
    private static int add(CommandSourceStack source, ServerLevel world, String id, String set, BlockPos pos, int icon, String label, String detail) throws CommandSyntaxException
    {
        HashMap<String,Point> pointMap=getPointMap(world,set);
        if(pointMap==null)throw SET_NOT_FOUND.create();
        pointMap.put(id,new Point(pos,label,detail,icon));
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int delete(CommandSourceStack source, ServerLevel world, String id, String set) throws CommandSyntaxException
    {
        HashMap<String,Point> pointMap=getPointMap(world,set);
        if(pointMap==null)throw SET_NOT_FOUND.create();
        pointMap.remove(id);
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int list(CommandSourceStack source, ServerLevel world, String set) throws CommandSyntaxException
    {
        HashMap<String,Point> pointMap=getPointMap(world,set);
        if(pointMap==null)throw SET_NOT_FOUND.create();
        for(java.util.Map.Entry<String,Point> entry: pointMap.entrySet())
        {
            source.sendSuccess(()-> Component.literal(format("%s: %s",entry.getKey(),entry.getValue())),false);
        }
        source.sendSuccess(()-> Component.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
    private static int addSet(CommandSourceStack source, ServerLevel world, String set, String label)
    {
        getSetMap(world).put(set,label);
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int deleteSet(CommandSourceStack source, ServerLevel world, String set)
    {
        getSetMap(world).remove(set);
        source.sendSuccess(()-> Component.literal("Done."),false);
        refresh();
        return Command.SINGLE_SUCCESS;
    }
    private static int listSet(CommandSourceStack source, ServerLevel world)
    {
        for(java.util.Map.Entry<String, String> entry: getSetMap(world).entrySet())
        {
            source.sendSuccess(()-> Component.literal(format("%s: %s",entry.getKey(),entry.getValue())),false);
        }
        source.sendSuccess(()-> Component.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }
}
