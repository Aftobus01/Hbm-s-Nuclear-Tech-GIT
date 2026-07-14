package com.hbm.items.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public enum GergToolType {
	SCREWDRIVER,
	HAMMER,
	SAW,
	CUTTER,
	WELDING_TORCH,
	WRENCH;

	private static final Map<GergToolType, List<ItemStack>> registry = new HashMap<>();
	private static final Map<Item, Integer> tierMap = new HashMap<>();

	public void register(ItemStack stack) {
		register(stack, 0);
	}

	public void register(ItemStack stack, int tier) {
		registry.computeIfAbsent(this, k -> new ArrayList<>()).add(stack);
		tierMap.put(stack.getItem(), tier);
	}

	public static ItemStack getAny(GergToolType type) {
		return getAny(type, 0);
	}

	public static ItemStack getAny(GergToolType type, int minTier) {
		List<ItemStack> stacks = registry.get(type);
		if(stacks != null && !stacks.isEmpty()) {
			for(ItemStack stack : stacks) {
				if(getTier(stack) >= minTier) {
					return stack.copy();
				}
			}
			return stacks.get(0).copy();
		}
		return null;
	}

	public static int getTier(ItemStack stack) {
		if(stack == null || stack.getItem() == null) return 0;
		if(stack.getItem() instanceof ItemGergTool) {
			return ((ItemGergTool) stack.getItem()).getTier();
		}
		return tierMap.getOrDefault(stack.getItem(), 0);
	}

	public static boolean isToolOfType(ItemStack stack, GergToolType type) {
		if(stack == null || stack.getItem() == null) return false;
		if(stack.getItem() instanceof ItemGergTool) {
			return ((ItemGergTool) stack.getItem()).getGergType() == type;
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
