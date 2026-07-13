# Gregification — TODO

## ✅ Implemented

- [x] **GregToolType enum** — SCREWDRIVER, HAMMER, SAW, CUTTER, WELDING_TORCH
- [x] **ItemGregTool** — extends ItemCraftingDegradation, dual GregToolType + IToolable.ToolType; registry-based isToolOfType for non-ItemGregTool items (blowtorch)
- [x] **GregToolRecipe IRecipe** — shaped + shapeless, flexible tool placement via 'T' markers and remaining-slot matching, NBT-based variable durability damage
- [x] **Always-on mode** — no config toggle, system is baked into early-game NTNH
- [x] **GregRecipeHelper** — `addGregShaped`/`addGregShapeless` convenience methods with 'T' marker support
- [x] **GregToolNEIHandler** — separate "Gregified Crafting" NEI tab
- [x] **SCREWDRIVER items** — screwdriver (100), screwdriver_desh (unbreakable)
- [x] **HAMMER items** — hammer_iron (100), hammer_steel (250)
- [x] **SAW items** — saw_iron (100), saw_steel (250), saw_desh (unbreakable)
- [x] **CUTTER items** — cutter_iron (100), cutter_steel (250), cutter_desh (unbreakable)
- [x] **WELDING_TORCH items** — welding_torch (150), welding_torch_desh (unbreakable); blowtorch/acetylene_torch also registered
- [x] **RecipeSorter registration** — `hbm:gregtool` after shaped, before shapeless
- [x] **Gregified early-game recipes** — press, barrel_steel, crate_iron/steel, anvil_iron/lead, furnace_iron, electric_furnace, wood_burner, tank_steel, bucket, hopper, wrench

## ❌ Not Implemented

### Features

- [ ] **ShapedMirrored support** in GregToolRecipe
