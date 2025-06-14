package com.hbm.items;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSeedFood;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemStrawberry extends ItemSeedFood {
	public ItemStrawberry(int healAmount, float saturation, Block crop, Block soil) {
		super(healAmount, saturation, crop, soil);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		return false; // Prevent planting
	}
}
