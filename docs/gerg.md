# Gerg System — Java Usage

`GergRecipeHelper` in `com.hbm.crafting.GergRecipeHelper` adds tool-based crafting recipes.

## Shaped

```java
GergRecipeHelper.addGergShaped(new ItemStack(YourItem, count), new Object[] {
    "IPI",
    "I I",
    "IPI",
    'I', STEEL.plate(),
    'P', STEEL.ingot()
}, GergToolType.SCREWDRIVER);
```

## Shaped with minimum tier

```java
GergRecipeHelper.addGergShaped(new ItemStack(YourItem, count), new Object[] {
    "III",
    "IDI",
    "III",
    'I', IRON.ingot(),
    'D', "gemDiamond"
}, 2 /* minTier */, GergToolType.HAMMER);
```

## Mirrored Shaped

```java
GergRecipeHelper.addGergShapedMirrored(new ItemStack(YourItem, count), new Object[] {
    " RI",
    "IPI",
    "IB ",
    'R', Blocks.furnace,
    'I', IRON.ingot(),
    'P', Blocks.piston,
    'B', IRON.block()
}, GergToolType.HAMMER, GergToolType.SCREWDRIVER);
```

## Shapeless

```java
GergRecipeHelper.addGergShapeless(new ItemStack(YourItem, count), new Object[] {
    ModItems.some_ingredient,
    "ingotIron"
}, GergToolType.CUTTER);
```

## Available tool types

- `GergToolType.SCREWDRIVER`
- `GergToolType.HAMMER`
- `GergToolType.SAW`
- `GergToolType.CUTTER`
- `GergToolType.WELDING_TORCH`
- `GergToolType.WRENCH`

## Tier system

Each tool has an integer tier. Recipes can specify a `minTier` parameter (default `0`) — only tools with tier >= `minTier` will satisfy the requirement. Known tiers:

| Tier | Example tools |
|------|--------------|
| 0    | Default (no explicit tier) |
| 1    | Iron tools |
| 2    | Steel tools |
| 9    | Desh tools (unbreakable) |

## Durability damage

Tools take durability damage each time they are used in a gerg recipe. The damage is calculated as:

```
damage = max(1, uniqueIngredients + outputStackSize)
```

- **Unique ingredients**: number of distinct ingredient types in the recipe
- **Output stack size**: how many items the recipe produces

This means more complex recipes (more ingredients, larger output) consume more durability. The damage value is written to the tool's NBT under the `"gergDurabilityDamage"` key. Welding torches (blowtorch/acetylene torch) scale their fuel consumption by this same factor (`5 * damage` gas for blowtorch, `2 * damage` of each fluid for acetylene torch).

## Multi-tool

The Gerg Multi-Tool (`multitool_gerg`) acts as `SCREWDRIVER`, `SAW`, and `CUTTER` simultaneously, reducing inventory clutter for advanced players.
