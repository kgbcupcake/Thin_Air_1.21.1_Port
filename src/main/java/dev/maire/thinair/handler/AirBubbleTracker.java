package dev.maire.thinair.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.maire.thinair.ThinAir;
import dev.maire.thinair.ThinAirMod;
import dev.maire.thinair.api.AirQualityHelper;
import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.capability.AirBubblePositionsCapability;
import dev.maire.thinair.init.ModCapabilities;
import dev.maire.thinair.network.ClientboundChunkAirQualityPacket;
import dev.maire.thinair.world.level.block.SafetyLanternBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class AirBubbleTracker {
    private static final Set<ChunkPos> CHUNKS_TO_SCAN = Collections.synchronizedSet(Sets.newHashSet());
    private static final Set<ChunkPos> DIRTY_CHUNKS = Collections.synchronizedSet(Sets.newHashSet());
    private static final List<Map.Entry<ChunkPos, BlockPos>> CHUNK_SCANNING_PROGRESS =
            Collections.synchronizedList(Lists.newLinkedList());

    public static void onBlockStateChange(ServerLevel level, BlockPos pos, BlockState oldBlockState, BlockState newBlockState) {
        ChunkPos chunkPos = new ChunkPos(pos);
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
        AirQualityLevel oldAirQuality = AirQualityLevel.getAirQualityFromBlock(oldBlockState);
        AirQualityLevel newAirQuality = AirQualityLevel.getAirQualityFromBlock(newBlockState);
        if (chunk != null && (!oldBlockState.is(newBlockState.getBlock()) || oldAirQuality != newAirQuality)) {
            AirBubblePositionsCapability capability = ModCapabilities.get(chunk);
            if (capability != null) {
            if (oldAirQuality != null) {
                AirQualityLevel removed = capability.getAirBubblePositions().remove(pos);
                if (removed == null) {
                    ThinAir.LOGGER.debug("Didn't remove any air bubbles at {}", pos);
                } else {
                    chunk.setUnsaved(true);
                    ThinAirMod.sendToAllTracking(chunk, new ClientboundChunkAirQualityPacket(
                            chunk.getPos(), Map.of(pos, removed), ClientboundChunkAirQualityPacket.Mode.REMOVE));
                }
            }

            if (newAirQuality != null) {
                AirQualityLevel clobbered = capability.getAirBubblePositions().put(pos, newAirQuality);
                if (clobbered != null) {
                    ThinAir.LOGGER.debug("Clobbered air bubble at {}: {}", pos, clobbered);
                }
                chunk.setUnsaved(true);
                ThinAirMod.sendToAllTracking(chunk, new ClientboundChunkAirQualityPacket(
                        chunk.getPos(), Map.of(pos, newAirQuality), ClientboundChunkAirQualityPacket.Mode.ADD));
            }
            }
        }
        DIRTY_CHUNKS.add(new ChunkPos(pos));
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        if (CHUNKS_TO_SCAN.add(chunk.getPos())) {
            CHUNK_SCANNING_PROGRESS.add(Map.entry(chunk.getPos(), getChunkStartingPosition(chunk)));
        }
    }

    public static void onChunkUnload(ServerLevel level, LevelChunk chunk) {
        CHUNKS_TO_SCAN.remove(chunk.getPos());
    }

    public static void onChunkWatch(ServerPlayer player, LevelChunk chunk, ServerLevel level) {
        AirBubblePositionsCapability capability = ModCapabilities.get(chunk);
        if (capability == null) {
            return;
        }
        Map<BlockPos, AirQualityLevel> airBubblePositions = capability.getAirBubblePositionsView();
        ThinAirMod.sendToPlayer(player, new ClientboundChunkAirQualityPacket(
                chunk.getPos(), airBubblePositions, ClientboundChunkAirQualityPacket.Mode.REPLACE));
    }

    public static void onLevelUnload(MinecraftServer server, LevelAccessor level) {
        CHUNKS_TO_SCAN.clear();
        CHUNK_SCANNING_PROGRESS.clear();
    }

    public static void onEndLevelTick(MinecraftServer server, ServerLevel level) {
        try {
        if (!CHUNK_SCANNING_PROGRESS.isEmpty()) {
            if (!level.players().isEmpty() && server.getTickCount() % 200 == 0) {
                CHUNK_SCANNING_PROGRESS.sort(Comparator.comparingInt(entry -> {
                    BlockPos worldPosition = entry.getKey().getWorldPosition();
                    return level.players().stream()
                            .map(player -> player.chunkPosition().getWorldPosition())
                            .mapToInt(playerPos -> (int) playerPos.distSqr(worldPosition))
                            .min()
                            .orElse(0);
                }));
            }
            ListIterator<Map.Entry<ChunkPos, BlockPos>> iterator = CHUNK_SCANNING_PROGRESS.listIterator();
            Map.Entry<ChunkPos, BlockPos> entry = iterator.next();
            ChunkPos chunkPos = entry.getKey();
            if (CHUNKS_TO_SCAN.contains(chunkPos)) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
                if (chunk != null) {
                    AirBubblePositionsCapability capability = ModCapabilities.get(chunk);
                    if (capability == null) {
                        return;
                    }
                    if (capability.getSkipCountLeft() <= 0) {
                        capability.setSkipCountLeft(8);
                        HashMap<BlockPos, AirQualityLevel> airBubblePositions = Maps.newHashMap();
                        BlockPos blockPos = collectAirQualityPositions(chunk, entry.getValue(), airBubblePositions);
                        boolean markDirty = false;
                        if (entry.getValue().equals(getChunkStartingPosition(chunk))) {
                            capability.getAirBubblePositions().clear();
                            capability.getAirBubblePositions().putAll(airBubblePositions);
                            ThinAirMod.sendToAllTracking(chunk, new ClientboundChunkAirQualityPacket(
                                    chunkPos, airBubblePositions, ClientboundChunkAirQualityPacket.Mode.REPLACE));
                            markDirty = true;
                        } else if (!airBubblePositions.isEmpty()) {
                            capability.getAirBubblePositions().putAll(airBubblePositions);
                            ThinAirMod.sendToAllTracking(chunk, new ClientboundChunkAirQualityPacket(
                                    chunkPos, airBubblePositions, ClientboundChunkAirQualityPacket.Mode.ADD));
                            markDirty = true;
                        }
                        if (markDirty) {
                            chunk.setUnsaved(true);
                        }
                        if (blockPos != null) {
                            iterator.set(Map.entry(chunkPos, blockPos));
                            return;
                        }
                    } else {
                        capability.setSkipCountLeft(capability.getSkipCountLeft() - 1);
                    }
                } else {
                    return;
                }
            }
            iterator.remove();
            CHUNKS_TO_SCAN.remove(chunkPos);
        }
        } finally {
            if (!DIRTY_CHUNKS.isEmpty()) {
                Set<ChunkPos> dirtyChunks;
                synchronized (DIRTY_CHUNKS) {
                    dirtyChunks = Sets.newHashSet(DIRTY_CHUNKS);
                    DIRTY_CHUNKS.clear();
                }
                for (ChunkPos dirtyChunkPos : dirtyChunks) {
                    updateLanternsInChunk(level, dirtyChunkPos);
                }
            }
        }
    }

    private static void updateLanternsInChunk(ServerLevel level, ChunkPos chunkPos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
        if (chunk == null) {
            return;
        }
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxX = chunkPos.getMaxBlockX();
        int maxZ = chunkPos.getMaxBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y < maxY; y++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockState blockState = chunk.getBlockState(blockPos);
                    if (!(blockState.getBlock() instanceof SafetyLanternBlock)) {
                        continue;
                    }
                    if (blockState.getValue(SafetyLanternBlock.LOCKED)) {
                        continue;
                    }
                    AirQualityLevel ambient = AirQualityHelper.INSTANCE.getAirQualityAtLocation(
                            level, Vec3.atCenterOf(blockPos), blockPos);
                    if (blockState.getValue(SafetyLanternBlock.AIR_QUALITY) != ambient) {
                        level.setBlockAndUpdate(blockPos, blockState.setValue(SafetyLanternBlock.AIR_QUALITY, ambient));
                    }
                }
            }
        }
    }

    private static BlockPos getChunkStartingPosition(LevelChunk chunk) {
        int posX = chunk.getPos().getMinBlockX();
        int posY = chunk.getMinBuildHeight();
        int posZ = chunk.getPos().getMinBlockZ();
        return new BlockPos(posX, posY, posZ);
    }

    @Nullable
    private static BlockPos collectAirQualityPositions(
            LevelChunk chunk, BlockPos startingPosition, Map<BlockPos, AirQualityLevel> airBubbleEntries) {
        int minX = chunk.getPos().getMinBlockX();
        int minY = chunk.getMinBuildHeight();
        int minZ = chunk.getPos().getMinBlockZ();
        int startX = startingPosition.getX() - minX;
        int startY = startingPosition.getY();
        int startZ = startingPosition.getZ() - minZ;
        int iterations = 0;
        for (int dx = startX; dx < 16; dx++, startX = 0) {
            for (int dz = startZ; dz < 16; dz++, startZ = 0) {
                int posX = minX + dx;
                int posZ = minZ + dz;
                int maxY = chunk.getLevel().getHeight(Heightmap.Types.WORLD_SURFACE, posX, posZ);
                for (int posY = startY; posY < maxY; posY++, startY = minY, iterations++) {
                    BlockPos blockPos = new BlockPos(posX, posY, posZ);
                    if (iterations >= 98304) {
                        return blockPos;
                    }
                    BlockState blockState = chunk.getBlockState(blockPos);
                    AirQualityLevel airQualityLevel = AirQualityLevel.getAirQualityFromBlock(blockState);
                    if (airQualityLevel != null) {
                        airBubbleEntries.put(blockPos, airQualityLevel);
                    }
                }
            }
        }
        return null;
    }
}
