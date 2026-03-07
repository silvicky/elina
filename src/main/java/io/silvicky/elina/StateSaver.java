package io.silvicky.elina;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.silvicky.elina.webmap.WebMapStorage;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class StateSaver extends SavedData
{
    public final HashMap<Identifier, HashMap<Identifier, HashSet<BlockPos> > > visitedStructure;
    public final HashMap<Identifier, WebMapStorage> webMapStorage;
    public static final Codec<StateSaver> CODEC= RecordCodecBuilder.create((instance)->
            instance.group
                    (
                            Codec.unboundedMap(Identifier.CODEC,Codec.unboundedMap(Identifier.CODEC,Codec.list(BlockPos.CODEC).xmap(HashSet::new, ArrayList::new)).xmap(HashMap::new, map->map)).xmap(HashMap::new, map->map).fieldOf("structure").orElse(new HashMap<>()).forGetter(stateSaver -> stateSaver.visitedStructure),
                            Codec.unboundedMap(Identifier.CODEC,WebMapStorage.CODEC).xmap(HashMap::new, map->map).fieldOf("map").orElse(new HashMap<>()).forGetter(stateSaver -> stateSaver.webMapStorage)
                    ).apply(instance,StateSaver::new));
    public StateSaver(HashMap<Identifier, HashMap<Identifier, HashSet<BlockPos> > > visitedStructure
            , HashMap<Identifier, WebMapStorage> webMapStorage)
    {
        this.visitedStructure=visitedStructure;
        this.webMapStorage = webMapStorage;
    }
    public StateSaver(){this(new HashMap<>(),new HashMap<>());}
    private static final SavedDataType<StateSaver> type = new SavedDataType<>(
            Elina.MOD_ID,
            StateSaver::new,
            CODEC,
            DataFixTypes.PLAYER
    );

    public static StateSaver getServerState(MinecraftServer server) {
        return getServerState(Objects.requireNonNull(server.getLevel(Level.OVERWORLD)));
    }
    //DO NOT USE THIS UNLESS DURING CONSTRUCTION OF OVERWORLD
    public static StateSaver getServerState(ServerLevel world) {
        DimensionDataStorage persistentStateManager = world.getDataStorage();
        StateSaver state = persistentStateManager.computeIfAbsent(type);
        state.setDirty();
        return state;
    }
}
