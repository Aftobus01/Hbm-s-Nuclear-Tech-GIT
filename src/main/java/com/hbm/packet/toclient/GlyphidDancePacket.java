package com.hbm.packet.toclient;

import com.hbm.sound.MovingSoundGlyphidDance;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class GlyphidDancePacket implements IMessage {

	public int entityId;
	public boolean start;

	public GlyphidDancePacket() { }

	public GlyphidDancePacket(int entityId, boolean start) {
		this.entityId = entityId;
		this.start = start;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		entityId = buf.readInt();
		start = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(entityId);
		buf.writeBoolean(start);
	}

	public static class Handler implements IMessageHandler<GlyphidDancePacket, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(GlyphidDancePacket m, MessageContext ctx) {
			Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(m.entityId);
			if(m.start) {
				if(entity != null && MovingSoundGlyphidDance.playingSounds.get(m.entityId) == null) {
					Minecraft.getMinecraft().getSoundHandler().playSound(new MovingSoundGlyphidDance(new ResourceLocation("hbm:la_cucaracha"), entity));
				}
			} else {
				MovingSoundGlyphidDance sound = MovingSoundGlyphidDance.playingSounds.get(m.entityId);
				if(sound != null) {
					sound.endSound();
				}
			}
			return null;
		}
	}
}
