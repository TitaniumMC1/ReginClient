package com.oyvey.utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FakeEntityFilter {
    private static ConcurrentHashMap<UUID, Integer> trustScores = new ConcurrentHashMap<>();

    public static boolean isFake(Entity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        UUID id = entity.getUuid();
        // DonutSMP fake players often have UUID starting with 00000000-
        if (id.toString().startsWith("00000000-")) return true;
        // No equipment changes? No movement? Low trust.
        int score = trustScores.getOrDefault(id, 0);
        if (score < -5) return true;
        return false;
    }

    public static void recordMovement(Entity entity) {
        UUID id = entity.getUuid();
        trustScores.merge(id, 1, Integer::sum);
    }
    public static void recordNoChange(Entity entity) {
        UUID id = entity.getUuid();
        trustScores.merge(id, -1, Integer::sum);
    }
}
