package io.silvicky.elina.webmap;

import io.silvicky.elina.Elina;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.saveddata.maps.MapIndex;

import java.awt.image.BufferedImage;

public class MapRender
{
    public static final int width=16;
    public static final int step=8;
    public static BufferedImage render(int id)
    {
        int maxId=Elina.server.overworld().getDataStorage().computeIfAbsent(MapIndex.TYPE).lastMapId;
        if(id<0||id>maxId)return null;
        MapItemSavedData mapState= Elina.server.overworld().getDataStorage().get(MapItemSavedData.type(new MapId(id)));
        if(mapState==null)return null;
        if(!mapState.locked)return null;
        BufferedImage ret=new BufferedImage(width,width,BufferedImage.TYPE_INT_RGB);
        for(int x=0;x<width;x++)
        {
            for(int y=0;y<width;y++)
            {
                ret.setRGB(x,y, MapColor.getColorFromPackedId(mapState.colors[x*step+y*step*128]));
                /*
                for(int x1=x*step;x1<x*step+step;x1++)
                {
                    for(int y1=y*step;y1<y*step+step;y1++)
                    {

                    }
                }*/
            }
        }
        return ret;
    }
}
