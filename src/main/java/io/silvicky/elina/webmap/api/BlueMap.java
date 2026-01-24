package io.silvicky.elina.webmap.api;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.LineMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Line;
import io.silvicky.elina.webmap.MapRender;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
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
    private static POIMarker fromPoint(BlueMapAPI api,Point point)
    {
        String icon=getImagePath(api,point.icon());
        return POIMarker.builder()
                .position(fromBlockPos(point.pos()))
                .label(point.label())
                .detail(point.detail())
                .icon(icon, MapRender.width/2,MapRender.width/2)
                .build();
    }
    private static String getImagePath(BlueMapAPI api,int id)
    {
        String ret=format("elina/map_%d.png",id);
        Path cur=api.getWebApp().getWebRoot().resolve(ret);
        cur.getParent().toFile().mkdirs();
        if(cur.toFile().exists())return ret;
        BufferedImage image=MapRender.render(id);
        if(image==null)return "assets/poi.svg";
        try
        {
            ImageIO.write(image,"png",cur.toFile());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return ret;
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
    private static void renderSubway(BlueMapAPI api,MarkerSet markerSet, SubwaySystem subwaySystem)
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
            int iconId;
            StringBuilder detail=new StringBuilder(entry.getValue().detail());
            if(lineCount==0)
            {
                iconId=-1;
            }
            else if(lineCount==1)
            {
                iconId=subwaySystem.lines.get(usage.get(entry.getKey()).getFirst()).icon;
                detail.append('\n');
                detail.append(subwaySystem.lines.get(usage.get(entry.getKey()).getFirst()).label);
            }
            else
            {
                iconId=subwaySystem.icon;
                detail.append('\n');
                Iterator<String> iterator=usage.get(entry.getKey()).iterator();
                detail.append(subwaySystem.lines.get(iterator.next()).label);
                while(iterator.hasNext())
                {
                    detail.append(", ");
                    detail.append(subwaySystem.lines.get(iterator.next()).label);
                }
            }
            String icon=getImagePath(api,iconId);
            POIMarker marker=POIMarker.builder()
                    .position(fromBlockPos(entry.getValue().pos()))
                    .label(entry.getValue().label())
                    .detail(detail.toString())
                    .icon(icon, MapRender.width/2,MapRender.width/2)
                    .build();
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
            renderSubway(api,subway,storage.subwaySystem());
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
                    markerSet.put(entry2.getKey(), fromPoint(api,entry2.getValue()));
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
