package com.chenweikeng.imf.skincache.prewarm;

import com.chenweikeng.imf.skincache.SkinCacheMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scans a loaded chunk for player head block entities and extracts their
 * owner profiles. Used by the pre-warming system to kick off skin loading
 * before the renderer first asks for them.
 */
public final class ChunkHeadScanner {

    private ChunkHeadScanner() {}

    /**
     * Returns all ResolvableProfiles from player skull block entities in this chunk.
     */
    public static List<ResolvableProfile> findPlayerHeadProfiles(LevelChunk chunk) {
        List<ResolvableProfile> profiles = new ArrayList<>();
        Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();

        for (BlockEntity be : blockEntities.values()) {
            if (be instanceof SkullBlockEntity skull) {
                ResolvableProfile owner = skull.getOwnerProfile();
                if (owner != null) {
                    profiles.add(owner);
                }
            }
        }

        return profiles;
    }
}
