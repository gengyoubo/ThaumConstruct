package github.com.gengyoubo.TCt.arcane;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.Level;
import net.minecraft.world.Containers;
import slimeknights.tconstruct.tables.block.TinkersAnvilBlock;

public final class ArcaneAnvilBlock extends TinkersAnvilBlock {
    public ArcaneAnvilBlock(BlockBehaviour.Properties properties) { super(properties, 6); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArcaneAnvilBlockEntity(pos, state); }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ArcaneAnvilBlockEntity anvil) {
            Containers.dropContents(level, pos, anvil.wandSlot());
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
