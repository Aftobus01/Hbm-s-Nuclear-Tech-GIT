package com.hbm.items.tool;

import net.minecraft.item.ItemStack;

public class ItemGergMultiTool extends ItemCraftingDegradation {

	public ItemGergMultiTool(int durability) {
		super(durability);
		GergToolType.SCREWDRIVER.register(new ItemStack(this));
		GergToolType.SAW.register(new ItemStack(this));
		GergToolType.CUTTER.register(new ItemStack(this));
		this.setFull3D();
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
}
