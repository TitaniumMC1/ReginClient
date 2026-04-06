package me.alpha432.oyvey.mixin.network;

import me.alpha432.oyvey.utils.PacketInterceptor;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "onChunkData", at = @At("HEAD"))
    private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        PacketInterceptor.onIncomingChunk(packet);
    }
}
