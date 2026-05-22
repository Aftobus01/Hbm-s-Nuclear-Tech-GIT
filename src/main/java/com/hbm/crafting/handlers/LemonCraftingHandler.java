package com.hbm.crafting.handlers;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

public class LemonCraftingHandler implements IRecipe {

	@Override
	public boolean matches(InventoryCrafting inv, World world) {
		boolean hasGrenade = false;
		boolean hasConcrete = false;
		boolean hasDye = false;
		int concreteCount = 0;
		int dyeCount = 0;
		int totalItems = 0;

		for(int i = 0; i < 9; i++) {
			ItemStack stack = inv.getStackInRowAndColumn(i % 3, i / 3);
			if(stack == null) continue;

			totalItems++;

			if(stack.getItem() == ModItems.grenade_universal && !hasGrenade) {
				if(!isCorrectGrenade(stack)) return false;
				hasGrenade = true;
			} else if(stack.getItem() == Item.getItemFromBlock(ModBlocks.concrete)) {
				concreteCount += stack.stackSize;
				hasConcrete = true;
			} else if(stack.getItem() == Items.dye && stack.getItemDamage() == 11) {
				dyeCount += stack.stackSize;
				hasDye = true;
			} else {
				return false;
			}
		}

		if(!hasGrenade) return false;
		if(concreteCount < 4) return false;
		if(dyeCount < 4) return false;
		if(totalItems > 9) return false;

		return true;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv) {
		return new ItemStack(ModItems.grenade_lemon, 1);
	}

	@Override
	public int getRecipeSize() {
		return 9;
	}

	@Override
	public ItemStack getRecipeOutput() {
		return new ItemStack(ModItems.grenade_lemon);
	}

	private boolean isCorrectGrenade(ItemStack stack) {
		if(stack.getItem() != ModItems.grenade_universal) return false;
		if(ItemGrenadeUniversal.getShell(stack) != EnumGrenadeShell.FRAG) return false;
		if(ItemGrenadeUniversal.getFilling(stack) != EnumGrenadeFilling.INC) return false;
		if(ItemGrenadeUniversal.getFuze(stack) != EnumGrenadeFuze.IMPACT) return false;
		EnumGrenadeExtra extra = ItemGrenadeUniversal.getExtra(stack);
		if(extra != EnumGrenadeExtra.TRIPLEX) return false;
		return true;
	}
}
