package com.hbm.items.food;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemMentholCrystal extends ItemFood {

	public ItemMentholCrystal() {
		super(0, 1F, false);
		this.setAlwaysEdible();
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		return EnumAction.eat;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		player.setItemInUse(stack, this.getMaxItemUseDuration(stack));
		return stack;
	}

	@Override
	public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
		if (!player.capabilities.isCreativeMode) {
			--stack.stackSize;
		}

		if (!world.isRemote) {
			world.playSoundEffect(player.posX, player.posY, player.posZ, "hbm:player.sniff", 1.0F,
					1.0F + (world.rand.nextFloat() - 0.5F) * 2F);
		}

		return stack;
	}
}
