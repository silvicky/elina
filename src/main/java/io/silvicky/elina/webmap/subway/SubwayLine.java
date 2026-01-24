package io.silvicky.elina.webmap.subway;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedList;

public class SubwayLine
{
    public String label;
    public String icon;
    public int color;
    public boolean ring;
    public final LinkedList<String> stations;

    public static Codec<SubwayLine> CODEC= RecordCodecBuilder.create((instance)->
            instance.group
                    (
                            Codec.STRING.fieldOf("label").forGetter(point -> point.label),
                            Codec.STRING.fieldOf("icon").forGetter(point -> point.icon),
                            Codec.list(Codec.STRING).xmap(LinkedList::new,list->list).fieldOf("stations").forGetter(point -> point.stations),
                            Codec.INT.fieldOf("color").forGetter(point->point.color),
                            Codec.BOOL.fieldOf("ring").forGetter(point->point.ring)
                            ).apply(instance, SubwayLine::new));

    public SubwayLine(String label,String icon,LinkedList<String> stations,int color,boolean ring)
    {
        this.label=label;
        this.icon=icon;
        this.stations = stations;
        this.color=color;
        this.ring=ring;
    }
    public SubwayLine(){this("","",new LinkedList<>(),0,false);}
    public void insert(String newStation, String prev)
    {
        stations.remove(newStation);
        int index=stations.indexOf(prev);
        stations.add(index+1,newStation);
    }
    public void remove(String newStation)
    {
        stations.remove(newStation);
    }
}
