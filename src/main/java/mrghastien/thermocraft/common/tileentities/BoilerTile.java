package mrghastien.thermocraft.common.tileentities;

import mrghastien.thermocraft.api.heat.IHeatHandler;
import mrghastien.thermocraft.api.heat.TransferType;
import mrghastien.thermocraft.common.capabilities.Capabilities;
import mrghastien.thermocraft.common.capabilities.fluid.ModFluidHandler;
import mrghastien.thermocraft.common.capabilities.fluid.ModFluidTank;
import mrghastien.thermocraft.common.capabilities.heat.HeatHandler;
import mrghastien.thermocraft.common.capabilities.heat.SidedHeatHandler;
import mrghastien.thermocraft.common.crafting.BoilingRecipe;
import mrghastien.thermocraft.common.crafting.ModRecipeType;
import mrghastien.thermocraft.common.inventory.containers.BaseContainer;
import mrghastien.thermocraft.common.inventory.containers.BoilerContainer;
import mrghastien.thermocraft.common.network.packets.PacketHandler;
import mrghastien.thermocraft.common.registries.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.container.Container;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BoilerTile extends BaseTile {

    private final SidedHeatHandler heatHandler = new SidedHeatHandler(1200, 40, 1, this::setChanged, d -> TransferType.INPUT);
    private final ModFluidHandler inputHandler = new ModFluidHandler(new ModFluidTank(5000));
    private final ModFluidHandler outputHandler = new ModFluidHandler(new ModFluidTank(5000));

    private boolean running;
    private BoilingRecipe currentRecipe;

    public BoilerTile() {
        super(ModTileEntities.BOILER.get());
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        heatHandler.getLazy().invalidate();
        inputHandler.getLazy().invalidate();
        outputHandler.getLazy().invalidate();
    }

    @Nullable
    @Override
    public Container createMenu(int id, PlayerInventory playerInventory, PlayerEntity player) {
        return new BoilerContainer(id, playerInventory, this);
    }

    @Override
    public void tick() {
        //If fluid in input, and the temperature is high enough, consume a certain amount of input per tick
        //depending on the temperature (higher = faster boiling)
        //Also extract energy from the heat handler
        //Finally fill the output tank.

        if(level.isClientSide()) return;
        this.inputHandler.fill(new FluidStack(Fluids.WATER, 10), IFluidHandler.FluidAction.EXECUTE);
        heatHandler.transferEnergy(heatHandler.getInsulationCoefficient() * (IHeatHandler.AIR_TEMPERATURE - heatHandler.getTemperature()));
        if(canRun()) {
            this.currentRecipe = getRecipe();

            if (currentRecipe == null) return;
            setActiveState();
            if(outputHandler.fill(currentRecipe.getOutput(), IFluidHandler.FluidAction.SIMULATE) == currentRecipe.getOutput().getAmount()) {
                FluidStack consumed = consumeInput();
                heatHandler.transferEnergy(currentRecipe.getInputHeatCapacity() * (IHeatHandler.AIR_TEMPERATURE - heatHandler.getTemperature()));
                storeResult();
            }
        } else {
            currentRecipe = null;
            setInactiveState();
        }
        super.tick();
    }

    private FluidStack consumeInput() {
        for(FluidStack stack : currentRecipe.getInput().getFluids()) {
            if(inputHandler.contains(stack)) {
                inputHandler.drain(stack.copy(), IFluidHandler.FluidAction.EXECUTE);
                return stack;
            }
        }
        return null;
    }

    private void storeResult() {
        outputHandler.fill(currentRecipe.getOutput().copy(), IFluidHandler.FluidAction.EXECUTE);
    }

    private BoilingRecipe getRecipe() {
        for(BoilingRecipe recipe : ModRecipeType.BOILING.getRecipes(level).values()) {
            if(recipe.matches(inputHandler)) return recipe;
        }
        return null;
    }

    private boolean canRun() {
        return heatHandler.getTemperature() > 400 && !inputHandler.isEmpty();
    }

    public ModFluidHandler getInputHandler() {
        return inputHandler;
    }

    public ModFluidHandler getOutputHandler() {
        return outputHandler;
    }

    public HeatHandler getHeatHandler() {
        return heatHandler;
    }

    protected BlockState getInactiveState(BlockState state) {
        return state.setValue(BlockStateProperties.LIT, false);
    }

    protected BlockState getActiveState(BlockState state) {
        return state.setValue(BlockStateProperties.LIT, true);
    }

    protected void updateBlockState(BlockState newState) {
        if (level == null) return;
        BlockState oldState = level.getBlockState(worldPosition);
        if (oldState != newState) {
            level.setBlock(worldPosition, newState, 3);
            //level.notifyBlockUpdate(worldPosition, oldState, newState, 3);
        }
    }

    protected void setInactiveState() {
        updateBlockState(getInactiveState(level.getBlockState(worldPosition)));
        running = false;
    }

    protected void setActiveState() {
        updateBlockState(getActiveState(level.getBlockState(worldPosition)));
        running = true;
    }

    @Override
    protected void loadInternal(BlockState state, CompoundNBT nbt) {
        heatHandler.deserializeNBT(nbt.getCompound("Heat"));
        inputHandler.getTank(0).setFluid(FluidStack.loadFluidStackFromNBT(nbt.getCompound("Input")));
        inputHandler.getTank(0).setFluid(FluidStack.loadFluidStackFromNBT(nbt.getCompound("Output")));
    }

    @Override
    protected void saveInternal(CompoundNBT nbt) {
        nbt.put("Heat", heatHandler.serializeNBT());
        nbt.put("Input", inputHandler.getFluidInTank(0).writeToNBT(new CompoundNBT()));
        nbt.put("Output", outputHandler.getFluidInTank(0).writeToNBT(new CompoundNBT()));
    }

    @Override
    public void registerContainerUpdatedData(BaseContainer c) {
        heatHandler.gatherData(c, PacketHandler.CONTAINER_LISTENERS.with(() -> c), level);
        inputHandler.gatherData(c, PacketHandler.CONTAINER_LISTENERS.with(() -> c), level);
        outputHandler.gatherData(c, PacketHandler.CONTAINER_LISTENERS.with(() -> c), level);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if(cap == Capabilities.HEAT_HANDLER_CAPABILITY) return heatHandler.getLazy(side).cast();
        if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if(side == Direction.WEST) return inputHandler.getLazy().cast();
            if(side == Direction.UP) return outputHandler.getLazy().cast();
        }
        return super.getCapability(cap, side);
    }
}