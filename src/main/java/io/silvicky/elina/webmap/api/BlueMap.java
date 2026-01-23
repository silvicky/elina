package io.silvicky.elina.webmap.api;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import io.silvicky.elina.webmap.WebMapStorage;
import io.silvicky.elina.webmap.entities.Point;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.silvicky.elina.Elina.server;
import static io.silvicky.elina.StateSaver.getServerState;

public class BlueMap
{
    public static void register()
    {
        BlueMapAPI.onEnable(api-> refresh());
    }
    private static POIMarker fromPoint(Point point)
    {
        return POIMarker.builder()
                .position(Vector3d.from(point.pos().getX(),point.pos().getY(),point.pos().getZ()))
                .label(point.label())
                .detail(point.detail())
                //.icon(point.icon(), 16,16)
                .build();
    }
    public static void refresh()
    {
        Optional<BlueMapAPI> apiOpt= BlueMapAPI.getInstance();
        if(apiOpt.isEmpty())return;
        BlueMapAPI api= apiOpt.get();
        for(Map.Entry<Identifier, WebMapStorage> entry:getServerState(server).webMapStorage.entrySet())
        {
            ServerWorld world=server.getWorld(RegistryKey.of(RegistryKeys.WORLD,entry.getKey()));
            WebMapStorage storage=entry.getValue();
            Map<String, MarkerSet> markerSets=new HashMap<>();
            for(Map.Entry<String,String> entry1:storage.sets().entrySet())
            {
                if(!markerSets.containsKey(entry1.getKey()))markerSets.put(entry1.getKey(), new MarkerSet(entry1.getValue()));
            }
            for(Map.Entry<String, HashMap<String, Point>> entry1:storage.points().entrySet())
            {
                if(!markerSets.containsKey(entry1.getKey()))markerSets.put(entry1.getKey(), new MarkerSet(entry1.getKey()));
                MarkerSet markerSet=markerSets.get(entry1.getKey());
                for(Map.Entry<String,Point> entry2:entry1.getValue().entrySet())
                {
                    markerSet.put(entry2.getKey(), fromPoint(entry2.getValue()));
                }
            }
            api.getWorld(world).ifPresent(blueMapWorld ->
            {
                for(BlueMapMap map: blueMapWorld.getMaps())
                {
                    map.getMarkerSets().clear();
                    map.getMarkerSets().putAll(markerSets);
                }
            });
        }
    }

}
