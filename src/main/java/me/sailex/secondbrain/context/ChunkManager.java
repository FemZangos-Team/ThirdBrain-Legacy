package me.sailex.secondbrain.context;

import me.sailex.altoclef.multiversion.EntityVer;
import me.sailex.secondbrain.config.BaseConfig;
import me.sailex.secondbrain.model.context.BlockData;
import me.sailex.secondbrain.util.LogUtil;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.concurrent.*;

import static me.sailex.secondbrain.util.MCDataUtil.getMiningLevel;
import static me.sailex.secondbrain.util.MCDataUtil.getToolNeeded;

public class ChunkManager {
    private static final long DIAG_RATE_LIMIT_MS = 5000L;
    private static final long THREAD_DIAG_RATE_LIMIT_MS = 60_000L;

    private final int verticalScanRange;
    private final int contextRangeInBlocks;
    private final int chunkRadius;

    private final ServerPlayerEntity npcEntity;
    private final ScheduledExecutorService threadPool;
    private final List<BlockData> currentLoadedBlocks;
    private int scannedColumnsCountLastRefresh;

    private volatile List<BlockData> nearbyBlocks = List.of();


    public ChunkManager(ServerPlayerEntity npcEntity, BaseConfig config) {
        this(
            npcEntity,
            Math.max(1, config.getContextChunkRadius()) * 16,
            config.getContextVerticalScanRange(),
            config.getChunkExpiryTime()
        );
    }

    public ChunkManager(ServerPlayerEntity npcEntity, int contextRangeInBlocks, int verticalScanRange, int chunkExpiryTime) {
        this.npcEntity = npcEntity;
        this.verticalScanRange = verticalScanRange;
        this.contextRangeInBlocks = Math.max(1, contextRangeInBlocks);
        this.chunkRadius = (this.contextRangeInBlocks - 1) / 16 + 1;
        this.currentLoadedBlocks = new ArrayList<>();
        this.threadPool = Executors.newSingleThreadScheduledExecutor();
        scheduleRefreshBlocks(chunkExpiryTime);
    }

    private void scheduleRefreshBlocks(int chunkExpiryTime) {
        threadPool.scheduleAtFixedRate(() -> {
            long startNs = System.nanoTime();
            int loadedChunksCount;
            int scannedColumnsCount;
            synchronized (this) {
                loadedChunksCount = updateAllBlocks();
                scannedColumnsCount = scannedColumnsCountLastRefresh;
                updateNearbyBlocks();
            }
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
            logChunkDiagnostics(elapsedMs, loadedChunksCount, scannedColumnsCount);
        }, 0, chunkExpiryTime, TimeUnit.SECONDS);
    }

    public List<BlockData> getBlocksOfType(String type, int numberOfBlocks) {
        List<BlockData> blocksFound = new ArrayList<>();

        for (BlockData block : currentLoadedBlocks) {
            if (blocksFound.size() >= numberOfBlocks) {
                break;
            } else if (type.equals(block.type())) {
                blocksFound.add(block);
            }
        }
        if (blocksFound.size() < numberOfBlocks) {
            LogUtil.error("Only %s blocks found of %s (wanted: %s)".formatted(
                    blocksFound.size(), type, numberOfBlocks));
        }
        return blocksFound;
    }

    /**
     * Updates block data of every block type nearest block to the npc
     */
    private void updateNearbyBlocks() {
        Map<String, BlockData> nearestBlocks = new HashMap<>();
        this.nearbyBlocks.clear();

        for (BlockData block : currentLoadedBlocks) {
            String blockType = block.type();
            if (!nearestBlocks.containsKey(blockType) ||
                    isCloser(block.position(), nearestBlocks.get(blockType).position())) {
                nearestBlocks.put(blockType, block);
            }
        }
        this.nearbyBlocks = List.copyOf(nearestBlocks.values());
    }

