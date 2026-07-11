package com.hbm.crafting;

import com.hbm.crafting.handlers.GregToolRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.tool.GregToolType;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.hbm.inventory.OreDictManager.*;

public class GregRecipeRegistry {

	public static void register() {

		addGregShaped(new ItemStack(ModItems.ducttape, 4),
				new Object[] { "F", "P", "S",
						'F', Items.string,
						'P', Items.paper,
						'S', "slimeball" },
				GregToolType.SCREWDRIVER);

		addGregShaped(new ItemStack(ModItems.plate_polymer, 4),
				new Object[] { "SWS",
						'S', Items.string,
						'W', Blocks.wool },
				GregToolType.SCREWDRIVER);

		addGregShaped(new ItemStack(ModItems.cell_empty, 6),
				new Object[] { " S ", "G G", " S ",
						'S', STEEL.plate(),
						'G', "paneGlass" },
				GregToolType.SCREWDRIVER);

		addGregShaped(new ItemStack(ModItems.bolt_spike, 2),
				new Object[] { "BB", "B ", "B ",
						'B', STEEL.bolt() },
				GregToolType.SCREWDRIVER);
	}

	public static void addGregShaped(ItemStack result, Object[] recipe, GregToolType... tools) {
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
				if(ch == ' ') {
					inputs[r * 3 + c] = null;
				} else {
					Object ing = charMap.get(ch);
					inputs[r * 3 + c] = ing;
				}
			}
		}

		GameRegistry.addRecipe(new GregToolRecipe(result, width, height, inputs, tools));
	}

	public static void addGregShapeless(ItemStack result, Object[] inputs, GregToolType... tools) {
		List<Object> inputList = new ArrayList<>();
		for(Object o : inputs) {
			if(o != null) inputList.add(o);
		}
		GameRegistry.addRecipe(new GregToolRecipe(result, inputList, tools));
	}
}
