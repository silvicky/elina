package io.silvicky.elina.webmap.entities;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;

public record Point(BlockPos pos, String label, String detail, int icon) implements WebMapEntity
{
    public static Codec<Point> CODEC= RecordCodecBuilder.create((instance)->
            instance.group
                    (
                            BlockPos.CODEC.fieldOf("pos").forGetter(point -> point.pos),
                            Codec.STRING.fieldOf("label").forGetter(point -> point.label),
                            Codec.STRING.fieldOf("detail").forGetter(point -> point.detail),
                            Codec.INT.fieldOf("icon").forGetter(point -> point.icon)
                    ).apply(instance, Point::new));
}
