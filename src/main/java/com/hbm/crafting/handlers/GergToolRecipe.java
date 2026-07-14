package com.hbm.crafting.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hbm.items.tool.GergToolType;
import com.hbm.items.tool.ItemBlowtorch;
import com.hbm.items.tool.ItemGergTool;

import net.minecraft.block.Block;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public class GergToolRecipe implements IRecipe {

	private final ItemStack result;

	private final boolean isShapeless;
	private final int width;
	private final int height;
	private final Object[] shapedInputs;
	private final List<Object> shapelessInputs;
	private final GergToolType[] tools;
	private final int minTier;

	private final int uniqueIngredients;
	private final int outputCount;

	private boolean mirrored;

	private ItemStack[] displayRecipe;

	public GergToolRecipe(ItemStack result, int width, int height, Object[] shapedInputs, int minTier, GergToolType... tools) {
		this.result = result;
		this.isShapeless = false;
		this.width = width;
		this.height = height;
		this.shapedInputs = shapedInputs;
		this.shapelessInputs = null;
		this.tools = tools;
		this.minTier = minTier;
		this.outputCount = result.stackSize;
		this.uniqueIngredients = countUniqueShaped();
	}

	public GergToolRecipe setMirrored(boolean mirrored) {
		this.mirrored = mirrored;
		return this;
	}

	public GergToolRecipe(ItemStack result, List<Object> shapelessInputs, int minTier, GergToolType... tools) {
		this.result = result;
		this.isShapeless = true;
		this.width = 0;
		this.height = 0;
		this.shapedInputs = null;
		this.shapelessInputs = shapelessInputs;
		this.tools = tools;
		this.minTier = minTier;
		this.outputCount = result.stackSize;
		this.uniqueIngredients = countUniqueShapeless();
	}

	private int countUniqueShaped() {
		List<Object> unique = new ArrayList<>();
		for(int i = 0; i < 9; i++) {
			Object ing = i < shapedInputs.length ? shapedInputs[i] : null;
			if(ing != null && !(ing instanceof GergToolType) && !containsIngredient(unique, ing)) {
				unique.add(ing);
			}
		}
		return unique.size();
	}

	private int countUniqueShapeless() {
		List<Object> unique = new ArrayList<>();
		for(Object ing : shapelessInputs) {
			if(!containsIngredient(unique, ing)) {
				unique.add(ing);
			}
		}
		return unique.size();
	}

	private boolean containsIngredient(List<Object> list, Object ing) {
		for(Object existing : list) {
			if(ingredientsMatch(existing, ing)) return true;
		}
		return false;
	}

	private boolean ingredientsMatch(Object a, Object b) {
		if(a instanceof ItemStack && b instanceof ItemStack) {
			ItemStack sa = (ItemStack) a;
			ItemStack sb = (ItemStack) b;
			return sa.getItem() == sb.getItem() && (sa.getItemDamage() == sb.getItemDamage() || sa.getItemDamage() == OreDictionary.WILDCARD_VALUE || sb.getItemDamage() == OreDictionary.WILDCARD_VALUE);
		}
		if(a instanceof String && b instanceof String) {
			return a.equals(b);
		}
		return false;
	}

	@Override
	public boolean matches(InventoryCrafting inv, World world) {
		boolean matched;
		if(isShapeless) {
			matched = matchesShapeless(inv);
		} else {
			matched = matchesShaped(inv);
		}

		if(matched) {
			writeToolDamageNBT(inv);
		}

		return matched;
	}

	private void writeToolDamageNBT(InventoryCrafting inv) {
		int damage = calcDurabilityDamage();
		for(int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack slot = inv.getStackInSlot(i);
			if(slot != null && (slot.getItem() instanceof ItemGergTool || slot.getItem() instanceof ItemBlowtorch)) {
				if(!slot.hasTagCompound()) {
					slot.setTagCompound(new NBTTagCompound());
				}
				slot.getTagCompound().setInteger("gergDurabilityDamage", damage);
			}
		}
	}

	private boolean matchesShaped(InventoryCrafting inv) {
		if(checkShapedPattern(inv, false)) return true;
		return mirrored && checkShapedPattern(inv, true);
	}

	private boolean checkShapedPattern(InventoryCrafting inv, boolean mirror) {
		boolean[] toolFound = new boolean[tools.length];
		int gridSize = inv.getSizeInventory();

		for(int i = 0; i < gridSize; i++) {
			ItemStack slot = inv.getStackInSlot(i);
			Object required = getShapedInput(i, mirror);

			if(required != null) {
				if(slot == null) return false;
				if(!itemMatches(required, slot)) return false;

			} else {
				if(slot != null) {
					boolean isTool = false;
					for(int t = 0; t < tools.length; t++) {
						if(!toolFound[t] && GergToolType.isToolOfType(slot, tools[t]) && GergToolType.getTier(slot) >= minTier) {
							toolFound[t] = true;
							isTool = true;
							break;
						}
					}
					if(!isTool) return false;
				}
			}
		}

		if(gridSize < shapedInputs.length) {
			for(int i = gridSize; i < shapedInputs.length; i++) {
				if(getShapedInput(i, mirror) != null) return false;
			}
		}

		for(boolean found : toolFound) {
			if(!found) return false;
		}

		return true;
	}

	private Object getShapedInput(int gridIndex, boolean mirror) {
		if(!mirror) return gridIndex < shapedInputs.length ? shapedInputs[gridIndex] : null;
		int r = gridIndex / 3;
		int c = gridIndex % 3;
		int mirroredCol = width - 1 - c;
		if(mirroredCol < 0 || mirroredCol >= width) return null;
		int mirroredIndex = r * 3 + mirroredCol;
		return mirroredIndex < shapedInputs.length ? shapedInputs[mirroredIndex] : null;
	}

	private boolean matchesShapeless(InventoryCrafting inv) {
		List<ItemStack> gridItems = new ArrayList<>();
		for(int i = 0; i < 9; i++) {
			ItemStack slot = inv.getStackInSlot(i);
			if(slot != null) gridItems.add(slot);
		}

		List<ItemStack> remaining = new ArrayList<>(gridItems);
		for(Object input : shapelessInputs) {
			boolean found = false;
			Iterator<ItemStack> it = remaining.iterator();
			while(it.hasNext()) {
				ItemStack stack = it.next();
				if(itemMatches(input, stack)) {
					it.remove();
					found = true;
					break;
				}
			}
			if(!found) return false;
		}

		boolean[] toolFound = new boolean[tools.length];
		for(ItemStack stack : remaining) {
			boolean isTool = false;
			for(int t = 0; t < tools.length; t++) {
				if(!toolFound[t] && GergToolType.isToolOfType(stack, tools[t]) && GergToolType.getTier(stack) >= minTier) {
					toolFound[t] = true;
					isTool = true;
					break;
				}
			}
			if(!isTool) return false;
		}

		for(boolean found : toolFound) {
			if(!found) return false;
		}

		return true;
	}

	private boolean itemMatches(Object required, ItemStack slot) {
		if(required instanceof ItemStack) {
			ItemStack req = (ItemStack) required;
			return slot.getItem() == req.getItem() &&
					(req.getItemDamage() == OreDictionary.WILDCARD_VALUE || slot.getItemDamage() == req.getItemDamage());
		} else if(required instanceof Block) {
			return slot.getItem() == Item.getItemFromBlock((Block) required);
		} else if(required instanceof String) {
			List<ItemStack> ores = OreDictionary.getOres((String) required);
			for(ItemStack ore : ores) {
				if(OreDictionary.itemMatches(ore, slot, false)) return true;
			}
			return false;
		}
		return false;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv) {
		return result.copy();
	}

	private int calcDurabilityDamage() {
		return Math.max(1, uniqueIngredients + outputCount);
	}

	@Override
	public int getRecipeSize() {
		if(isShapeless) {
			return shapelessInputs.size() + tools.length;
		}
		int count = 0;
		for(Object o : shapedInputs) {
			if(o != null && !(o instanceof GergToolType)) {
				count++;
			}
		}
		count += tools.length;
		return Math.min(count, 9);
	}

	@Override
	public ItemStack getRecipeOutput() {
		return result;
	}

	public ItemStack[] getDisplayRecipe() {
		if(displayRecipe == null) buildDisplayRecipe();
		return displayRecipe;
	}

	private void buildDisplayRecipe() {
		displayRecipe = new ItemStack[9];

		if(isShapeless) {
			int slot = 0;
			if(shapelessInputs != null) {
				for(Object input : shapelessInputs) {
					if(slot >= 9) break;
					displayRecipe[slot++] = getItemStack(input);
				}
			}
			for(GergToolType tool : tools) {
				if(slot >= 9) break;
				displayRecipe[slot++] = GergToolType.getAny(tool, minTier);
			}
		} else {
			if(shapedInputs != null) {
				for(int i = 0; i < 9; i++) {
					if(i < shapedInputs.length) {
						displayRecipe[i] = getItemStack(shapedInputs[i]);
					}
				}
			}
			int toolSlot = 8;
			for(GergToolType tool : tools) {
				while(toolSlot >= 0 && displayRecipe[toolSlot] != null) {
					toolSlot--;
				}
				if(toolSlot >= 0) {
					displayRecipe[toolSlot] = GergToolType.getAny(tool, minTier);
					toolSlot--;
				}
			}
		}
	}

	private ItemStack getItemStack(Object obj) {
		if(obj instanceof GergToolType) {
			return GergToolType.getAny((GergToolType) obj);
		}
		if(obj instanceof ItemStack) return ((ItemStack) obj).copy();
		if(obj instanceof Block) return new ItemStack((Block) obj);
		if(obj instanceof String) {
			List<ItemStack> ores = OreDictionary.getOres((String) obj);
			if(!ores.isEmpty()) return ores.get(0).copy();
		}
		return null;
	}

	public GergToolType[] getRequiredTools() {
		return tools;
	}

	public boolean isShapeless() {
		return isShapeless;
	}
}
