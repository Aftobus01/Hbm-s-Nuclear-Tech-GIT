package com.hbm.items.machine;

import com.hbm.util.CompatExternal;

import com.hbm.blocks.machine.BlockFluidBarrel;
import com.hbm.inventory.FluidContainer;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Unsiphonable;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemPipette;

import api.hbm.fluidmk2.IFluidStandardReceiverMK2;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ItemFluidSiphon extends Item {

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int i, float f0, float f1, float f2) {
		TileEntity te = CompatExternal.getCoreFromPos(world, x, y, z);

		if(te != null && te instanceof IFluidStandardReceiverMK2) {
			FluidTank[] tanks = ((IFluidStandardReceiverMK2) te).getReceivingTanks();

			boolean hasDrainedTank = false;

			// We need to iterate through the inventory for _each_ siphonable
			// tank, so we can handle fluids that can only go into certain containers
			// After we successfully siphon any fluid from a tank, we stop
			// further processing, multiple fluid types require multiple clicks
			for(FluidTank tank : tanks) {
				if(tank.getFill() <= 0)
					continue;

				ItemStack availablePipette = null;
				FluidType tankType = tank.getTankType();

				if(tankType.hasTrait(FT_Unsiphonable.class))
					continue;

				for(int j = 0; j < player.inventory.mainInventory.length && tank.getFill() > 0; j++) {
					int barrelFill = fillPartialBarrel(player, j, tank);
					if(barrelFill > 0) {
						hasDrainedTank = true;
						tank.setFill(tank.getFill() - barrelFill);
					}
				}

				boolean[] blockedSlots = new boolean[player.inventory.mainInventory.length];
				while(tank.getFill() > 0) {
					int barrelSlot = findLargestEmptyBarrel(player, tankType, blockedSlots);
					if(barrelSlot < 0) break;

					int barrelFill = fillEmptyBarrel(player, barrelSlot, tank);
					if(barrelFill > 0) {
						hasDrainedTank = true;
						tank.setFill(tank.getFill() - barrelFill);
					} else {
						blockedSlots[barrelSlot] = true;
					}
				}

				for(int j = 0; j < player.inventory.mainInventory.length; j++) {
					ItemStack inventoryStack = player.inventory.mainInventory[j];
					if(inventoryStack == null)
						continue;

					FluidContainer container = FluidContainerRegistry.getContainer(tankType, inventoryStack);

					if(availablePipette == null && inventoryStack.getItem() instanceof ItemPipette) {
						ItemPipette pipette = (ItemPipette) inventoryStack.getItem();
						if(!pipette.willFizzle(tankType) && pipette != ModItems.pipette_laboratory) { // Ignoring laboratory pipettes for now
							availablePipette = inventoryStack;
						}
					}

					if(container == null)
						continue;

					ItemStack full = FluidContainerRegistry.getFullContainer(inventoryStack, tankType);

					while(tank.getFill() >= container.content && inventoryStack.stackSize > 0) {
						hasDrainedTank = true;

						inventoryStack.stackSize--;
						if(inventoryStack.stackSize <= 0) {
							player.inventory.mainInventory[j] = null;
						}

						ItemStack filledContainer = full.copy();
						tank.setFill(tank.getFill() - container.content);
						player.inventory.addItemStackToInventory(filledContainer);
					}
				}

				// If the remainder of the tank can only fit into a pipette,
				// fill a pipette with the remainder
				// Will not auto-fill fizzlable pipettes, there is no feedback
				// for the fizzle in this case, and that's a touch too unfair
				if(availablePipette != null && tank.getFill() < 1000) {
					ItemPipette pipette = (ItemPipette) availablePipette.getItem();

					if(pipette.acceptsFluid(tankType, availablePipette)) {
						hasDrainedTank = true;
						tank.setFill(pipette.tryFill(tankType, tank.getFill(), availablePipette));
					}
				}

				if(hasDrainedTank)
					return true;
			}
		}

		return false;
	}

	private int findLargestEmptyBarrel(EntityPlayer player, FluidType type, boolean[] blockedSlots) {
		int largestSlot = -1;
		int largestCapacity = -1;

		for(int slot = 0; slot < player.inventory.mainInventory.length; slot++) {
			if(blockedSlots[slot]) continue;

			ItemStack barrelStack = player.inventory.mainInventory[slot];
			if(barrelStack == null) continue;

			Block block = Block.getBlockFromItem(barrelStack.getItem());
			if(!(block instanceof BlockFluidBarrel)) continue;

			BlockFluidBarrel barrel = (BlockFluidBarrel) block;
			if(!barrel.canStore(type)) continue;

			FluidTank barrelTank = barrel.getTankFromStack(barrelStack);
			if(barrelTank.getFill() > 0) continue;

			if(barrelTank.getMaxFill() > largestCapacity) {
				largestSlot = slot;
				largestCapacity = barrelTank.getMaxFill();
			}
		}

		return largestSlot;
	}

	private int fillEmptyBarrel(EntityPlayer player, int slot, FluidTank sourceTank) {
		ItemStack barrelStack = player.inventory.mainInventory[slot];
		Block block = Block.getBlockFromItem(barrelStack.getItem());

		if(!(block instanceof BlockFluidBarrel)) return 0;

		BlockFluidBarrel barrel = (BlockFluidBarrel) block;
		if(!barrel.canStore(sourceTank.getTankType())) return 0;

		FluidTank barrelTank = barrel.getTankFromStack(barrelStack);
		if(barrelTank.getFill() > 0) return 0;

		int amount = Math.min(sourceTank.getFill(), barrelTank.getMaxFill());
		if(amount <= 0) return 0;

		barrelTank.setTankType(sourceTank.getTankType());
		barrelTank.withPressure(sourceTank.getPressure());
		barrelTank.setFill(amount);

		if(barrelStack.stackSize == 1) {
			barrel.writeTankToStack(barrelStack, barrelTank);
			player.inventory.markDirty();
			return amount;
		}

		ItemStack filledBarrel = barrelStack.copy();
		filledBarrel.stackSize = 1;
		barrel.writeTankToStack(filledBarrel, barrelTank);

		if(!player.inventory.addItemStackToInventory(filledBarrel)) return 0;

		barrelStack.stackSize--;
		player.inventory.markDirty();
		return amount;
	}

	private int fillPartialBarrel(EntityPlayer player, int slot, FluidTank sourceTank) {
		ItemStack barrelStack = player.inventory.mainInventory[slot];
		if(barrelStack == null) return 0;

		Block block = Block.getBlockFromItem(barrelStack.getItem());

		if(!(block instanceof BlockFluidBarrel)) return 0;

		BlockFluidBarrel barrel = (BlockFluidBarrel) block;
		FluidTank barrelTank = barrel.getTankFromStack(barrelStack);

		if(barrelTank.getFill() <= 0 || barrelTank.getFill() >= barrelTank.getMaxFill()) return 0;
		if(barrelTank.getTankType() != sourceTank.getTankType()) return 0;
		if(barrelTank.getPressure() != sourceTank.getPressure()) return 0;

		int amount = Math.min(sourceTank.getFill(), barrelTank.getMaxFill() - barrelTank.getFill());
		if(amount <= 0) return 0;

		barrelTank.setFill(barrelTank.getFill() + amount);

		if(barrelStack.stackSize == 1) {
			barrel.writeTankToStack(barrelStack, barrelTank);
			player.inventory.markDirty();
			return amount;
		}

		ItemStack filledBarrel = barrelStack.copy();
		filledBarrel.stackSize = 1;
		barrel.writeTankToStack(filledBarrel, barrelTank);

		if(!player.inventory.addItemStackToInventory(filledBarrel)) return 0;

		barrelStack.stackSize--;
		player.inventory.markDirty();
		return amount;
	}

}
