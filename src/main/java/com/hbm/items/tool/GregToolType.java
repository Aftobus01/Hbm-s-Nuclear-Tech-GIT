package com.hbm.items.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

public enum GregToolType {
	SCREWDRIVER,
	HAMMER,
	SAW,
	CUTTER,
	WELDING_TORCH;

	private static final Map<GregToolType, List<ItemStack>> registry = new HashMap<>();

	public void register(ItemStack stack) {
		registry.computeIfAbsent(this, k -> new ArrayList<>()).add(stack);
	}

	public static ItemStack getAny(GregToolType type) {
		List<ItemStack> stacks = registry.get(type);
		if(stacks != null && !stacks.isEmpty()) {
			return stacks.get(0).copy();
		}
		return null;
	}

	public static boolean isToolOfType(ItemStack stack, GregToolType type) {
		if(stack == null || stack.getItem() == null) return false;
		if(stack.getItem() instanceof ItemGregTool) {
			return ((ItemGregTool) stack.getItem()).getGregType() == type;
		}
		List<ItemStack> registered = registry.get(type);
		if(registered != null) {
			for(ItemStack s : registered) {
				if(s.getItem() == stack.getItem()) return true;
			}
		}
		return false;
	}
}
