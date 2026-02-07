package io.silvicky.elina.webmap.subway;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedList;

import static java.lang.String.format;

public class SubwayLine
{
    public String label;
    public int icon;
    public int color;
    public boolean ring;
    public final LinkedList<String> stations;
    @Override
    public String toString()
    {
        StringBuilder stringBuilder=new StringBuilder(format("%s, icon=%d, color=%s",label,icon,Integer.toString(color,16).toUpperCase()));
        if(ring)stringBuilder.append(", ring");
        return stringBuilder.toString();
    }

    public static Codec<SubwayLine> CODEC= RecordCodecBuilder.create((instance)->
            instance.group
                    (
                            Codec.STRING.fieldOf("label").forGetter(point -> point.label),
                            Codec.INT.fieldOf("icon").forGetter(point -> point.icon),
                            Codec.list(Codec.STRING).xmap(LinkedList::new,list->list).fieldOf("stations").forGetter(point -> point.stations),
                            Codec.INT.fieldOf("color").forGetter(point->point.color),
                            Codec.BOOL.fieldOf("ring").forGetter(point->point.ring)
                            ).apply(instance, SubwayLine::new));

    public SubwayLine(String label,int icon,LinkedList<String> stations,int color,boolean ring)
    {
        this.label=label;
        this.icon=icon;
        this.stations = stations;
        this.color=color;
        this.ring=ring;
    }
    public SubwayLine(){this("",-1,new LinkedList<>(),0,false);}
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
