package io.silvicky.elina.webmap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.silvicky.elina.webmap.entities.Point;
import io.silvicky.elina.webmap.farm.FarmInfo;
import io.silvicky.elina.webmap.subway.SubwaySystem;

import java.util.HashMap;

public record WebMapStorage(HashMap<String, HashMap<String, Point>> points,
                            HashMap<String, String> sets,
                            SubwaySystem subwaySystem,
                            HashMap<String, FarmInfo> farms)
{
    public static Codec<WebMapStorage> CODEC= RecordCodecBuilder.create((instance)->
            instance.group
                    (
                            Codec.unboundedMap(Codec.STRING,Codec.unboundedMap(Codec.STRING,Point.CODEC).xmap(HashMap::new, map->map)).xmap(HashMap::new, map->map).fieldOf("points").orElse(new HashMap<>()).forGetter(webMapStorage -> webMapStorage.points),
                            Codec.unboundedMap(Codec.STRING,Codec.STRING).xmap(HashMap::new, map->map).fieldOf("sets").orElse(new HashMap<>()).forGetter(webMapStorage -> webMapStorage.sets),
                            SubwaySystem.CODEC.fieldOf("subway").forGetter(webMapStorage -> webMapStorage.subwaySystem),
                            Codec.unboundedMap(Codec.STRING,FarmInfo.CODEC).xmap(HashMap::new, map->map).fieldOf("farms").orElse(new HashMap<>()).forGetter(webMapStorage -> webMapStorage.farms)
                    ).apply(instance,WebMapStorage::new));
    public WebMapStorage(){this(new HashMap<>(),new HashMap<>(),new SubwaySystem(),new HashMap<>());}
}
