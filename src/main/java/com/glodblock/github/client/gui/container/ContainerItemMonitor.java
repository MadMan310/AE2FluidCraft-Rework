package com.glodblock.github.client.gui.container;

import java.nio.BufferOverflowException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.AEApi;
import appeng.api.implementations.guiobjects.IPortableCell;
import appeng.api.implementations.tiles.IMEChest;
import appeng.api.implementations.tiles.IViewCellStorage;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.slot.SlotRestrictedInput;
import appeng.me.helpers.ChannelPowerSrc;
import appeng.util.Platform;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.client.gui.container.base.FCContainerMonitor;
import com.glodblock.github.network.SPacketMEInventoryUpdate;

public class ContainerItemMonitor extends FCContainerMonitor<IAEItemStack> {

    private final IItemList<IAEItemStack> items = AEApi.instance().storage().createItemList();

    public ContainerItemMonitor(final InventoryPlayer ip, final ITerminalHost monitorable) {
        this(ip, monitorable, true);
    }

    protected ContainerItemMonitor(final InventoryPlayer ip, final ITerminalHost monitorable,
            final boolean bindInventory) {
        super(ip, monitorable, bindInventory);
        if (Platform.isServer()) {
            this.serverCM = monitorable.getConfigManager();
            this.monitor = monitorable.getItemInventory();
            if (this.monitor != null) {
                this.monitor.addListener(this, null);
                this.setCellInventory(this.monitor);
                if (monitorable instanceof IPortableCell) {
                    this.setPowerSource((IEnergySource) monitorable);
                } else if (monitorable instanceof IMEChest) {
                    this.setPowerSource((IEnergySource) monitorable);
                } else if (monitorable instanceof IGridHost) {
                    final IGridNode node = ((IGridHost) monitorable).getGridNode(ForgeDirection.UNKNOWN);
                    if (node != null) {
                        this.networkNode = node;
                        final IGrid g = node.getGrid();
                        if (g != null) {
                            this.setPowerSource(new ChannelPowerSrc(this.networkNode, g.getCache(IEnergyGrid.class)));
                        }
                    }
                }
            } else {
                this.setValidContainer(false);
            }
        } else {
            this.monitor = null;
        }

        this.canAccessViewCells = false;
        if (monitorable instanceof IViewCellStorage) {
            for (int y = 0; y < 5; y++) {
                this.cellView[y] = new SlotRestrictedInput(
                        SlotRestrictedInput.PlacableItemType.VIEW_CELL,
                        ((IViewCellStorage) monitorable).getViewCellStorage(),
                        y,
                        206,
                        y * 18 + 8,
                        this.getInventoryPlayer());
                this.cellView[y].setAllowEdit(this.canAccessViewCells);
                this.addSlotToContainer(this.cellView[y]);
            }
        }

        if (bindInventory) {
            this.bindPlayerInventory(ip, 0, 0);
        }
    }

    @Override
    protected void processItemList() {
        if (!this.items.isEmpty()) {
            final IItemList<IAEItemStack> monitorCache = this.monitor.getStorageList();
            final SPacketMEInventoryUpdate piu = new SPacketMEInventoryUpdate();
            for (final IAEItemStack is : this.items) {
                final IAEItemStack send = monitorCache.findPrecise(is);
                if (send == null) {
                    is.setStackSize(0);
                    piu.appendItem(is);
                } else {
                    piu.appendItem(send);
                }
            }

            if (!piu.isEmpty()) {
                this.items.resetStatus();

                for (final Object c : this.crafters) {
                    if (c instanceof EntityPlayer) {
                        FluidCraft.proxy.netHandler.sendTo(piu, (EntityPlayerMP) c);
                    }
                }
            }
        }
    }

    @Override
    protected void queueInventory(final ICrafting c) {
        if (Platform.isServer() && c instanceof EntityPlayer && this.monitor != null) {
            SPacketMEInventoryUpdate piu = new SPacketMEInventoryUpdate();
            final IItemList<IAEItemStack> monitorCache = this.monitor.getStorageList();

            for (final IAEItemStack send : monitorCache) {
                try {
                    piu.appendItem(send);
                } catch (final BufferOverflowException boe) {
                    FluidCraft.proxy.netHandler.sendTo(piu, (EntityPlayerMP) c);

                    piu = new SPacketMEInventoryUpdate();
                    piu.appendItem(send);
                }
            }
            FluidCraft.proxy.netHandler.sendTo(piu, (EntityPlayerMP) c);
        }
    }

    @Override
    public void removeCraftingFromCrafters(final ICrafting c) {
        super.removeCraftingFromCrafters(c);
        if (this.crafters.isEmpty() && this.monitor != null) {
            this.monitor.removeListener(this);
        }
    }

    @Override
    public void postChange(final IBaseMonitor<IAEItemStack> monitor, final Iterable<IAEItemStack> change,
            final BaseActionSource source) {
        for (final IAEItemStack is : change) {
            this.items.add(is);
        }
    }

    @Override
    protected boolean isWirelessTerminal() {
        return false;
    }
}
