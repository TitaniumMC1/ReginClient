package reign.mixin;

import reign.utils.PacketInterceptor;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    
    @Inject(method = "onChunkData", at = @At("HEAD"), cancellable = true)
    private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        if (PacketInterceptor.onIncomingPacket(packet)) {
            ci.cancel();  // Optionally cancel if we want to replace or drop
        }
    }
    
    // Generic catch-all for any packet if needed
    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends Packet<?>> void onHandlePacket(T packet, CallbackInfo ci) {
        if (PacketInterceptor.onIncomingPacketGeneric(packet)) {
            ci.cancel();
        }
    }
}
