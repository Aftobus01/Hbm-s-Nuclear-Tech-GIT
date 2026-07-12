# Gregification — TODO

## ✅ Implemented

- [x] **GregToolType enum** — SCREWDRIVER, HAMMER, SAW, CUTTER, WELDING_TORCH
- [x] **ItemGregTool** — extends ItemCraftingDegradation, dual GregToolType + IToolable.ToolType
- [x] **GregToolRecipe IRecipe** — shaped + shapeless, flexible tool placement in empty slots, NBT-based variable durability damage
- [x] **OEM auto-registration** — GregToolRecipe handles both modes internally (config-gated). Greg ON: tools required + durability damage. Greg OFF: pure OEM recipe.
- [x] **GregRecipeHelper** — `addGregShaped`/`addGregShapeless` convenience methods
- [x] **GregToolNEIHandler** — separate "Gregified Crafting" NEI tab, display adapts to mode
- [x] **Config** — `enableGregification` (default: true) under GREGIFICATION category
- [x] **SCREWDRIVER items** — 2 variants: screwdriver (100 durability) + desh screwdriver (unbreakable)
- [x] **HAMMER item** — iron hammer (100 durability)
- [x] **RecipeSorter registration** — `hbm:gregtool` after shaped, before shapeless
- [x] **Example recipe** — barrel_steel (4 steel plates + 2 steel ingots + screwdriver)

## ❌ Not Implemented

### Tools

- [ ] **SAW item** — registered in ModItems, added to GregToolType.SAW registry
- [ ] **CUTTER item** — registered in ModItems, added to GregToolType.CUTTER registry
- [ ] **WELDING_TORCH item** — registered in ModItems, added to GregToolType.WELDING_TORCH registry

### Features

- [ ] **ShapedMirrored support** in GregToolRecipe
