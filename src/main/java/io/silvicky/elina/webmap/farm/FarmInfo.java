package io.silvicky.elina.webmap.farm;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;

import static java.lang.String.format;

public record FarmInfo(HashSet<Identifier> items, BlockPos pos, String label, String detail)
{
    public FarmInfo(BlockPos pos, String label, String detail)
    {
        this(new HashSet<>(),pos,label,detail);
    }
    @Override
    public String toString()
    {
        return format("%s(%s),%s",label,pos.toShortString(),detail);
    }
    public static Codec<FarmInfo> CODEC= RecordCodecBuilder.create((instance)->
            instance.group
                    (
                            Codec.list(Identifier.CODEC).xmap(HashSet::new,ArrayList::new).fieldOf("items").forGetter(info -> info.items),
                            BlockPos.CODEC.fieldOf("pos").forGetter(info -> info.pos),
                            Codec.STRING.fieldOf("label").forGetter(info -> info.label),
                            Codec.STRING.fieldOf("detail").forGetter(info -> info.detail)
                    ).apply(instance,FarmInfo::new));
}
