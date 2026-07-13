package com.hbm.crafting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.hbm.crafting.handlers.GregToolRecipe;
import com.hbm.items.tool.GregToolType;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemStack;

public class GregRecipeHelper {

	public static void addGregShaped(ItemStack result, Object[] recipe, GregToolType... tools) {
		addGregShaped0(result, recipe, false, tools);
	}

	public static void addGregShapedMirrored(ItemStack result, Object[] recipe, GregToolType... tools) {
		addGregShaped0(result, recipe, true, tools);
	}

	private static void addGregShaped0(ItemStack result, Object[] recipe, boolean mirrored, GregToolType... tools) {
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

		GregToolRecipe gregRecipe = new GregToolRecipe(result, width, height, inputs, tools);
		if(mirrored) gregRecipe.setMirrored(true);
		GameRegistry.addRecipe(gregRecipe);
	}

	public static void addGregShapeless(ItemStack result, Object[] inputs, GregToolType... tools) {
		List<Object> inputList = new ArrayList<>();
		for(Object o : inputs) {
			if(o != null) inputList.add(o);
		}
		GameRegistry.addRecipe(new GregToolRecipe(result, inputList, tools));
	}
}
