package com.hbm.items.food;

import com.hbm.extprop.HbmLivingProps;
import com.hbm.items.ModItems;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemCookie extends ItemFood {

	public ItemCookie(int hunger, float saturation, boolean isWolfFood) {
		super(hunger, saturation, isWolfFood);
		if(this == ModItems.cookie_uranium) {
			this.setAlwaysEdible();
		}
	}

	@Override
	protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
		if(!world.isRemote) {
			if(this == ModItems.cookie_uranium) {
				HbmLivingProps.incrementRadiation(player, 5F);
			}
		}
	}
}
