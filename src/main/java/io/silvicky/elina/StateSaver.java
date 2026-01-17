package io.silvicky.elina;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import static io.silvicky.elina.command.Locate.STRUCTURE;

public class StateSaver extends PersistentState {
    public final HashMap<Identifier, HashMap<Identifier, HashSet<BlockPos> > > visitedStructure;
    public static final Codec<StateSaver> CODEC= RecordCodecBuilder.create((instance)->
            instance.group
                    (
                            Codec.unboundedMap(Identifier.CODEC,Codec.unboundedMap(Identifier.CODEC,Codec.list(BlockPos.CODEC).xmap(HashSet::new, ArrayList::new)).xmap(HashMap::new, map->map)).xmap(HashMap::new, map->map).fieldOf(STRUCTURE).orElse(new HashMap<>()).forGetter(stateSaver -> stateSaver.visitedStructure)
                    ).apply(instance,StateSaver::new));
    public StateSaver(HashMap<Identifier, HashMap<Identifier, HashSet<BlockPos> > > visitedStructure){this.visitedStructure=visitedStructure;}
    public StateSaver(){this(new HashMap<>());}
    private static final PersistentStateType<StateSaver> type = new PersistentStateType<>(
            Elina.MOD_ID,
            StateSaver::new,
            CODEC,
            DataFixTypes.PLAYER
    );

    public static StateSaver getServerState(MinecraftServer server) {
        return getServerState(Objects.requireNonNull(server.getWorld(World.OVERWORLD)));
    }
    //DO NOT USE THIS UNLESS DURING CONSTRUCTION OF OVERWORLD
    public static StateSaver getServerState(ServerWorld world) {
        PersistentStateManager persistentStateManager = world.getPersistentStateManager();
        StateSaver state = persistentStateManager.getOrCreate(type);
        state.markDirty();
        return state;
    }
}
