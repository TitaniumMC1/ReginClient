package me.alpha432.oyvey.utils;

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
    public static boolean onOutgoingMove(PlayerMoveC2SPacket packet) {
        if (moveHandler != null) moveHandler.accept(packet);
        return false;
    }
}
