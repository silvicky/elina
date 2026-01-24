package io.silvicky.elina.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static io.silvicky.elina.command.Locate.locateArgumentBuilder;
import static io.silvicky.elina.command.Map.mapArgumentBuilder;
import static io.silvicky.elina.command.Subway.subwayArgumentBuilder;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandRoot
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        dispatcher.register(
                literal("elina")
                        .requires(source -> source.getPermissions().hasPermission(new Permission.Level(PermissionLevel.ALL)))
                        .executes(context->help(context.getSource()))
                        .then(locateArgumentBuilder)
                        .then(mapArgumentBuilder)
                        .then(subwayArgumentBuilder));
    }
    private static int help(ServerCommandSource source)
    {
        source.sendFeedback(()-> Text.literal("Usage: /elina <command> ..."),false);
        return Command.SINGLE_SUCCESS;
    }
}
