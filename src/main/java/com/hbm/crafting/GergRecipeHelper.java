package com.hbm.crafting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.hbm.crafting.handlers.GergToolRecipe;
import com.hbm.items.tool.GergToolType;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemStack;

public class GergRecipeHelper {

	public static void addGergShaped(ItemStack result, Object[] recipe, GergToolType... tools) {
		addGergShaped(result, recipe, 0, tools);
	}

	public static void addGergShaped(ItemStack result, Object[] recipe, int minTier, GergToolType... tools) {
		addGergShaped0(result, recipe, false, minTier, tools);
	}

	public static void addGergShapedMirrored(ItemStack result, Object[] recipe, GergToolType... tools) {
		addGergShapedMirrored(result, recipe, 0, tools);
	}

	public static void addGergShapedMirrored(ItemStack result, Object[] recipe, int minTier, GergToolType... tools) {
		addGergShaped0(result, recipe, true, minTier, tools);
	}

	private static void addGergShaped0(ItemStack result, Object[] recipe, boolean mirrored, int minTier, GergToolType... tools) {
		List<String> rows = new ArrayList<>();
		int idx = 0;
		while(idx < recipe.length && recipe[idx] instanceof String) {
			rows.add((String) recipe[idx]);
			idx++;
		}

		int height = rows.size();
		int width = 0;
		for(String row : rows) {
			if(row.length() > width) width = row.length();
		}

		HashMap<Character, Object> charMap = new HashMap<>();
		while(idx < recipe.length) {
			char key = (Character) recipe[idx++];
			Object value = recipe[idx++];
			charMap.put(key, value);
		}

		Object[] inputs = new Object[9];
		Arrays.fill(inputs, null);

		for(int r = 0; r < height; r++) {
			String row = rows.get(r);
			for(int c = 0; c < row.length(); c++) {
				char ch = row.charAt(c);
				if(ch != ' ') {
					Object ing = charMap.get(ch);
					inputs[r * 3 + c] = ing;
				}
			}
		}

		GergToolRecipe gergRecipe = new GergToolRecipe(result, width, height, inputs, minTier, tools);
		if(mirrored) gergRecipe.setMirrored(true);
		GameRegistry.addRecipe(gergRecipe);
	}

	public static void addGergShapeless(ItemStack result, Object[] inputs, GergToolType... tools) {
		addGergShapeless(result, inputs, 0, tools);
	}

	public static void addGergShapeless(ItemStack result, Object[] inputs, int minTier, GergToolType... tools) {
		List<Object> inputList = new ArrayList<>();
		for(Object o : inputs) {
			if(o != null) inputList.add(o);
		}
		GameRegistry.addRecipe(new GergToolRecipe(result, inputList, minTier, tools));
	}
}
