package com.hbm.render.item;

import org.lwjgl.opengl.GL11;

import com.hbm.main.ResourceManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

public class ItemRenderGrenadeLemon implements IItemRenderer {

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		switch(type) {
		case EQUIPPED:
		case EQUIPPED_FIRST_PERSON:
		case ENTITY:
		case INVENTORY: return true;
		default: return false;
		}
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return type == ItemRenderType.ENTITY && 
				(helper == ItemRendererHelper.ENTITY_BOBBING || helper == ItemRendererHelper.ENTITY_ROTATION);
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glShadeModel(GL11.GL_SMOOTH);

		if(type == ItemRenderType.INVENTORY) {
			RenderHelper.enableGUIStandardItemLighting();
			GL11.glTranslated(8, 8, 0);
			GL11.glScaled(-1, -1, -1);
			GL11.glRotated(45, 0, 0, 1);
			GL11.glRotated(150, 0, 1, 0);
			GL11.glRotated(15, 1, 0, 0);
			GL11.glScaled(2, 2, 2);
			renderLemon();
		}

		if(type == ItemRenderType.EQUIPPED) {
			GL11.glScaled(0.125, 0.125, 0.125);
			GL11.glTranslated(3, 1, -0.5);
			renderLemon();
		}

		if(type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
			GL11.glScaled(0.125, 0.125, 0.125);
			GL11.glTranslated(3, 3, -3);
			GL11.glRotated(180, 0, -1, 0);
			renderLemon();
		}

		if(type == ItemRenderType.ENTITY) {
			GL11.glScaled(0.125, 0.125, 0.125);
			renderLemon();
		}

		GL11.glShadeModel(GL11.GL_FLAT);
	}

	private void renderLemon() {
		Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.lemon_tex);
		ResourceManager.lemon.renderPart("lemon");

		Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.lemon_extra_tex);
		ResourceManager.lemon.renderPart("Sphere_Sphere.002");
	}
}
