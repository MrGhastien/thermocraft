package mrghastien.thermocraft.client.screens;

import mrghastien.thermocraft.client.screens.widgets.ThermoBar;
import mrghastien.thermocraft.client.screens.widgets.Widget;
import mrghastien.thermocraft.common.inventory.containers.ThermalCapacitorContainer;
import mrghastien.thermocraft.common.tileentities.ThermalCapacitorTile;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class ThermalCapacitorScreen extends BaseContainerScreen<ThermalCapacitorContainer, ThermalCapacitorTile> {

    public ThermalCapacitorScreen(ThermalCapacitorContainer container, PlayerInventory playerInventory, ITextComponent title) {
        super(container, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        addWidget(new ThermoBar(tileEntity::getHeatHandler, 62, 9, 52, 69, 293, 1000, Widget.Orientation.UP));
        this.imageHeight = 175;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = (this.imageWidth - font.width(title)) / 2;
    }
}