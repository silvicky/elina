package io.silvicky.elina.webmap.subway;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;

import static java.lang.String.format;

public record SubwayStation(BlockPos pos, String label, String detail)
{
    @Override
    public String toString()
    {
        return format("%s(%s),%s",label,pos.toShortString(),detail);
    }
    public static Codec<SubwayStation> CODEC= RecordCodecBuilder.create((instance)->
            instance.group
                    (
                            BlockPos.CODEC.fieldOf("pos").forGetter(point -> point.pos),
                            Codec.STRING.fieldOf("label").forGetter(point -> point.label),
                            Codec.STRING.fieldOf("detail").forGetter(point -> point.detail)
                    ).apply(instance,SubwayStation::new));
}
