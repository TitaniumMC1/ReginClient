package com.oyvey.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DonutAntiEspBypass {
    private static boolean enabled = false;
    private static final ConcurrentHashMap<ChunkPos, byte[]> disguisedChunks = new ConcurrentHashMap<>();
    private static final List<BlockPos> revealedOres = new ArrayList<>();
    private static final List<BlockPos> revealedChests = new ArrayList<>();
    private static int spoofCooldown = 0;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void enable() {
        if (enabled) return;
        enabled = true;
        PacketInterceptor.registerChunkHandler(DonutAntiEspBypass::onChunkData);
        PacketInterceptor.registerMoveHandler(DonutAntiEspBypass::onPlayerMove);
        System.out.println("[OyVey] DonutAntiEspBypass enabled");
    }

    public static void disable() {
        enabled = false;
        PacketInterceptor.registerChunkHandler(null);
        PacketInterceptor.registerMoveHandler(null);
        revealedOres.clear();
        revealedChests.clear();
        disguisedChunks.clear();
    }

    private static void onChunkData(ChunkDataS2CPacket packet) {
        if (!enabled) return;
        ChunkPos pos = packet.getChunkPos();
        byte[] data = packet.getData(); // compressed block data (disguised version)
        disguisedChunks.put(pos, data);

        // Trigger double-load attack (spoof position to get real chunk data)
        if (spoofCooldown <= 0 && mc.player != null) {
            spoofPositionInsideChunk(pos);
            spoofCooldown = 20; // cooldown in ticks
        } else if (spoofCooldown > 0) {
            spoofCooldown--;
        }

        // Scan for hidden tile entities (chests, shulkers) that slipped through
        var blockEntities = packet.getBlockEntities();
        for (var nbt : blockEntities) {
            String id = nbt.getString("id");
            if (id.contains("chest") || id.contains("shulker") || nbt.contains("Items")) {
                int x = nbt.getInt("x");
                int y = nbt.getInt("y");
                int z = nbt.getInt("z");
                BlockPos chestPos = new BlockPos(x, y, z);
                if (!revealedChests.contains(chestPos)) {
                    revealedChests.add(chestPos);
                    if (mc.player != null) {
                        mc.player.sendMessage(Text.literal("§6[OyVey] §eChest found at §f" + x + " " + y + " " + z), false);
                    }
                }
            }
        }
    }

    private static void spoofPositionInsideChunk(ChunkPos pos) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        double centerX = pos.getStartX() + 8;
        double centerZ = pos.getStartZ() + 8;
        double fakeY = mc.player.getY();
        PlayerMoveC2SPacket.PositionAndOnGround fakePacket =
            new PlayerMoveC2SPacket.PositionAndOnGround(centerX, fakeY, centerZ, true);
        mc.getNetworkHandler().sendPacket(fakePacket);

        // After 2 ticks, compare disguised vs real chunk
        mc.execute(() -> {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            compareAndReveal(pos);
        });
    }

    private static void compareAndReveal(ChunkPos pos) {
        if (mc.world == null) return;
        byte[] disguised = disguisedChunks.get(pos);
        if (disguised == null) return;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = mc.world.getBottomY(); y <= mc.world.getTopY(); y++) {
                    BlockPos bp = new BlockPos(pos.getStartX() + x, y, pos.getStartZ() + z);
                    Block current = mc.world.getBlockState(bp).getBlock();
                    if (isValuableBlock(current)) {
                        if (!revealedOres.contains(bp)) {
                            revealedOres.add(bp);
                            if (mc.player != null) {
                                mc.player.sendMessage(Text.literal("§6[OyVey] §aOre revealed: §f" + bp.getX() + " " + bp.getY() + " " + bp.getZ()), false);
                            }
                        }
                    }
                }
            }
        }
        disguisedChunks.remove(pos);
    }

    private static boolean isValuableBlock(Block block) {
        return block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE ||
               block == Blocks.NETHERITE_ORE || block == Blocks.ANCIENT_DEBRIS ||
               block == Blocks.EMERALD_ORE || block == Blocks.GOLD_ORE ||
               block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST ||
               block == Blocks.SHULKER_BOX;
    }

    private static void onPlayerMove(PlayerMoveC2SPacket packet) {
        // Reserved for future advanced spoofing (e.g., modify packet on the fly)
    }

    // Call this in your render loop (e.g., RenderWorldLastEvent) to draw ESP boxes
    public static void onRender3D() {
        if (!enabled || mc.player == null) return;
        for (BlockPos pos : revealedOres) {
            drawBox(pos, 0xFFFF0000, "ORE");
        }
        for (BlockPos pos : revealedChests) {
            drawBox(pos, 0xFF00FF00, "CHEST");
        }
    }

    private static void drawBox(BlockPos pos, int color, String label) {
        // Use your client's existing render utilities.
        // Example with a simple box around the block:
        // RenderUtils.drawBlockBox(pos, color);
        // RenderUtils.drawText(label, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, color);
    }
}
