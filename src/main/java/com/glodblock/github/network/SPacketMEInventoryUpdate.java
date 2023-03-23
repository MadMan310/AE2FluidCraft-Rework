package com.glodblock.github.network;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

import com.glodblock.github.client.gui.GuiFluidCraftConfirm;
import com.glodblock.github.client.gui.GuiFluidMonitor;
import com.glodblock.github.client.gui.GuiItemMonitor;
import com.glodblock.github.client.gui.GuiLevelMaintainer;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SPacketMEInventoryUpdate implements IMessage {

    private List<Object> list;
    private byte ref;
    private boolean isFluid = false;

    public SPacketMEInventoryUpdate() {
        ref = 0;
        list = new LinkedList<>();
    }

    public SPacketMEInventoryUpdate(Boolean isFluid) {
        ref = 0;
        list = new LinkedList<>();
        this.isFluid = isFluid;
    }

    public SPacketMEInventoryUpdate(byte b) {
        ref = b;
        list = new LinkedList<>();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        isFluid = buf.readBoolean();
        long amount = buf.readLong();
        ref = buf.readByte();
        list = new LinkedList<>();
        try {
            for (int i = 0; i < amount; i++) {
                if (isFluid) {
                    list.add(AEFluidStack.loadFluidStackFromPacket(buf));
                } else {
                    list.add(AEItemStack.loadItemStackFromPacket(buf));
                }
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(isFluid);
        buf.writeLong(list.size());
        buf.writeByte(ref);
        try {
            for (Object is : list) {
                if (is instanceof IAEItemStack) ((IAEItemStack) is).writeToPacket(buf);
                if (is instanceof IAEFluidStack) ((IAEFluidStack) is).writeToPacket(buf);
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public void appendItem(final IAEItemStack is) throws BufferOverflowException {
        list.add(is);
    }

    public void appendFluid(final IAEFluidStack is) throws BufferOverflowException {
        list.add(is);
    }

    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    public static class Handler implements IMessageHandler<SPacketMEInventoryUpdate, IMessage> {

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public IMessage onMessage(SPacketMEInventoryUpdate message, MessageContext ctx) {
            final GuiScreen gs = Minecraft.getMinecraft().currentScreen;
            if (gs instanceof GuiFluidMonitor) {
                ((GuiFluidMonitor) gs).postUpdate((List) message.list);
            } else if (gs instanceof GuiItemMonitor) {
                ((GuiItemMonitor) gs).postUpdate((List) message.list);
            } else if (gs instanceof GuiFluidCraftConfirm) {
                ((GuiFluidCraftConfirm) gs).postUpdate((List) message.list, message.ref);
            } else if (gs instanceof GuiLevelMaintainer) {
                ((GuiLevelMaintainer) gs).postUpdate((List) message.list);
            }
            return null;
        }
    }
}
