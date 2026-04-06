package com.oyvey.utils;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import java.util.function.Consumer;

public class PacketInterceptor {
    private static Consumer<ChunkDataS2CPacket> chunkHandler = null;
    private static Consumer<PlayerMoveC2SPacket> moveHandler = null;

    public static void registerChunkHandler(Consumer<ChunkDataS2CPacket> handler) {
        chunkHandler = handler;
    }
    public static void registerMoveHandler(Consumer<PlayerMoveC2SPacket> handler) {
        moveHandler = handler;
    }
    public static void onIncomingChunk(ChunkDataS2CPacket packet) {
        if (chunkHandler != null) chunkHandler.accept(packet);
    }
    public static boolean onOutgoingPacket(Packet<?> packet) {
        if (packet instanceof PlayerMoveC2SPacket && moveHandler != null) {
            moveHandler.accept((PlayerMoveC2SPacket) packet);
        }
        return false; // never cancel by default
    }
}
