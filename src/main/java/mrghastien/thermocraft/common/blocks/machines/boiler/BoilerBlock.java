package mrghastien.thermocraft.common.blocks.machines.boiler;

import mrghastien.thermocraft.common.blocks.MachineBlock;
import mrghastien.thermocraft.common.registries.ModTileEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.util.ForgeSoundType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BoilerBlock extends MachineBlock {

    public BoilerBlock() {
        super(Properties.of(Material.METAL).sound(ForgeSoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.LIT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos blockPos, @Nonnull BlockState blockState) {
        return new BoilerBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <A extends BlockEntity> BlockEntityTicker<A> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<A> type) {
        return createTicker(level, type, ModTileEntities.BOILER.get());
    }
}
