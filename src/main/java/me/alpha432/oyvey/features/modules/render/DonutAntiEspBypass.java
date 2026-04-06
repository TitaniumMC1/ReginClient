package me.alpha432.oyvey.module.impl.donut;

import me.alpha432.oyvey.OyVey; // your main class
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DonutAntiEspBypass {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static final ConcurrentHashMap<ChunkPos, byte[]> disguisedChunks = new ConcurrentHashMap<>();
    private static final List<BlockPos> revealedOres = new ArrayList<>();
    private static final List<BlockPos> revealedChests = new ArrayList<>();
    private static int spoofCooldown = 0;

    public static void enable() {
        enabled = true;
        OyVey.LOGGER.info("[OyVey] DonutAntiEspBypass enabled");
    }

    public static void disable() {
        enabled = false;
        disguisedChunks.clear();
        revealedOres.clear();
        revealedChests.clear();
    }

    public static void onChunkData(ChunkDataS2CPacket packet) {
        if (!enabled || mc.player == null) return;

        ChunkPos pos = packet.getChunkPos();
        disguisedChunks.put(pos, packet.getData());

        // Trigger double-load attack
        if (spoofCooldown <= 0) {
            spoofPositionInsideChunk(pos);
            spoofCooldown = 20;
        } else {
            spoofCooldown--;
        }

        // Scan for hidden tile entities (chests/shulkers)
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
                    if (mc.player != null)
                        mc.player.sendMessage(Text.literal("§6[OyVey] §eChest found at §f" + x + " " + y + " " + z), false);
                }
            }
        }
    }

    private static void spoofPositionInsideChunk(ChunkPos pos) {
        if (mc.getNetworkHandler() == null) return;
        double centerX = pos.getStartX() + 8;
        double centerZ = pos.getStartZ() + 8;
        double fakeY = mc.player.getY();
        PlayerMoveC2SPacket.PositionAndOnGround fakePacket =
            new PlayerMoveC2SPacket.PositionAndOnGround(centerX, fakeY, centerZ, true);
        mc.getNetworkHandler().sendPacket(fakePacket);

        // Compare after 2 ticks
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
                            if (mc.player != null)
                                mc.player.sendMessage(Text.literal("§6[OyVey] §aOre revealed: §f" + bp.getX() + " " + bp.getY() + " " + bp.getZ()), false);
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

    public static boolean onOutgoingPacket(Packet<?> packet) {
        // You can modify movement packets here if needed, but for now just observe
        return false;
    }

    // Call this from your render event (e.g., Render3DEvent)
    public static void onRender3D() {
        if (!enabled || mc.player == null) return;
        // Draw ESP boxes for revealedOres and revealedChests
        // Use your client's existing ESP renderer (e.g., RenderUtils.drawBox)
        for (BlockPos pos : revealedOres) {
            // drawBox(pos, 0xFFFF0000);
        }
        for (BlockPos pos : revealedChests) {
            // drawBox(pos, 0xFF00FF00);
        }
    }
}
