package io.silvicky.elina.webmap.subway;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;

public record SubwayStation(BlockPos pos, String label, String detail)
{
    public static Codec<SubwayStation> CODEC= RecordCodecBuilder.create((instance)->
            instance.group
                    (
                            BlockPos.CODEC.fieldOf("pos").forGetter(point -> point.pos),
                            Codec.STRING.fieldOf("label").forGetter(point -> point.label),
                            Codec.STRING.fieldOf("detail").forGetter(point -> point.detail)
                    ).apply(instance,SubwayStation::new));
}
