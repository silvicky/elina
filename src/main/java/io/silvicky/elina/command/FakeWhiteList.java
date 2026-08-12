package io.silvicky.elina.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.HashSet;

import static io.silvicky.elina.StateSaver.getServerState;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class FakeWhiteList
{
    public static final String LIST = "list";
    public static final String KEY = "key";
    public static final String PATTERN = "pattern";
    public static final String COMMAND = "command";
    public static LiteralArgumentBuilder<CommandSourceStack> fwlArgumentBuilder
            = literal("fwl")
            .then(literal("add")
                    .then(argument(LIST, StringArgumentType.word())
                            .then(argument(KEY, StringArgumentType.word())
                                    .executes(ctx->add(ctx.getSource(), StringArgumentType.getString(ctx,LIST), StringArgumentType.getString(ctx,KEY))))))
            .then(literal("remove")
                    .then(argument(LIST, StringArgumentType.word())
                            .then(argument(KEY, StringArgumentType.word())
                                    .executes(ctx->remove(ctx.getSource(), StringArgumentType.getString(ctx,LIST), StringArgumentType.getString(ctx,KEY))))))
            .then(literal("list")
                    .then(argument(LIST, StringArgumentType.word())
                            .executes(ctx->list(ctx.getSource(), StringArgumentType.getString(ctx,LIST)))))
            .then(literal("do")
                    .then(argument(LIST, StringArgumentType.word())
                            .then(argument(PATTERN, StringArgumentType.string())
                                    .then(argument(COMMAND, StringArgumentType.greedyString())
                                            .executes(ctx->doIt(ctx.getSource(),StringArgumentType.getString(ctx,LIST),StringArgumentType.getString(ctx,PATTERN),StringArgumentType.getString(ctx,COMMAND)))))));
    private static int add(CommandSourceStack source, String list, String key)
    {
        getServerState(source.getServer()).fwlKeys
                .computeIfAbsent(list,_->new HashSet<>()).add(key);
        source.sendSuccess(()-> Component.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }

    private static int remove(CommandSourceStack source, String list, String key)
    {
        getServerState(source.getServer()).fwlKeys
                .computeIfAbsent(list, _ -> new HashSet<>()).remove(key);
        source.sendSuccess(() -> Component.literal("Done."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int list(CommandSourceStack source, String list)
    {
        HashSet<String> keySet=getServerState(source.getServer()).fwlKeys.getOrDefault(list,new HashSet<>());
        StringBuilder stringBuilder=new StringBuilder();
        boolean first=true;
        for(String k:keySet)
        {
            if(!first)stringBuilder.append(", ");
            first=false;
            stringBuilder.append(k);
        }
        source.sendSuccess(()-> Component.literal(stringBuilder.toString()),false);
        source.sendSuccess(()-> Component.literal("Done."),false);
        return Command.SINGLE_SUCCESS;
    }

    private static int doIt(CommandSourceStack source, String list, String pattern, String command)
    {
        HashSet<String> keySet= getServerState(source.getServer()).fwlKeys.getOrDefault(list, new HashSet<>());
        StringBuilder stringBuilder=new StringBuilder();
        for(String k:keySet)stringBuilder.append(pattern.replaceAll("%",k));
        String finalCommand=command.replaceAll("%", stringBuilder.toString());
        source.getServer().getCommands().performPrefixedCommand(source, finalCommand);
        source.sendSuccess(() -> Component.literal("Done."), false);
        return Command.SINGLE_SUCCESS;
    }
}
