package net.sbo.mod.utils.hubcache;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.jspecify.annotations.Nullable;

/**
 * A chunk the server already unloaded, put back from {@link ChunkSnapshot} so it keeps rendering.
 */
public final class FakeChunk extends LevelChunk {
    public FakeChunk(Level level, ChunkPos pos, @Nullable LevelChunkSection[] sections) {
        super(level, pos, UpgradeData.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(), 0L, sections, null, null);
    }

    @Override
    public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, int flags) {
        // The sections are shared with the snapshot, so they must never change. Servers sometimes still
        // send block updates for chunks they just unloaded.
        return null;
    }
}
