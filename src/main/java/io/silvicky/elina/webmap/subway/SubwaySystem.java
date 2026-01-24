package io.silvicky.elina.webmap.subway;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;

public class SubwaySystem
{
    public final HashMap<String,SubwayLine> lines;
    public final HashMap<String,SubwayStation> stationDetails;
    public String icon;
    public SubwaySystem(HashMap<String,SubwayLine> lines,HashMap<String,SubwayStation> stationDetails,String icon)
    {
        this.lines=lines;
        this.stationDetails=stationDetails;
        this.icon=icon;
    }
    public SubwaySystem()
    {
        this(new HashMap<>(),new HashMap<>(),"");
    }
    public static Codec<SubwaySystem> CODEC= RecordCodecBuilder.create((instance)->
            instance.group
                    (
                            Codec.unboundedMap(Codec.STRING,SubwayLine.CODEC).xmap(HashMap::new,map->map).fieldOf("lines").forGetter(point -> point.lines),
                            Codec.unboundedMap(Codec.STRING,SubwayStation.CODEC).xmap(HashMap::new,map->map).fieldOf("stations").forGetter(point -> point.stationDetails),
                            Codec.STRING.fieldOf("icon").forGetter(point -> point.icon)
                    ).apply(instance, SubwaySystem::new));
}
