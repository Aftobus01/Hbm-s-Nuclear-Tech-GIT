package com.hbm.sound;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class MovingSoundGlyphidDance extends MovingSound {

	public static Map<Integer, MovingSoundGlyphidDance> playingSounds = new HashMap<>();
	public Entity entity;

	public MovingSoundGlyphidDance(ResourceLocation res, Entity entity) {
		super(res);
		this.entity = entity;
		this.repeat = false;
		this.volume = 2.0F;
		this.field_147663_c = 1.0F;
		playingSounds.put(entity.getEntityId(), this);
	}

	@Override
	public void update() {
		if(entity == null || entity.isDead) {
			this.donePlaying = true;
			playingSounds.remove(entity != null ? entity.getEntityId() : null);
			return;
		}
		this.xPosF = (float) entity.posX;
		this.yPosF = (float) entity.posY;
		this.zPosF = (float) entity.posZ;
	}

	public void endSound() {
		this.donePlaying = true;
		playingSounds.remove(entity != null ? entity.getEntityId() : null);
	}
}
