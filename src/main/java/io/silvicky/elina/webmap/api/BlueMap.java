package io.silvicky.elina.webmap.api;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.LineMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Line;
import io.silvicky.elina.webmap.WebMapStorage;
import io.silvicky.elina.webmap.entities.Point;
import io.silvicky.elina.webmap.subway.SubwayLine;
import io.silvicky.elina.webmap.subway.SubwayStation;
import io.silvicky.elina.webmap.subway.SubwaySystem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;

import static io.silvicky.elina.Elina.server;
import static io.silvicky.elina.StateSaver.getServerState;
import static java.lang.String.format;

public class BlueMap
{
    public static void register()
    {
        BlueMapAPI.onEnable(api-> refresh());
    }
    private static POIMarker fromPoint(Point point)
    {
        return POIMarker.builder()
                .position(fromBlockPos(point.pos()))
                .label(point.label())
                .detail(point.detail())
                //.icon(point.icon(), 16,16)
                .build();
    }
    public static Vector3d fromBlockPos(BlockPos pos)
    {
        return Vector3d.from(pos.getX(), pos.getY(), pos.getZ());
    }
    private static LineMarker fromBlockPoss(BlockPos a,BlockPos b,int color)
    {
        return LineMarker.builder()
                .label("")
                .lineColor(new Color(color))
                .line(new Line(fromBlockPos(a),fromBlockPos(b)))
                .build();
    }
    private static void renderSubway(MarkerSet markerSet, SubwaySystem subwaySystem)
    {
        Map<String, List<String>> usage=new HashMap<>();
        for(Map.Entry<String,SubwayLine> entry:subwaySystem.lines.entrySet())
        {
            for(String station:entry.getValue().stations)
            {
                usage.putIfAbsent(station,new ArrayList<>());
                usage.get(station).add(entry.getKey());
            }
        }
        for(Map.Entry<String, SubwayStation> entry:subwaySystem.stationDetails.entrySet())
        {
            int lineCount=usage.getOrDefault(entry.getKey(),new ArrayList<>()).size();
            POIMarker marker=POIMarker.builder()
                    .position(fromBlockPos(entry.getValue().pos()))
                    .label(entry.getValue().label())
                    .detail(entry.getValue().detail())
                    .build();
            /*if(lineCount==0)
            {
                marker.setIcon("",16,16);
            }
            else if(lineCount==1)
            {
                marker.setIcon(usage.get(entry.getKey()).getFirst(),16,16);
            }
            else
            {
                marker.setIcon(subwaySystem.icon, 16,16);
            }*/
            markerSet.put(entry.getKey(), marker);
        }
        int segmentCount=0;
        String segmentFormat="segment_%s";
        for(SubwayLine line:subwaySystem.lines.values())
        {
            if(line.stations.isEmpty())continue;
            Iterator<String> iterator=line.stations.iterator();
            String first=iterator.next();
            String last=first;
            while(iterator.hasNext())
            {
                String cur=iterator.next();
                markerSet.put(format(segmentFormat,segmentCount++),fromBlockPoss(subwaySystem.stationDetails.get(last).pos(),subwaySystem.stationDetails.get(cur).pos(), line.color));
                last=cur;
            }
            if(line.ring)markerSet.put(format(segmentFormat,segmentCount++),fromBlockPoss(subwaySystem.stationDetails.get(last).pos(),subwaySystem.stationDetails.get(first).pos(), line.color));
        }
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
            MarkerSet subway=new MarkerSet("Subway");
            markerSets.put("subway",subway);
            renderSubway(subway,storage.subwaySystem());
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
