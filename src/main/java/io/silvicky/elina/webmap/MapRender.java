package io.silvicky.elina.webmap;

import io.silvicky.elina.Elina;
import net.minecraft.block.MapColor;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.world.IdCountsState;

import java.awt.image.BufferedImage;

public class MapRender
{
    public static final int width=16;
    public static final int step=8;
    public static BufferedImage render(int id)
    {
        int maxId=Elina.server.getOverworld().getPersistentStateManager().getOrCreate(IdCountsState.STATE_TYPE).map;
        if(id<0||id>maxId)return null;
        MapState mapState= Elina.server.getOverworld().getPersistentStateManager().get(MapState.createStateType(new MapIdComponent(id)));
        if(mapState==null)return null;
        if(!mapState.locked)return null;
        BufferedImage ret=new BufferedImage(width,width,BufferedImage.TYPE_INT_RGB);
        for(int x=0;x<width;x++)
        {
            for(int y=0;y<width;y++)
            {
                ret.setRGB(x,y, MapColor.getRenderColor(mapState.colors[x*step+y*step*128]));
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
