package com.hbm.handler.nei;

import java.util.ArrayList;
import java.util.List;

import com.hbm.crafting.handlers.GergToolRecipe;
import com.hbm.items.tool.GergToolType;

import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;

public class GergToolNEIHandler extends TemplateRecipeHandler {

	@Override
	public String getRecipeName() {
		return "Gergified Crafting";
	}

	@Override
	public String getGuiTexture() {
		return "textures/gui/container/crafting_table.png";
	}

	@Override
	public void loadCraftingRecipes(String outputId, Object... results) {
		if(outputId.equals("item") && getClass() == GergToolNEIHandler.class) {
			for(Object o : CraftingManager.getInstance().getRecipeList()) {
				if(o instanceof GergToolRecipe) {
					GergToolRecipe recipe = (GergToolRecipe) o;
					for(Object r : results) {
						if(r instanceof ItemStack && recipe.getRecipeOutput() != null && NEIServerUtils.areStacksSameTypeCrafting(recipe.getRecipeOutput(), (ItemStack) r)) {
							arecipes.add(new GergCachedRecipe(recipe));
							break;
						}
					}
				}
			}
		} else {
			super.loadCraftingRecipes(outputId, results);
		}
	}

	@Override
	public void loadCraftingRecipes(ItemStack result) {
		for(Object o : CraftingManager.getInstance().getRecipeList()) {
			if(o instanceof GergToolRecipe) {
				GergToolRecipe recipe = (GergToolRecipe) o;
				if(recipe.getRecipeOutput() != null && NEIServerUtils.areStacksSameTypeCrafting(recipe.getRecipeOutput(), result)) {
					arecipes.add(new GergCachedRecipe(recipe));
				}
			}
		}
	}

	@Override
	public void loadUsageRecipes(String inputId, Object... ingredients) {
		if(inputId.equals("item") && getClass() == GergToolNEIHandler.class) {
			for(Object o : CraftingManager.getInstance().getRecipeList()) {
				if(o instanceof GergToolRecipe) {
					GergToolRecipe recipe = (GergToolRecipe) o;
					ItemStack[] display = recipe.getDisplayRecipe();
					if(display != null) {
						for(ItemStack slot : display) {
							if(slot == null) continue;
							for(Object ingredient : ingredients) {
								if(ingredient instanceof ItemStack && NEIServerUtils.areStacksSameTypeCrafting(slot, (ItemStack) ingredient)) {
									arecipes.add(new GergCachedRecipe(recipe));
									break;
								}
							}
						}
					}
				}
			}
		} else {
			super.loadUsageRecipes(inputId, ingredients);
		}
	}

	@Override
	public void loadUsageRecipes(ItemStack ingredient) {
		for(Object o : CraftingManager.getInstance().getRecipeList()) {
			if(o instanceof GergToolRecipe) {
				GergToolRecipe recipe = (GergToolRecipe) o;
				ItemStack[] display = recipe.getDisplayRecipe();
				if(display != null) {
					for(ItemStack slot : display) {
						if(slot != null && NEIServerUtils.areStacksSameTypeCrafting(slot, ingredient)) {
							arecipes.add(new GergCachedRecipe(recipe));
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public int recipiesPerPage() {
		return 2;
	}

	private class GergCachedRecipe extends CachedRecipe {

		private final List<PositionedStack> ingredients = new ArrayList<>();
		private PositionedStack result;

		public GergCachedRecipe(GergToolRecipe recipe) {
			ItemStack[] display = recipe.getDisplayRecipe();
			if(display != null) {
				for(int i = 0; i < 9; i++) {
					if(display[i] != null) {
						ingredients.add(new PositionedStack(display[i], 25 + (i % 3) * 18, 6 + (i / 3) * 18));
					}
				}
			}
			this.result = new PositionedStack(recipe.getRecipeOutput(), 119, 24);
		}

		@Override
		public List<PositionedStack> getIngredients() {
			return getCycledIngredients(cycleticks / 48, ingredients);
		}

		@Override
		public PositionedStack getResult() {
			return result;
		}
	}
}
