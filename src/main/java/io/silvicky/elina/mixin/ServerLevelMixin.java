package io.silvicky.elina.mixin;

import io.silvicky.elina.Elina;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.CustomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ServerLevel.class)
public class ServerLevelMixin
{
    @Inject(method = "<init>",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/LevelStem;generator()Lnet/minecraft/world/level/chunk/ChunkGenerator;",shift = At.Shift.AFTER))
    private void inject(MinecraftServer server, Executor workerExecutor, LevelStorageSource.LevelStorageAccess session, ServerLevelData properties, ResourceKey<Level> worldKey, LevelStem dimensionOptions, boolean debugWorld, long seed, List<CustomSpawner> spawners, boolean shouldTickTime, RandomSequences randomSequenceState, CallbackInfo ci)
    {
        if(Elina.server!=null)return;
        Elina.server=server;
    }
}
