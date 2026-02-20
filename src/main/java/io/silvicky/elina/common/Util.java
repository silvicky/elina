package io.silvicky.elina.common;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Collection;

public class Util
{
    public static <T> String collectionToString(Collection<T> list)
    {
        StringBuilder tot= new StringBuilder();
        boolean first=true;
        for(T t:list)
        {
            if(!first)tot.append(", ");
            tot.append(t.toString());
        }
        return tot.toString();
    }
    public static final String OVERWORLD="overworld";
    public static final String NETHER="the_nether";
    public static final String END="the_end";
    public enum DimensionType
    {
        OVERWORLD_TYPE(OVERWORLD),
        NETHER_TYPE(NETHER),
        END_TYPE(END),
        SINGLET("");
        public final String name;
        DimensionType(String name){this.name=name;}
        public static DimensionType getDimensionType(String id)
        {
            if(id.endsWith(NETHER))return NETHER_TYPE;
            if(id.endsWith(END))return END_TYPE;
            if(id.endsWith(OVERWORLD))return OVERWORLD_TYPE;
            return SINGLET;
        }
        public static DimensionType getDimensionType(ServerWorld world)
        {
            return getDimensionType(world.getRegistryKey().getValue().getPath());
        }
    }
    public static Identifier getDimensionId(ServerWorld world, DimensionType type)
    {
        return getDimensionId(world.getRegistryKey().getValue(),type);
    }

    public static String getDimensionId(String id, DimensionType type)
    {
        if(id.endsWith(NETHER))id=id.substring(0,id.length()- NETHER.length())+ type.name;
        if(id.endsWith(END))id=id.substring(0,id.length()- END.length())+ type.name;
        return id;
    }
    public static Identifier getDimensionId(Identifier id, DimensionType type)
    {
        return Identifier.of(id.getNamespace(),getDimensionId(id.getPath(),type));
    }
}
