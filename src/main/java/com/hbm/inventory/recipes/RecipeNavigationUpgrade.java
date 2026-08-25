package com.hbm.inventory.recipes;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;

/**
 * Shapeless recipe: navigationUpgrade + filled_map -> navigationUpgrade with updated map NBT.
 * Mirrors OC's ExtendedRecipe logic for navigationUpgrade (oc:data/oc:map).
 */
public class RecipeNavigationUpgrade implements IRecipe {

    private ItemStack getNavigationUpgradeStack() {
        // Try ore dict first (correct damage regardless of OC version)
        for(ItemStack stack : OreDictionary.getOres("oc:navigationUpgrade")) {
            return stack.copy();
        }
        // Fallback via GameRegistry damage 40 (delegate 35) - common for GTNH OC
        Item ocItem = GameRegistry.findItem("OpenComputers", "item");
        if(ocItem != null) {
            // Try common damages for navigation upgrade across OC versions
            int[] candidates = new int[]{35, 36, 40, 41};
            for(int dmg : candidates) {
                ItemStack test = new ItemStack(ocItem, 1, dmg);
                // Check unlocalized name contains navigationUpgrade
                try {
                    String name = test.getUnlocalizedName();
                    if(name != null && name.toLowerCase().contains("navigation")) {
                        return test;
                    }
                } catch(Throwable t) {}
            }
            return new ItemStack(ocItem, 1, 35);
        }
        return null;
    }

    private boolean isNavigationUpgrade(ItemStack stack) {
        if(stack == null) return false;
        // Check ore dict
        for(ItemStack ore : OreDictionary.getOres("oc:navigationUpgrade")) {
            if(ore.getItem() == stack.getItem() && ore.getItemDamage() == stack.getItemDamage()) return true;
        }
        // Fallback via delegated check
        if(Loader.isModLoaded("OpenComputers")) {
            Item ocItem = GameRegistry.findItem("OpenComputers", "item");
            if(ocItem != null && stack.getItem() == ocItem) {
                // navigation upgrade damages vary, but we can check via unlocalized name
                String unlocalized = stack.getUnlocalizedName();
                if(unlocalized != null && unlocalized.toLowerCase().contains("navigation")) return true;
                // Also try candidates
                int dmg = stack.getItemDamage();
                // delegate 35 is navigation in vanilla GTNH OC
                if(dmg == 35 || dmg == 36 || dmg == 40) return true;
            }
        }
        return false;
    }

    private boolean isFilledMap(ItemStack stack) {
        return stack != null && stack.getItem() == Items.filled_map;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        int navCount = 0;
        int mapCount = 0;
        int other = 0;
        for(int i=0;i<inv.getSizeInventory();i++) {
            ItemStack s = inv.getStackInSlot(i);
            if(s == null) continue;
            if(isNavigationUpgrade(s)) navCount++;
            else if(isFilledMap(s)) mapCount++;
            else other++;
        }
        return navCount == 1 && mapCount == 1 && other == 0;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack nav = null;
        ItemStack map = null;
        for(int i=0;i<inv.getSizeInventory();i++) {
            ItemStack s = inv.getStackInSlot(i);
            if(s == null) continue;
            if(isNavigationUpgrade(s)) nav = s;
            else if(isFilledMap(s)) map = s;
        }
        if(nav == null || map == null) return null;
        ItemStack out = nav.copy();
        out.stackSize = 1;
        if(!out.hasTagCompound()) out.setTagCompound(new NBTTagCompound());
        NBTTagCompound root = out.getTagCompound();
        if(!root.hasKey("oc:data")) root.setTag("oc:data", new NBTTagCompound());
        NBTTagCompound data = root.getCompoundTag("oc:data");
        // Store map as in OC ExtendedRecipe: data.setNewCompoundTag("oc:map", map.writeToNBT)
        NBTTagCompound mapTag = new NBTTagCompound();
        map.writeToNBT(mapTag);
        data.setTag("oc:map", mapTag);
        return out;
    }

    @Override
    public int getRecipeSize() {
        return 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        ItemStack nav = getNavigationUpgradeStack();
        if(nav != null) {
            nav.stackSize = 1;
            return nav;
        }
        return null;
    }
}