    /**
     * Updates all blocks in the chunks around the NPC
     */
    private int updateAllBlocks() {
        currentLoadedBlocks.clear();
        scannedColumnsCountLastRefresh = 0;
        World world = EntityVer.getWorld(npcEntity);
        ChunkPos centerChunk = npcEntity.getChunkPos();
        int centerX = npcEntity.getBlockPos().getX();
        int centerZ = npcEntity.getBlockPos().getZ();
        int loadedChunksCount = 0;

        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                ChunkPos pos = new ChunkPos(centerChunk.x + x, centerChunk.z + z);

                boolean isLoaded = world.isChunkLoaded(pos.x, pos.z);

                if (isLoaded) {
                    loadedChunksCount++;
                    ScanChunkResult result = scanChunk(pos, centerX, centerZ);
                    currentLoadedBlocks.addAll(result.blocks());
                    scannedColumnsCountLastRefresh += result.scannedColumnsCount();
                }
            }
        }
        return loadedChunksCount;
    }

    private ScanChunkResult scanChunk(ChunkPos chunk, int centerX, int centerZ) {
        World world = EntityVer.getWorld(npcEntity);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int baseY = Math.max(0, npcEntity.getBlockPos().getY() - verticalScanRange);
        int maxY = Math.min(world.getHeight(), npcEntity.getBlockPos().getY() + verticalScanRange);

        List<BlockData> blocks = new ArrayList<>();
        int scannedColumnsCount = 0;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int blockX = chunk.getStartX() + x;
                int blockZ = chunk.getStartZ() + z;

                if (Math.abs(blockX - centerX) > contextRangeInBlocks || Math.abs(blockZ - centerZ) > contextRangeInBlocks) {
                    continue;
                }

                scannedColumnsCount++;
                pos.set(blockX, baseY, blockZ);
                WorldChunk currentChunk = world.getWorldChunk(pos);
                for (int y = baseY; y < maxY; y++) {
                    pos.set(blockX, y, blockZ);
                    BlockState blockState = currentChunk.getBlockState(pos);
                    String blockType = blockState.getBlock()
                            .getName().getString()
                            .toLowerCase().replace(" ", "_");

                    if (blockType.contains("air")) continue;

                    if (isAccessible(pos, currentChunk)) {
                        blocks.add(new BlockData(blockType, pos.toImmutable(),
                                getMiningLevel(blockState), getToolNeeded(blockState)));
                    }
                }
            }
        }
        return new ScanChunkResult(blocks, scannedColumnsCount);
    }

    private boolean isAccessible(BlockPos pos, WorldChunk chunk) {
        for (Direction dir : Direction.values()) {
            if (chunk.getBlockState(pos.offset(dir)).isAir()) {
                return true;
            }
        }
        return false;
    }

    private boolean isCloser(BlockPos pos1, BlockPos pos2) {
        double dist1 = npcEntity.getBlockPos().getSquaredDistance(pos1);
        double dist2 = npcEntity.getBlockPos().getSquaredDistance(pos2);
        return dist1 < dist2;
    }

    public void stopService() {
        this.threadPool.shutdownNow();
    }

    private void logChunkDiagnostics(long elapsedMs, int loadedChunksCount, int scannedColumnsCount) {
        if (!LogUtil.isVerboseEnabled()) {
            return;
        }
        String npcName = npcEntity.getName().getString();
        String threadName = Thread.currentThread().getName();
        int loadedBlocks = currentLoadedBlocks.size();
        int nearbyTypes = nearbyBlocks.size();
        String baseDetails = "area=chunk npc=%s thread=%s loaded_chunks=%d scanned_columns=%d loaded_blocks=%d nearby_types=%d"
                .formatted(npcName, threadName, loadedChunksCount, scannedColumnsCount, loadedBlocks, nearbyTypes);

        if (elapsedMs > 250) {
            LogUtil.warnRateLimited(
                    "chunk.refresh.slow." + npcName,
                    "[SB-DIAG] " + baseDetails + " metric=refresh_ms value=" + elapsedMs,
                    DIAG_RATE_LIMIT_MS
            );
        }
        if (loadedBlocks > 40_000) {
            LogUtil.warnRateLimited(
                    "chunk.loaded_blocks.high." + npcName,
                    "[SB-DIAG] " + baseDetails + " metric=loaded_blocks value=" + loadedBlocks,
                    DIAG_RATE_LIMIT_MS
            );
        }
        if (nearbyTypes > 500) {
            LogUtil.warnRateLimited(
                    "chunk.nearby_types.high." + npcName,
                    "[SB-DIAG] " + baseDetails + " metric=nearby_types value=" + nearbyTypes,
                    DIAG_RATE_LIMIT_MS
            );
        }
        if (!"Server thread".equals(threadName)) {
            LogUtil.warnRateLimited(
                    "chunk.thread.off_server." + npcName,
                    "[SB-DIAG] " + baseDetails + " metric=off_thread_world_scan value=1",
                    THREAD_DIAG_RATE_LIMIT_MS
            );
        }
    }

    private record ScanChunkResult(List<BlockData> blocks, int scannedColumnsCount) {}

    public List<BlockData> getNearbyBlocks() {
        return nearbyBlocks;
    }
}
