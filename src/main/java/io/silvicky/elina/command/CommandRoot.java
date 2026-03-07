package io.silvicky.elina.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static io.silvicky.elina.command.Farm.farmArgumentBuilder;
import static io.silvicky.elina.command.Locate.locateArgumentBuilder;
import static io.silvicky.elina.command.Map.mapArgumentBuilder;
import static io.silvicky.elina.command.Subway.subwayArgumentBuilder;
import static net.minecraft.commands.Commands.literal;

public class CommandRoot
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
                literal("elina")
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.ALL)))
                        .executes(context->help(context.getSource()))
                        .then(locateArgumentBuilder)
                        .then(mapArgumentBuilder)
                        .then(subwayArgumentBuilder)
                        .then(farmArgumentBuilder));
    }
    private static int help(CommandSourceStack source)
    {
        source.sendSuccess(()-> Component.literal("Usage: /elina <command> ..."),false);
        return Command.SINGLE_SUCCESS;
    }
}
