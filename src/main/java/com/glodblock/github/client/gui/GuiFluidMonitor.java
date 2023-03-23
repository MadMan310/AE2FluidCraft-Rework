package com.glodblock.github.client.gui;

import java.util.List;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.localization.GuiText;
import appeng.util.item.AEItemStack;

import com.glodblock.github.client.gui.base.FCGuiMonitor;
import com.glodblock.github.client.gui.container.ContainerFluidMonitor;
import com.glodblock.github.client.me.FluidRepo;
import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.util.Util;

public class GuiFluidMonitor extends FCGuiMonitor<IAEFluidStack> {

    public GuiFluidMonitor(final InventoryPlayer inventoryPlayer, final ITerminalHost te,
            final ContainerFluidMonitor c) {
        super(inventoryPlayer, te, c);
        this.repo = new FluidRepo(getScrollBar(), this);
    }

    public void postUpdate(final List<IAEFluidStack> list) {
        for (final IAEFluidStack is : list) {
            IAEItemStack stack = AEItemStack.create(ItemFluidDrop.newDisplayStack(is.getFluidStack()));
            stack.setStackSize(is.getStackSize());
            this.repo.postUpdate(stack);
        }
        this.repo.updateView();
        this.setScrollBar();
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString(this.getGuiDisplayName(GuiText.Terminal.getLocal()), 8, 6, 4210752);
        this.fontRendererObj.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3, 4210752);
    }

    @Override
    protected IAEItemStack transformItem(IAEItemStack stack) {
        if (stack.getItem() instanceof ItemFluidDrop) {
            return ItemFluidDrop.newAeStack(ItemFluidDrop.getAeFluidStack(stack));
        }
        return stack;
    }

    @Override
    public void setTextFieldValue(String displayName, int mousex, int mousey, ItemStack stack) {
        if (Util.getFluidFromItem(stack) != null) {
            setSearchString(Util.getFluidFromItem(stack).getLocalizedName(), true);
        }
    }
}
