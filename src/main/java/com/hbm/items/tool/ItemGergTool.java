package com.hbm.items.tool;

import com.hbm.main.MainRegistry;

import api.hbm.block.IToolable;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemGergTool extends ItemCraftingDegradation {

	private final GergToolType gergType;
	private final IToolable.ToolType toolType;
	private final int tier;

	public ItemGergTool(GergToolType gergType, IToolable.ToolType toolType, int durability, int tier) {
		super(durability);
		this.gergType = gergType;
		this.toolType = toolType;
		this.tier = tier;
		if(toolType != null) {
			toolType.register(new ItemStack(this));
		}
		this.gergType.register(new ItemStack(this), tier);
		this.setFull3D();
		this.setCreativeTab(MainRegistry.controlTab);
	}

	public GergToolType getGergType() {
		return gergType;
	}

	public IToolable.ToolType getToolType() {
		return toolType;
	}

	public int getTier() {
		return tier;
	}

	@Override
	public ItemStack getContainerItem(ItemStack stack) {
		if(this.getMaxDamage() <= 0) return stack.copy();

		int damage = 1;
		if(stack.hasTagCompound() && stack.getTagCompound().hasKey("gergDurabilityDamage")) {
			damage = stack.getTagCompound().getInteger("gergDurabilityDamage");
		}

		ItemStack copy = stack.copy();
		copy.setItemDamage(stack.getItemDamage() + damage);
		return copy;
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float fX, float fY, float fZ) {
		if(toolType == null) return false;
		Block b = world.getBlock(x, y, z);
		if(b instanceof IToolable) {
			if(((IToolable) b).onScrew(world, player, x, y, z, side, fX, fY, fZ, toolType)) {
				if(this.getMaxDamage() > 0)
					stack.damageItem(1, player);
				return true;
			}
		}
		return false;
	}
}
