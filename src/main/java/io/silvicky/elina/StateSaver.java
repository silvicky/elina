package io.silvicky.elina;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.silvicky.elina.webmap.WebMapStorage;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class StateSaver extends SavedData
{
    private static boolean checkMigrate=true;
    private static final Identifier id=Identifier.fromNamespaceAndPath("silvicky",Elina.MOD_ID);
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
            id,
            StateSaver::new,
            CODEC,
            DataFixTypes.PLAYER
    );
    private static void migrate(MinecraftServer server)
    {
        Path root= server.getDataStorage().dataFolder;
        Path oldPath=root.resolve("Elina.dat");
        Path newPath=id.withSuffix(".dat").resolveAgainst(root);
        newPath.getParent().toFile().mkdir();
        if(oldPath.toFile().exists())
        {
            try
            {
                Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            }
            catch(Exception e){throw new RuntimeException(e);}
        }
    }
    public static StateSaver getServerState(MinecraftServer server) {
        if(checkMigrate)
        {
            migrate(server);
            checkMigrate=false;
        }
        SavedDataStorage persistentStateManager = server.getDataStorage();
        StateSaver state = persistentStateManager.computeIfAbsent(type);
        state.setDirty();
        return state;
    }
}
