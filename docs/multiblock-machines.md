# Multiblock Machine Architecture

## Table of Contents

1. [Overview & Architecture](#1-overview--architecture)
2. [Block Layer — BlockDummyable](#2-block-layer--blockdummyable)
3. [Multiblock Structure Validation](#3-multiblock-structure-validation)
4. [Proxy System](#4-proxy-system)
5. [Module Processing System](#5-module-processing-system)
6. [Generic Recipe System](#6-generic-recipe-system)
7. [Assembly Machine Deep Dive](#7-assembly-machine-deep-dive)
8. [Chemical Plant Deep Dive](#8-chemical-plant-deep-dive)
9. [Machine Catalog](#9-machine-catalog)
10. [Special Structure Patterns](#10-special-structure-patterns)
11. [Data Flow & Sync](#11-data-flow--sync)
12. [Extending the System](#12-extending-the-system)

---

## 1. Overview & Architecture

Multiblock machines in HBM's Nuclear Tech Mod follow a four-layer architecture that cleanly separates concerns:

```
Block Layer        → BlockDummyable subclasses (placement, structure validation, GUI opening)
Tile Entity Layer  → TileEntityMachineBase subclasses (inventory, power, fluid, ticking)
Module Layer       → ModuleMachineBase subclasses (processing logic, slot/tank mapping)
Recipe Layer       → GenericRecipes subclasses (recipe definitions, serialization, lookup)
```

The system has two tiers:

| Tier | Single-module | Multi-module (Factory) |
|------|---------------|----------------------|
| Assembly | `MachineAssemblyMachine` + `TileEntityMachineAssemblyMachine` | `MachineAssemblyFactory` + `TileEntityMachineAssemblyFactory` (4 modules) |
| Chemical | `MachineChemicalPlant` + `TileEntityMachineChemicalPlant` | `MachineChemicalFactory` + `TileEntityMachineChemicalFactory` (4 modules) |
| Structure | 3 tall, 3×3 footprint | 3 tall, 5×5 footprint |
| Extra dummies | 8 surrounding y=0 positions | Dynamic proxy delegates |

All new multiblocks extend `BlockDummyable`, which provides placement, dummy block metadata management, structure validation, orphan detection, and GUI opening. Dummy positions use the same block ID as the core, differentiated by metadata ranges. Proxy TileEntities on extra dummies delegate API calls (power, fluid, inventory) back to the core TE.

---

## 2. Block Layer — BlockDummyable

`BlockDummyable` extends `BlockContainer` and provides the foundation for all multiblock machines. It handles:

- Placement and core position calculation
- Structure requirement checking
- Dummy block placement and metadata management
- GUI opening via `standardOpenBehavior`
- Orphan detection and destruction

### Metadata Convention

Metadata encodes three distinct states using four ranges:

| Range | Meaning |
|-------|---------|
| `0–5` | Dummy blocks: `ForgeDirection` ordinal pointing **toward** the core block. Used for orphan neighbor checks. |
| `6–11` | "Extra" dummies: Same as 0–5 but with the `extra = 6` flag added. Enables external IO connections (pipes, cables) on specific faces. Created via `makeExtra()`. |
| `12–15` | Core blocks: `direction + offset(10)` where direction is the machine's facing rotation. Has a TileEntity. |

The core block has metadata `≥ 12`, giving `ForgeDirection.UNKNOWN` when read, which is how `findCoreRec()` identifies it.

```java
public static final int offset = 10;
public static final int extra = 6;
```

### Core Position Calculation (`onBlockPlacedBy`)

When a player places a multiblock block against a face:

1. The placed block is immediately removed (`setBlockToAir`)
2. The player's facing direction is determined from `rotationYaw`
3. If placed on a wall (`UP`/`DOWN` excluded), the facing direction is the placement side
4. The core position is offset backward by `-getOffset()` from the placement position
5. `getDirModified()` can override the direction (used to restrict rotations)
6. For wall placements, the offset is adjusted by the rotated dimensions to push the core outward
7. `checkRequirement()` validates the volume; if it fails, the item is returned to the player
8. On success, the core block is placed with metadata `dir.ordinal() + offset`
9. `fillSpace()` populates the dummy blocks
10. `IPersistentNBT.restoreData()` restores any stored NBT data (from wrenching)

---

## 3. Multiblock Structure Validation

### Dimension Arrays

Every multiblock defines its size with a six-element array:

```java
int[] dimensions = {up, down, north, south, west, east};
```

| Machine | Dimensions | Offset | Volume |
|---------|-----------|--------|--------|
| Assembly Machine | `{2, 0, 1, 1, 1, 1}` | 1 | 3 tall, 3×3 footprint (9 blocks + core) |
| Chemical Plant | `{2, 0, 1, 1, 1, 1}` | 1 | Same |
| Assembly Factory | `{2, 0, 2, 2, 2, 2}` | 2 | 3 tall, 5×5 footprint |
| Chemical Factory | `{2, 0, 2, 2, 2, 2}` | 2 | Same |

The **offset** controls how far the structure core is from the placement block along the facing direction.

### Structure Checking

`MultiblockHandlerXR.checkSpace()` validates the entire volume:

1. The dimension array is rotated to match the machine's facing direction via `rotate()`
2. A 3D bounding box is computed: `x - rot[4]` to `x + rot[5]`, `y - rot[1]` to `y + rot[0]`, `z - rot[2]` to `z + rot[3]`
3. Every block in this volume must be replaceable (air, grass, fluids, etc.)
4. A hard cap of 2000 blocks prevents runaway checks

### Rotation

`MultiblockHandlerXR.rotate()` maps the dimension array to different cardinal directions:

```java
// When facing SOUTH: dimensions are used as-is
// When facing NORTH: north↔south, west↔east swapped
// When facing EAST:  north→west, south→east, west→south, east→north
// When facing WEST:  north→east, south→west, west→north, east→south
```

### Filling Space

`fillSpace()` iterates the same volume and places dummy blocks with metadata pointing toward the core:

```java
// Downward blocks → ForgeDirection.DOWN.ordinal()
// Upward blocks   → ForgeDirection.UP.ordinal()
// Westward blocks → ForgeDirection.WEST.ordinal()
// etc.
```

The core position itself is skipped (`continue`).

### Extra Dummies

`makeExtra()` upgrades a regular dummy (meta 0–5) to an extra dummy (meta + 6). This flags the position as IO-capable, enabling external pipe/cable connections. Both Assembly Machine and Chemical Plant override `fillSpace()` to call `makeExtra()` on all 8 surrounding ground-level positions:

```java
for(int i = -1; i <= 1; i++) for(int j = -1; j <= 1; j++) {
    if(i != 0 || j != 0) this.makeExtra(world, x + i, y, z + j);
}
```

### Core Resolution

`findCoreRec()` walks from any dummy block back to the core using recursive backtracking:

1. Read metadata from the current position
2. Strip the `extra` flag if present
3. If the block at this position is of the same type and `ForgeDirection.getOrientation(metadata) == ForgeDirection.UNKNOWN`, it's the core
4. Otherwise, get the opposite direction from metadata and step one block that way
5. Track visited positions in a list to prevent infinite loops

### Orphan Protection

Both `onNeighborBlockChange()` and `updateTick()` call `destroyIfOrphan()`. This checks if the neighbor the dummy points toward is still of the same block type. If not, the dummy is removed. The `safeRem` flag suppresses this during controlled placement/destruction.

---

## 4. Proxy System

Dummy blocks use the **Proxy pattern** to delegate all API calls back to the core Tile Entity.

### TileEntityProxyBase

The base class provides `getTE()` which calls `BlockDummyable.findCore()` to locate the core block, then retrieves the TileEntity from that position. It also has a fallback for blocks implementing `IProxyController` (used by non-BlockDummyable machines with proxy blocks):

```java
// In TileEntityProxyBase.getTE():
if(this.getBlockType() instanceof IProxyController) {
    IProxyController controller = (IProxyController) this.getBlockType();
    TileEntity tile = controller.getCore(worldObj, xCoord, yCoord, zCoord);
    if(tile != null && !(tile instanceof TileEntityProxyBase)) return tile;
}
```

### TileEntityProxyCombo

Extends `ProxyBase` and implements **eight API interfaces**:

| Interface | Purpose |
|-----------|---------|
| `IEnergyReceiverMK2` | Power reception |
| `IEnergyConductorMK2` | Power conduction |
| `ISidedInventory` | Item access (pipes/hoppers) |
| `IFluidReceiverMK2` | Fluid reception |
| `IHeatSource` | Heat transfer |
| `ICrucibleAcceptor` | Molten metal casting |
| `SimpleComponent` / `OCComponent` | OpenComputers integration |
| `IRORValueProvider` / `IRORInteractive` | Redstone Over Radio |

Each capability is gated by a boolean flag set at construction time. For Assembly Machine and Chemical Plant, dummies are created with:

```java
new TileEntityProxyCombo().inventory().power().fluid();
```

This enables item, energy, and fluid access on all extra dummy positions without requiring the player to target the core block.

### TileEntityProxyDyn

Used by Factory (4×) machines. Extends `ProxyCombo` and uses `IProxyDelegateProvider` to return different API surfaces depending on the dummy's position. This enables:
- Recipe IO dummies (allow only recipe-related fluid/item access)
- Coolant dummies (allow only water/coolant fluid access)

Both delegate types ultimately forward to the same core TE, but enforce slot/tank filtering per-position.

### Other Proxy Variants

| Proxy | Purpose | Notes |
|-------|---------|-------|
| `TileEntityProxyEnergy` | Power-only delegation | Implements `IEnergyReceiverMK2` |
| `TileEntityProxyInventory` | Inventory-only delegation | `@Deprecated`, implements `ISidedInventory` |
| `TileEntityProxyConductor` | Power conduction-only | Implements `IEnergyConnectorMK2` |

These are simpler proxies used when only a single capability needs to be exposed at a dummy position, rather than the full combo proxy.

### Position-Dependent Slot Access (IConditionalInvAccess)

`TileEntityProxyCombo` checks if the core TE implements `IConditionalInvAccess`. If it does, the proxy passes its own `(x, y, z)` position to the core's slot-access methods instead of delegating generically. This allows Factory machines and other multi-proxy machines to expose different slots depending on which dummy position is being accessed:

```java
// In TileEntityProxyCombo:
if(getCoreObject() instanceof IConditionalInvAccess)
    return ((IConditionalInvAccess) getCoreObject())
        .getAccessibleSlotsFromSide(xCoord, yCoord, zCoord, side);
```

Used by `TileEntityMachineAssemblyFactory`, `TileEntityMachineChemicalFactory`, `TileEntityMachineCyclotron`, `TileEntityMachineRotaryFurnace`, and others.

---

## 5. Module Processing System

The `ModuleMachineBase` abstract class encapsulates all processing logic for a single "recipe slot". This design allows the same code to handle both single-module (Assembly Machine, Chemical Plant) and multi-module (Factory) machines.

### Module Structure

```java
public abstract class ModuleMachineBase {
    // Configuration (set via builder methods)
    public int index;                    // Module index (0 for single, 0-3 for factory)
    public IEnergyHandlerMK2 battery;    // Power source reference
    public ItemStack[] slots;            // TE's inventory array
    public int[] inputSlots;             // Indices into slots
    public int[] outputSlots;            // Indices into slots
    public FluidTank[] inputTanks;       // Input fluid tanks
    public FluidTank[] outputTanks;      // Output fluid tanks

    // Runtime state
    public String recipe;                // Current recipe name
    public double progress;              // Progress toward completion (0.0 - 1.0)

    // Return signals
    public boolean didProcess;           // True if processing happened this tick
    public boolean markDirty;            // True if inventory was modified
}
```

### Module Implementations

| Module | Input Slots | Output Slots | Input Tanks | Output Tanks | Used By |
|--------|-------------|--------------|-------------|--------------|---------|
| `ModuleMachineAssembler` | 12 | 1 | 1 | 1 | Assembly Machine, Assembly Factory |
| `ModuleMachineChemplant` | 3 | 3 | 3 | 3 | Chemical Plant, Chemical Factory |
| `ModuleMachineFusion` | 0 | 1 | 3 | 1 | Fusion Torus |

### ModuleMachineAssembler

- 12 input slots, 1 output slot, 1 input tank, 1 output tank
- Overrides `setupTanks()` to dynamically resize tanks (max of current fill or `recipe_fluid_amount × 2`, minimum 4000 mB)

```java
// Configuration via builder pattern:
new ModuleMachineAssembler(0, this, slots)
    .itemInput(4)          // input starts at slot 4 (occupies slots 4-15)
    .itemOutput(16)        // output at slot 16
    .fluidInput(inputTank)
    .fluidOutput(outputTank);
```

### ModuleMachineChemplant

- 3 input slots, 3 output slots, 3 input tanks, 3 output tanks

```java
new ModuleMachineChemplant(0, this, slots)
    .itemInput(4, 5, 6)
    .itemOutput(7, 8, 9)
    .fluidInput(inputTanks[0], inputTanks[1], inputTanks[2])
    .fluidOutput(outputTanks[0], outputTanks[1], outputTanks[2]);
```

### ModuleMachineFusion

Used by the Fusion Torus. Has a unique processing lifecycle:
- No item inputs (inputs are fluid-only: 3 input tanks for D/T/He3)
- Continuously consumes input fluids each tick (not just on completion)
- Has a `bonus`/`bonusSpeed` system for over-unity production bonuses
- Does not use the standard `canProcess` → `process` two-step; instead `preUpdate()` sets speed parameters before `update()` is called

```java
new ModuleMachineFusion(0, this, slots)
    .itemOutput(0)
    .fluidInput(inputTanks[0], inputTanks[1], inputTanks[2])
    .fluidOutput(outputTanks[0]);
```

### Processing Lifecycle

The `update()` method orchestrates the full lifecycle each tick:

```
update(speed, power, extraCondition, blueprint)
    │
    ├─ 1. Get recipe by name from recipeSet.recipeNameMap
    │
    ├─ 2. Blueprint gate check:
    │      If recipe.isPooled() and !recipe.isPartOfPool(blueprintPool)
    │      → Reset recipe to "null", clear progress
    │
    ├─ 3. setupTanks(recipe):
    │      Conform tank types/pressures to recipe requirements
    │
    ├─ 4. canProcess(recipe, speed, power):
    │      ├─ Auto-switch: Check if first input slot item matches a different
    │      │   recipe in the same autoSwitchGroup → switch recipe
    │      ├─ Power check: battery >= recipe.power × power_mult
    │      ├─ hasInput(): All input slots match recipe AStack, all tanks have fluid
    │      └─ canFitOutput(): Output slots are empty or can stack, tanks have room
    │
    └─ 5. process(recipe, speed, power) [if canProcess returned true]:
           ├─ Deduct power: battery -= recipe.power × power_mult
           ├─ Advance progress: progress += min(speed / recipe.duration, 1.0)
           └─ On completion (progress >= 1.0):
                ├─ consumeInput(): Decrease item stack sizes, decrease fluid fills
                ├─ produceItem(): Add output items/fluids
                ├─ If can still process → keep fractional overflow progress
                └─ Else → reset progress to 0
```

### Auto-Switch Mechanism

Recipes in the same `autoSwitchGroup` can change automatically when the first input slot item changes:

```java
if(recipe.autoSwitchGroup != null && inputSlots.length > 0 && slots[inputSlots[0]] != null) {
    for(GenericRecipe nextRec : autoSwitchGroup) {
        if(nextRec.inputItem[0].matchesRecipe(itemToSwitchBy, true)) {
            this.recipe = nextRec.getInternalName();
            return false; // defer processing one tick
        }
    }
}
```

This enables workflows where changing the first ingredient (e.g., iron ingot → gold ingot) automatically selects the correct recipe.

---

## 6. Generic Recipe System

### GenericRecipe (POJO)

A single recipe definition:

```java
public class GenericRecipe {
    String name;               // Internal name (e.g., "ass.plateiron")
    AStack[] inputItem;        // Item inputs (ComparableStack, OreDictStack, or NBTStack)
    FluidStack[] inputFluid;   // Fluid inputs (type, amount, pressure)
    IOutput[] outputItem;      // Item outputs (ChanceOutput or ChanceOutputMulti)
    FluidStack[] outputFluid;  // Fluid outputs
    int duration;              // Ticks to complete
    long power;                // HE/t consumption
    String[] blueprintPools;   // Required blueprints (null = always available)
    String autoSwitchGroup;    // Auto-switch group name

    // Builder methods for fluid recipe registration:
    GenericRecipe.setup(duration, power)
        .inputItems(AStack...)
        .inputFluids(FluidStack...)
        .outputItems(IOutput...)
        .outputFluids(FluidStack...)
        .setPools("alt.plates", "discover.x")
        .setGroup("autoswitch.plates", recipeSet)
}
```

### GenericRecipes<T>

Manages collections of recipes:

```java
public abstract class GenericRecipes<T extends GenericRecipe> extends SerializableRecipe {
    List<T> recipeOrderedList;                       // Ordered list for iteration
    HashMap<String, T> recipeNameMap;                // Name → recipe lookup
    HashMap<String, List<String>> blueprintPools;    // Pool name → recipe names
    HashMap<String, List<GenericRecipe>> autoSwitchGroups;
}
```

### Blueprint Pools

Recipes can be gated behind blueprint items. Pools use four prefixes:

| Prefix | Purpose |
|--------|---------|
| `alt.` | Alternate recipes (obtainable through other means) |
| `discover.` | Discoverable (no other source) |
| `secret.` | Hidden recipes |
| `528.` | 528 greyprint system |

If a recipe has `blueprintPools` set, the machine requires a blueprint item in the blueprint slot whose pools overlap with the recipe's pools.

### Serialization

`SerializableRecipe` provides:
- JSON read/write via Gson
- Template generation (prefixed with `_`)
- Network sync (used for recipe synchronization)
- File management in the `hbmRecipes/` config directory

Each recipe type has its own JSON file:
- `hbmAssemblyMachine.json` for `AssemblyMachineRecipes`
- `hbmChemicalPlant.json` for `ChemicalPlantRecipes`
- `hbmFusionRecipes.json` for `FusionRecipes`

### Output Types

`IOutput` has two implementations:

- **`ChanceOutput`**: Always produces a stack, optionally with a per-item chance
  ```java
  new ChanceOutput(new ItemStack(Items.iron_ingot))          // 100%
  new ChanceOutput(new ItemStack(Items.diamond), 0.25F)      // 25% chance
  ```

- **`ChanceOutputMulti`**: Weighted random selection from a pool
  ```java
  new ChanceOutputMulti(
      new ItemStack(Items.iron_ingot), 10,   // weight 10
      new ItemStack(Items.gold_ingot),  5,   // weight 5
      new ItemStack(Items.diamond),     1    // weight 1
  )
  ```

---

## 7. Assembly Machine Deep Dive

### Block: MachineAssemblyMachine

```java
@Override public int[] getDimensions() { return new int[] {2, 0, 1, 1, 1, 1}; }
@Override public int getOffset() { return 1; }
```

`createNewTileEntity` creates:
- Core (meta ≥ 12): `TileEntityMachineAssemblyMachine`
- Extra dummies (meta 6–11): `TileEntityProxyCombo` with inventory, power, and fluid capabilities
- Regular dummies (meta 0–5): No TileEntity (returns null)

### Tile Entity: TileEntityMachineAssemblyMachine

**Capacity:** 17 slots, 1 fluid tank (4000 mB input), 1 fluid tank (4000 mB output)

**Slot layout:**

| Slot(s) | Purpose |
|---------|---------|
| 0 | Battery (power source) |
| 1 | Blueprint (recipe gating) |
| 2–3 | Upgrades (Speed/Power/Overdrive, max level 3 each) |
| 4–15 | Input grid (12 slots, unordered) |
| 16 | Output |

**Server tick (`updateEntity`):**

1. **Power**: Recipe dictates `maxPower = recipe.power × 100` (minimum 100k HE). Battery item is charged via `Library.chargeTEFromItems()`.
2. **Fluid IO**: 12 connection positions (2 blocks out on each axis, 3 y-levels around center). Subscribes power and input fluid, provides output fluid.
3. **Upgrades**: `UpgradeManagerNT` checks slots 2–3 each tick.
4. **Speed/power multipliers**:
   ```java
   speed = 1.0 + (SPEED_level / 3.0) + OVERDRIVE_level
   power = 1.0 - (POWER_level × 0.25) + (SPEED_level × 1.0) + (OVERDRIVE_level × 10/3)
   ```
5. **Module**: `assemblerModule.update(speed, pow, true, slots[1])`
6. **Meteorite sword easter egg**: If slot 0 contains `meteorite_sword_alloyed` while processing, it transforms into `meteorite_sword_machined`.
7. **Network sync**: `networkPackNT(100)` every 100 ticks (5 seconds).

**Client tick:**

- Frame detection: every 20 ticks, checks if `y+3` is non-air (cosmetic frame model)
- Audio loop management (`ELECTRIC_MOTOR_LOOP`)
- Arm animation: `AssemblerArm` inner class manages 4-DOF arm positions (pivot, arm, piston, striker) with random positioning and smooth interpolation
- Ring animation: rotational ring that targets random angles with acceleration/deceleration

### Container: ContainerMachineAssemblyMachine

Extends `ContainerBase`, 176×256 GUI size. Shift-click behavior routes items to appropriate slot groups.

### GUI: GUIMachineAssemblyMachine

Renders:
- Power bar (top left)
- Progress bar with recipe-based coloring
- LED indicators showing input item/fluid requirements
- Fluid tank renderer
- Recipe selector button → opens `GUIScreenRecipeSelector`
- Ghost-rendered input items in the 12 input slots (shows what the recipe expects)

---

## 8. Chemical Plant Deep Dive

### Block: MachineChemicalPlant

Structurally identical to Assembly Machine:
```java
@Override public int[] getDimensions() { return new int[] {2, 0, 1, 1, 1, 1}; }
@Override public int getOffset() { return 1; }
```

Same `fillSpace` override for extra dummies; same proxy setup.

### Tile Entity: TileEntityMachineChemicalPlant

**Capacity:** 22 slots, 3 input tanks (24000 mB each), 3 output tanks (24000 mB each)

**Slot layout:**

| Slot(s) | Purpose |
|---------|---------|
| 0 | Battery |
| 1 | Blueprint |
| 2–3 | Upgrades |
| 4–6 | Solid input (3 types) |
| 7–9 | Solid output (3 types) |
| 10–12 | Input fluid buckets (fill) |
| 13–15 | Input fluid empty buckets (return) |
| 16–18 | Output fluid buckets (fill) |
| 19–21 | Output fluid empty buckets (return) |

**Server tick:**

Same as Assembly Machine with these additions:
- **Bucket handling**: `inputTanks[i].loadTank(10, 13, slots)` fills tanks from input bucket slots and returns empties; `outputTanks[i].unloadTank(16, 19, slots)` fills output bucket slots from tanks and returns empties.
- **3 input/3 output subscribe/provide**: All three input tanks try to subscribe fluids; all three output tanks try to provide.

**Client tick:**

- Frame detection (same as assembler)
- Audio: `CHEMPLANT_LOOP`
- Simple animation: `anim` counter increments while processing

### Container: ContainerMachineChemicalPlant

176×256 GUI. Additional fluid bucket/empty bucket slots for all 3 input and 3 output tanks.

### GUI: GUIMachineChemicalPlant

Renders:
- Power bar
- Progress bar
- 3× fluid tank displays (input and output)
- Recipe selector button

---

## 9. Machine Catalog

Every machine in this catalog extends `BlockDummyable` (the new multiblock system). Dimensions follow the format `{up, down, north, south, west, east}`. The **offset** is how far the core is from the placement block.

### 9.1 Assembly & Chemical

| Machine | Dimensions | Offset | Proxy | Description |
|---------|-----------|--------|-------|-------------|
| `MachineAssemblyMachine` | `{2,0,1,1,1,1}` | 1 | Combo+Fluid+Power+Inv | 3×3 auto-crafter (12 input slots) |
| `MachineAssemblyFactory` | `{2,0,2,2,2,2}` | 2 | ProxyDyn | 5×5 four-module assembler |
| `MachineChemicalPlant` | `{2,0,1,1,1,1}` | 1 | Combo+Fluid+Power+Inv | 3×3 chemical reactor (3-liquid) |
| `MachineChemicalFactory` | `{2,0,2,2,2,2}` | 2 | ProxyDyn | 5×5 four-module chemplant |
| `MachinePrecAss` | extends `MachineAssemblyMachine` | — | — | Precision assembler variant |

### 9.2 Oil Processing

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `MachineRefinery` | `{8,0,1,1,1,1}` | 1 | 10-block-tall oil refinery tower (5 outputs) |
| `MachineFractionTower` | `{2,0,1,1,1,1}` | 1 | 3-tall fractional distillation tower |
| `MachineHydrotreater` | `{6,0,1,1,1,1}` | 1 | 8-block-tall hydrotreating tower |
| `MachineVacuumDistill` | `{8,0,1,1,1,1}` | 1 | 10-block-tall vacuum distillation tower |
| `MachineCatalyticCracker` | `{0,0,3,3,2,3}` | 3 | 6×7 fluid catalytic cracker |
| `MachineCatalyticReformer` | `{2,0,1,1,2,2}` | 1 | Naphtha catalytic reformer |
| `MachineCoker` | `{22,0,1,1,1,1}` | 1 | 24-block-tall coker unit (extra structure above) |
| `MachineAlkylation` | `{3,0,2,2,1,1}` | 2 | High-octane alkylation plant |
| `MachineCryoDistill` | `{3,2,3,3,2,2}` | 3 | Cryogenic gas distillation (heightOffset=2) |

### 9.3 Nuclear Processing

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `MachinePUREX` | `{4,0,2,2,2,2}` | 2 | Nuclear fuel reprocessing (5×5×5) |
| `MachineSILEX` | `{2,0,1,1,1,1}` | 1 | Laser isotope separation |
| `MachineCyclotron` | `{2,0,2,2,2,2}` | 2 | Particle accelerator (12 particle path extras) |
| `MachineFEL` | `{2,0,4,2,1,1}` | 2 | Free Electron Laser |
| `MachineGasCent` | `{3,0,0,0,0,0}` | 0 | Gas centrifuge (4-tall, 1×1) |
| `MachineCentrifuge` | `{3,0,0,0,0,0}` | 0 | Industrial centrifuge (4-tall, 1×1) |

### 9.4 Reactors & Power Plants

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `MachineICF` | `{5,0,1,1,8,8}` | 1 | Inertial Confinement Fusion (6-tall, 17×17) |
| `Watz` | `{2,0,3,3,1,1}` | 3 | WATZ power plant (3-tall, 7×3 cross, 4 extra zones) |
| `WatzPump` | `{1,0,0,0,0,0}` | 0 | WATZ fluid pump |
| `ReactorZirnox` | `{1,0,2,2,2,2}` | 2 | ZIRNOX gas-cooled reactor (5×5×6, 3 extra zones) |
| `ReactorResearch` | `{2,0,0,0,0,0}` | 0 | Research reactor (3-tall, 1×1) |
| `MachineReactorBreeding` | `{2,0,2,2,2,2}` | 2 | Breeding reactor (5×5) |
| `MachineRadGen` | `{2,0,3,2,1,1}` | 2 | Radiation-powered generator |

### 9.5 Fusion Components (New System)

All extend `BlockDummyable` and are part of the new fusion system. The core is `MachineFusionTorus`; sub-components connect to it.

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `MachineFusionTorus` | `{4,0,7,7,7,7}` | 7 | Core fusion reactor (5-tall, 15×15 ring, 3D layout) |
| `MachineFusionKlystron` | `{3,0,4,3,2,2}` | 3 | RF heating klystron |
| `MachineFusionKlystronCreative` | `{3,0,4,3,2,2}` | 3 | Creative klystron (infinite) |
| `MachineFusionBreeder` | `{3,0,2,2,1,1}` | 2 | Tritium breeder blanket |
| `MachineFusionBoiler` | `{3,0,4,4,1,1}` | 4 | Fusion heat exchanger |
| `MachineFusionMHDT` | `{2,0,6,7,2,2}` | 7 | MHD generator (steam from heat) |
| `MachineFusionCollector` | `{3,0,2,1,2,2}` | 1 | Particle collector |
| `MachineFusionCoupler` | `{3,0,1,1,1,1}` | 0 | Waveguide coupler |
| `MachineFusionPlasmaForge` | `{2,0,2,2,5,5}` | 5 | Plasma forge |

### 9.6 RBMK Reactor

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `RBMKBase` (abstract) | `{3,0,0,0,0,0}` | 0 | Base for all RBMK columns (4-tall, configurable height) |
| `RBMKConsole` | `{3,0,0,0,2,2}` | 1 | Control console (5×5 side structure) |
| `RBMKCraneConsole` | `{1,0,0,0,1,1}` | 1 | Fuel crane console |
| `RBMKAutoloader` | `{8,0,0,0,0,0}` | 0 | Fuel autoloader arm (9-tall) |

RBMK columns are 1×1 structures with configurable height via `RBMKDials.getColumnHeight()`. The `getDirModified()` override forces `DIR_NO_LID` (no facing rotation). Has lid mechanics with `metaToLid()`.

### 9.7 Heat Systems

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `MachineHeatBoiler` | `{3,0,1,1,1,1}` | 1 | Heat-to-steam boiler (explosion mechanics) |
| `MachineHeatBoilerIndustrial` | `{4,0,1,1,1,1}` | 1 | Industrial heat boiler |
| `MachineStirling` | `{1,0,1,1,1,1}` | 1 | Stirling engine (heat→power) |
| `MachineSteamEngine` | `{1,0,5,1,1,1}` | 1 | Large steam engine (flywheel extras) |
| `MachineLargeTurbine` | `{1,0,3,1,1,1}` | 1 | Large steam turbine |
| `MachineIndustrialTurbine` | `{2,0,3,3,1,1}` | 3 | Multi-stage industrial turbine |
| `MachineCombustionEngine` | `{1,0,1,0,3,2}` | 0 | Internal combustion engine |
| `MachineTurbineGas` | `{2,0,1,1,4,5}` | 1 | Gas turbine (complex rotor structure) |
| `MachineTurbofan` | `{2,0,1,1,3,3}` | 1 | Turbofan engine |
| `HeaterElectric` | `{0,0,1,2,1,1}` | 2 | Electric heater |
| `HeaterFirebox` | `{0,0,1,1,1,1}` | 1 | Solid-fuel firebox |
| `HeaterHeatex` | `{0,0,1,1,1,1}` | 1 | Heat exchanger |
| `HeaterOilburner` | `{1,0,1,1,1,1}` | 1 | Oil-fueled burner |
| `HeaterOven` | `{0,0,1,1,1,1}` | 1 | Electric oven heater |
| `MachineChungus` | `{4,0,10,3,2,2}` | 3 | Large steam boiler with variable steam types |
| `MachineChimneyBrick` | `{12,0,1,1,1,1}` | 1 | Brick chimney (14-tall) |
| `MachineChimneyIndustrial` | `{22,0,1,1,1,1}` | 1 | Industrial chimney (24-tall) |
| `MachineRadiator` | `{1,1,0,0,1,1}` | 0 | Cooling radiator (heightOffset=1) |

### 9.8 Processing & Manufacturing

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `MachineCrystallizer` | `{5,0,1,1,1,1}` | 1 | Crystallization chamber (7-tall) |
| `MachineElectrolyser` | `{0,0,5,5,1,3}` | 5 | Electrolysis plant (multi-layer) |
| `MachineCompressor` | `{2,0,1,2,1,1}` | 2 | Gas compressor (tall intake) |
| `MachineCompressorCompact` | `{2,0,1,1,3,3}` | 1 | Compact gas compressor |
| `MachineArcFurnaceLarge` | `{4,0,2,2,2,2}` | 2 | Large electric arc furnace |
| `MachineRotaryFurnace` | `{4,0,1,1,2,2}` | 1 | Rotary kiln furnace |
| `MachineSawmill` | `{1,0,1,1,1,1}` | 1 | Multiblock sawmill |
| `MachineCrucible` | `{1,0,1,1,1,1}` | 1 | Foundry crucible (3×3) |
| `MachineStrandCaster` | `{2,0,3,3,1,1}` | 3 | Continuous strand caster |
| `MachineMixer` | `{2,0,1,1,1,1}` | 1 | Fluid/item mixing |
| `MachineSolidifier` | `{3,0,1,1,1,1}` | 1 | Fluid→solid machine |
| `MachineLiquefactor` | `{3,0,1,1,1,1}` | 1 | Solid→fluid machine |
| `MachineRadiolysis` | `{2,0,2,2,2,2}` | 2 | Radiation chemical processing |
| `MachinePyroOven` | `{2,0,1,1,1,1}` | 1 | Pyrolysis oven |
| `MachinePress` | `{2,0,0,0,0,0}` | 0 | Multiblock press (isLegacyMonoblock) |

### 9.9 Manufacturing & Processing (Additional)

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `MachineCompressor` | `{2,0,1,2,1,1}` | 2 | Gas compressor (tall vertical intake) |
| `MachineCompressorCompact` | `{2,0,1,1,3,3}` | 1 | Compact gas compressor (3×3) |
| `MachineConveyorPress` | `{2,0,0,0,0,0}` | 0 | Conveyor-fed press |
| `MachineEPress` | `{2,0,0,0,0,0}` | 0 | Electric press (isLegacyMonoblock) |
| `MachineArcWelder` | `{1,0,1,0,1,1}` | 0 | Arc welder for crafting |
| `MachineAmmoPress` | varies | — | Ammunition press |
| `MachineAshpit` | varies | — | Ash collection pit |
| `MachineSolderingStation` | varies | — | Soldering station for circuits |
| `MachineVacuumCircuit` | varies | — | Vacuum circuit engraver |
| `MachineCondenserPowered` | varies | — | Powered condenser |
| `MachineDriveProcessor` | varies | — | Data/manufacturing drive processor |
| `MachineExposureChamber` | varies | — | Radiation exposure chamber |
| `MachineDishControl` | varies | — | Satellite dish controller |
| `MachineAtmosphericEmitter` | varies | — | Atmospheric emitter device |
| `FurnaceCombination` | varies | — | Combination furnace |
| `FurnaceIron` | varies | — | Iron furnace |
| `FurnaceSteel` | varies | — | Steel furnace |

### 9.10 Fluid Handling & Extraction

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `MachineOilWell` | `{9,0,1,1,1,1}` | 0 | Oil derrick (custom multi-zone validation) |
| `MachinePumpjack` | `{3,0,0,0,0,6}` | 0 | Pumpjack (beam structure extras) |
| `MachineFrackingTower` | `{3,0,0,0,0,0}` | 0 | Fracking tower |
| `MachinePump` | `{3,0,1,1,1,1}` | 1 | Fluid pump |
| `MachineDrain` | `{0,0,2,0,0,0}` | 0 | Fluid drain |
| `MachineGasFlare` | `{11,0,1,1,1,1}` | 1 | Gas flare stack (13-tall) |
| `MachineFluidTank` | varies | — | Large fluid tank |
| `MachineBigAssTank9000` | `{4,0,2,2,1,1}` | 1 | Extremely large fluid tank |
| `MachineFENSU` | `{4,0,1,1,2,2}` | 1 | Fluid energy storage |

### 9.10 Energy Storage & Distribution

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `MachineBatterySocket` | `{1,0,1,0,1,0}` | 0 | Modular battery socket |
| `MachineBatteryREDD` | `{9,0,2,2,4,4}` | 1 | REDD large battery |
| `PylonMedium` | varies | — | Medium power pylon |
| `PylonLarge` | varies | — | Large power pylon |
| `Substation` | varies | — | Power substation |

### 9.11 Launch Pads & Space

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `SoyuzLauncher` | custom `{0,0,0,0,0,0}` | 0 | Soyuz rocket launch system (7 volumes, forces EAST) |
| `LaunchPad` | custom | — | Standard missile launch pad |
| `LaunchPadLarge` | `{0,0,4,4,4,4}` | — | Large launch pad (9×9) |
| `LaunchPadRocket` | custom | — | Rocket launch pad |
| `LaunchPadRusted` | custom | — | Rusted/derelict launch pad |
| `MachineRocketAssembly` | `{0,2,4,4,4,4}` | 4 | Rocket assembly station (9×9×3, 8 leg zones) |
| `BlockTransporterRocket` | varies | — | Transporter rocket |
| `BlockOrbitalStation` | varies | — | Orbital station controller |
| `BlockOrbitalStationComputer` | varies | — | Orbital station computer |

### 9.12 Power Generation

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `MachineIGenerator` | memorial | 0 | Legacy industrial generator (memorial, non-functional) |
| `MachineMagma` | `{3,3,3,3,3,3}` | 3 | Geothermal power (7×7×7, heightOffset=3) |
| `MachineSolar` | varies | — | Solar panel array |
| `MachineHephaestus` | `{11,0,1,1,1,1}` | 1 | Geothermal generator (13-tall) |
| `MachineOrbus` | `{4,0,2,1,2,1}` | 1 | Orbital beam receiver |

### 9.13 Dyson Sphere

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `MachineDysonLauncher` | `{16,0,8,0,0,0}` | 0 | Dyson swarm launcher (massive 3D structure) |
| `MachineDysonReceiver` | `{2,0,4,2,2,2}` | 2 | Dyson power receiver (dish structure) |
| `MachineDysonConverterAnatmogenesis` | `{2,0,5,5,1,1}` | 5 | Dyson matter converter |
| `MachineDysonConverterHE` | `{2,0,4,4,1,1}` | 4 | Dyson high-energy converter |
| `MachineDysonConverterTU` | `{2,0,1,6,1,1}` | 6 | Dyson thermal upgrade converter |

### 9.14 Particle Accelerator (Albion)

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `BlockPASource` | `{1,1,1,1,4,4}` | 0 | Particle source (6×6×6) |
| `BlockPARFC` | `{1,1,1,1,4,4}` | 0 | RF cavity accelerator |
| `BlockPAQuadrupole` | `{1,1,1,1,1,1}` | 0 | Quadrupole magnet (3×3×3) |
| `BlockPADipole` | `{1,1,1,1,1,1}` | 0 | Dipole magnet (3×3×3) |
| `BlockPADetector` | `{2,2,2,2,4,4}` | 0 | Detector (5×5×9) |
| `BlockPABeamline` | `{0,0,0,0,1,1}` | 0 | Beam line (1×1×2 corridor) |

### 9.15 Towers & Elevators

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `BlockCargoElevator` | `{0,0,1,1,1,1}` | 1 | Cargo elevator platform (3×3 base) |
| `MachineTowerSmall` | varies | — | Small cooling tower |
| `MachineTowerLarge` | varies | — | Large cooling tower |
| `AtmoTower` | varies | — | Atmospheric processing tower |
| `BlockAtmosphericCompressor` | varies | — | Atmospheric gas compressor |
| `DeuteriumTower` | varies | — | Deuterium extraction tower |
| `FractionSpacer` | varies | — | Fractionation column spacer |
| `BlockAirScrubber` | varies | — | Air scrubbing/filtration |
| `BlockLantern` | varies | — | Hanging lantern (4-tall) |
| `BlockLanternBehemoth` | varies | — | Large behemoth lantern |
| `BlockDoorGeneric` | varies | — | Generic multiblock door |

### 9.16 Other Notable Machines

| Machine | Dimensions | Offset | Description |
|---------|-----------|--------|-------------|
| `MachineExcavator` | `{3,0,3,3,3,3}` | 3 | Mining excavator (7×7×7, 4-zone validation) |
| `MachineMiningLaser` | `{1,1,1,1,1,1}` | 0 | Mining laser (half underground, heightOffset=-1) |
| `MachineAnnihilator` | `{2,0,4,4,1,1}` | 4 | Annihilator (voids items for energy, large off-axis rail) |
| `MachineLPW2` | `{6,0,3,3,9,10}` | 9 | Large particle weapon (rotation override) |
| `MachineStardar` | `{0,3,2,2,2,2}` | 2 | Radar/FAA system (heightOffset=3) |
| `MachineHydroponic` | `{2,0,0,0,2,2}` | 0 | Hydroponic farm (3×5×3) |

---

## 10. Special Structure Patterns

While most multiblocks use the standard `checkRequirement` + `fillSpace` from `BlockDummyable`, several machines override these methods for custom structure shapes.

### 10.1 Multi-Zone Validation

Machines whose structure goes beyond a simple rectangular bounding box override `checkRequirement()` to call `MultiblockHandlerXR.checkSpace()` multiple times with different dimension arrays. These machines also override `fillSpace()` with matching multiple `fillSpace()` calls.

| Machine | Zones | Description |
|---------|-------|-------------|
| `MachineOilWell` | 6 zones | Underground pipe (1 block down), 4 diagonal feet (8-tall), main tower (8-tall) |
| `Watz` | 5 zones | Main 7×3 center + 4 diagonal wing extensions |
| `MachineExcavator` | 4 zones | Main 7×7 + 3 additional off-axis dig zones |
| `MachineICF` | 3 zones | Main 17×17 + 2 wing sections at y+3 |
| `SoyuzLauncher` | 7 zones | Pedestal, 4 legs, launch ring, central column |
| `ReactorZirnox` | 4 zones | Main 5×5 + 3 diagonal extra zones |
| `MachineLPW2` | custom | Complex rotation with large barrel structure |
| `MachineCoker` | 2 zones | Main column + extra tall structure above |
| `MachineDysonLauncher` | 5+ zones | Massive multi-component launcher structure |

### 10.2 Custom 3D Pattern Matching (Fusion Torus)

The `MachineFusionTorus` uses a completely different approach from standard `checkSpace()`. It defines a `int[][][] layout` array (three 15×15 layers) where each value encodes the required block type:

| Value | Block Required |
|-------|---------------|
| `0` | Air (no check) |
| `1` | `BlockFusionComponent` meta 1 (BSCOO welded) |
| `2` | `BlockFusionComponent` meta 2 (blanket) |
| `3` | `BlockFusionComponent` meta 3 (motor) |

```java
// In checkRequirement: iterates the layout, checks each non-zero position
// can be placed on. In fillSpace: places the actual block at each position.
for(int iy = 0; iy < 5; iy++) {
    int l = iy > 2 ? 4 - iy : iy;  // mirror top/bottom
    int[][] layer = layout[l];
    for(int ix = 0; ix < layer.length; ix++) {
        for(int iz = 0; iz < layer.length; iz++) {
            if(layout[l][ix][iz] > 0) {
                // validate / place block
            }
        }
    }
}
```

The shape forms a donut/torus outline. Extra dummies are placed at 24 specific positions around the ring perimeter for IO connections.

### 10.3 Builder Block Pattern (Fusion Torus Assembly)

The fusion torus uses a two-step assembly process:

1. **Builder block**: `BlockFusionTorusStruct` (extends `BlockContainer`, NOT BlockDummyable) is placed by the player. Its TE (`TileEntityFusionTorusStruct`) runs every 20 ticks and scans the surrounding volume using `MachineFusionTorus.layout` as a template.

2. **Check & convert**: When all positions have the correct `BlockFusionComponent` blocks, the builder block replaces itself with `ModBlocks.fusion_torus` at meta 12 and calls `block.fillSpace()` to place the final dummy blocks.

```java
// TileEntityFusionTorusStruct.updateEntity()
if(!worldObj.isRemote && worldObj.getTotalWorldTime() % 20 == 0) {
    for all positions in MachineFusionTorus.layout:
        check BlockFusionComponent with correct meta
    if all correct:
        worldObj.setBlock(x, y, z, ModBlocks.fusion_torus, 12, 3);
        block.fillSpace(worldObj, x, y, z, ForgeDirection.NORTH, 0);
}
```

### 10.4 RBMK Dynamic Height

`RBMKBase` uses configurable column height instead of a fixed dimension array. It provides both `getDimensions()` (required by the abstract class) and an overload `getDimensions(World world)` used internally:

```java
// RBMKBase provides both the standard abstract override:
@Override public int[] getDimensions() { return new int[] {3, 0, 0, 0, 0, 0}; }

// And a world-aware overload for the dial-based height:
public int[] getDimensions(World world) {
    return new int[] {RBMKDials.getColumnHeight(world), 0, 0, 0, 0, 0};
}
```

The `getDirModified()` override forces `DIR_NO_LID` (no facing rotation) and the `fillSpace()` method uses the dial value to place the correct number of dummy columns. Lid states are tracked via `metaToLid()`. Extra dummies are placed at the top of the column for fuel loading.

### 10.5 BlockDummyableBeam (Pass-Through Strut)

`BlockDummyableBeam` at `src/main/java/com/hbm/blocks/BlockDummyableBeam.java` is a specialized variant of BlockDummyable that acts as a visual strut/beam. It has `{0,0,0,0,0,0}` dimensions and returns `null` from `createNewTileEntity()`. All interactions (activation, break, neighbor change, overlay) are delegated to the neighboring BlockDummyable that the beam metadata points toward:

```java
// In BlockDummyableBeam.findCore():
// Follow metadata direction, then delegate to the adjacent real dummyable
Block b = world.getBlock(x, y, z);
if(b instanceof BlockDummyable && !(b instanceof BlockDummyableBeam))
    return ((BlockDummyable) b).findCore(world, x, y, z);
```

Used for decorative/structural beams on large multiblocks. Has no TE and passes all logic to the parent machine.

### 10.6 Proxy Variants for Different Capabilities

Most machines use `TileEntityProxyCombo` with `.inventory().power().fluid()` for full capability access. Some machines use specialized proxy configurations:

| Machine | Proxy Setup | Rationale |
|---------|-------------|-----------|
| Assembly/Chemical Plant | `Combo().inventory().power().fluid()` | Full IO at all extra dummies |
| Factory (4×) | `TileEntityProxyDyn` | Position-dependent filtering |
| Watz | `Combo().inventory().fluid()` | No power IO needed |
| Excavator | `Combo().power().fluid()` | No inventory IO needed |
| Dyson Launcher | `Combo(true, false, false)` or `Combo(false, true, false)` | Per-position power vs fluid |
| PUREX | `Combo().inventory().power().fluid()` | Full IO |
| Oil Well | No proxies | No extra dummies |

---

## 11. Data Flow & Sync

### Server → Client Synchronization

The primary sync channel is `networkPackNT()`:

```
Server tick completion
    → networkPackNT(interval_ticks)
    → serialize(ByteBuf buf)
         ├─ FluidTank.serialize(buf) [all tanks]
         ├─ power / maxPower (long)
         ├─ didProcess (boolean)
         └─ ModuleMachineBase.serialize(buf)
              ├─ progress (double)
              └─ recipe (String via ByteBufUtils)
    → PacketDispatcher.sendToAllAround()
```

The Chemical Plant serializes 6 tanks (3 input + 3 output); the Assembly Machine serializes 2.

### Client → Server (Recipe Selection)

When the player selects a recipe in the GUI:

```
GUIScreenRecipeSelector.onClick()
    → IControlReceiver packet (NBTTagCompound)
         ├─ "index"     : int (module index, always 0 for single-module machines)
         └─ "selection" : String (recipe name, e.g., "ass.plateiron")
    → TileEntity.receiveControl(NBTTagCompound)
         ├─ module.recipe = selection
         └─ markChanged()
```

### Persistence

Both NBT and the packet system use the same data layout:

```java
// readFromNBT / writeToNBT
this.progress = nbt.getDouble("progress" + index);
this.recipe = nbt.getString("recipe" + index);

// serialize / deserialize (network)
buf.writeDouble(progress);
ByteBufUtils.writeUTF8String(buf, recipe);
```

### Audio Lifecycle

A `didProcess` state change triggers audio:
- On server: `didProcess` flag is set in the packet
- On client: `deserialize()` compares old vs new `didProcess` — playback `ASSEMBLER_STOP` on transition
- `updateEntity()` on client manages audio wrapper lifecycle (create, keepAlive, stop) based on processing state and player distance

---

## 12. Tutorial: Adding a New Multiblock Machine

This section walks through adding a complete new multiblock machine, from block creation to registration. It covers both the **module-based pattern** (recipe-driven machines using `ModuleMachineBase` + `GenericRecipes`) and the **direct-processing pattern** (simpler machines with custom logic).

We will create an example machine called the **"Crystallizer"** — a 7-block-tall multiblock that processes items using power and fluid, with recipe gating via a module.

---

### 12.1 Step 1: Create the Block Class

Every multiblock machine is a subclass of `BlockDummyable`. This file goes in `src/main/java/com/hbm/blocks/machine/`.

```java
package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineMyProcessor;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class MachineMyProcessor extends BlockDummyable {

    public MachineMyProcessor() {
        super(Material.iron);
    }

    // --- Tile Entity Creation ---

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        // meta >= 12 → core block (has the main TE)
        if(meta >= 12) return new TileEntityMachineMyProcessor();
        // meta 6-11 → "extra" dummy blocks (IO-capable, get a proxy TE)
        if(meta >= 6)  return new TileEntityProxyCombo().inventory().power().fluid();
        // meta 0-5  → regular dummies (no TE)
        return null;
    }

    // --- Structure Dimensions ---

    @Override
    public int[] getDimensions() {
        // {up, down, north, south, west, east}
        return new int[] {5, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        // How far the core is from the placement block
        return 1;
    }

    // --- GUI Opening ---

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z,
            EntityPlayer player, int side, float hitX, float hitY, float hitZ) {

        if(world.isRemote) return true;

        if(!player.isSneaking()) {
            int[] pos = this.findCore(world, x, y, z);
            if(pos == null) return false;

            FMLNetworkHandler.openGui(player, MainRegistry.instance,
                0, world, pos[0], pos[1], pos[2]);
            return true;
        }
        return false;
    }

    // --- Extra Dummy Placement for IO ---

    @Override
    protected void fillSpace(World world, int x, int y, int z,
            ForgeDirection dir, int o) {
        // Place standard dummy blocks first
        super.fillSpace(world, x, y, z, dir, o);

        // Place 4 extra dummies at ground-level corners for pipe/cable connections
        this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o + 1);
        this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o + 1);
        this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o - 1);
        this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o - 1);
    }
}
```

**Key decisions:**

| Decision | Options | When to use |
|----------|---------|-------------|
| `getOffset()` | `0` = core at placement, `1` = 1 block back, `N` = N blocks back | Base your machine dimensions. Offset=1 for 3×3, 2 for 5×5, etc. |
| `createNewTileEntity` | Return core TE, proxy TE, or null | Core needs TE. Extras need proxy if IO is needed. Dummies get null. |
| `onBlockActivated` vs `standardOpenBehavior` | Manual vs convenience method | Use `standardOpenBehavior` for simple cases; manual for extra logic |
| `fillSpace` override | Add `makeExtra` calls for each IO position | Always add extras where pipes/cables should connect |

**Simpler shortcut**: If your machine doesn't need extra dummies and follows standard behavior, use `standardOpenBehavior`:

```java
@Override
public boolean onBlockActivated(...) {
    return this.standardOpenBehavior(world, x, y, z, player, 0);
}
```

This is a convenience method in `BlockDummyable` that calls `findCore()` and opens the GUI.

---

### 12.2 Step 2: Create the Tile Entity

The Tile Entity lives in `src/main/java/com/hbm/tileentity/machine/`. There are two approaches:

#### Pattern A: Module-Based (for recipe-driven machines)

Extends `TileEntityMachineBase` and delegates to a `ModuleMachineBase` instance.

```java
package com.hbm.tileentity.machine;

import com.hbm.inventory.container.ContainerMyProcessor;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.gui.GUIMyProcessor;
import com.hbm.lib.ForgeDirection;
import com.hbm.module.machine.ModuleMachineAssembler;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class TileEntityMachineMyProcessor extends TileEntityMachineBase
        implements IGUIProvider {

    public FluidTank inputTank;
    public FluidTank outputTank;

    // Module instance handles all processing logic
    public ModuleMachineAssembler module;

    public TileEntityMachineMyProcessor() {
        // Slot count: 0=battery, 1=blueprint, 2-3=upgrades, 4-15=input, 16=output
        super(17);

        // Create tanks (4 buckets each)
        this.inputTank = new FluidTank(Fluids.NONE, 4000);
        this.outputTank = new FluidTank(Fluids.NONE, 4000);

        // Create and configure the module
        this.module = new ModuleMachineAssembler(0, this, slots)
            .itemInput(4)
            .itemOutput(16)
            .fluidInput(inputTank)
            .fluidOutput(outputTank);
    }

    @Override
    public void updateEntity() {

        if(worldObj.isRemote) {
            // --- CLIENT: animation ---
            return;
        }

        // --- SERVER: processing ---

        // Handle power from battery item
        // (simplified — see TileEntityMachineAssemblyMachine for full power logic)

        // Detect upgrades and compute speed/power multipliers
        double speed = 1.0;
        double power = 1.0;

        // Run the module's processing lifecycle
        boolean didProcess = this.module.update(speed, power, true, slots[1]);

        // Fluid IO: subscribe input, provide output
        this.subscribeToAround(inputTank, getConsPos());
        this.provideFluidToAround(outputTank, getConsPos());

        // Network sync every 5 seconds
        if(worldObj.getTotalWorldTime() % 100 == 0) {
            this.networkPackNT(100);
        }

        if(didProcess || this.module.markDirty) {
            this.markChanged();
        }
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        this.inputTank.serialize(buf);
        this.outputTank.serialize(buf);
        this.module.serialize(buf);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        this.inputTank.deserialize(buf);
        this.outputTank.deserialize(buf);
        this.module.deserialize(buf);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.inputTank.readFromNBT(nbt, "inputTank");
        this.outputTank.readFromNBT(nbt, "outputTank");
        this.module.readFromNBT(nbt);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        this.inputTank.writeToNBT(nbt, "inputTank");
        this.outputTank.writeToNBT(nbt, "outputTank");
        this.module.writeToNBT(nbt);
    }

    public int[] getConsPos() {
        // Returns positions around the machine for IO connections
        // (override to match your machine's shape)
        return new int[] {0, 0, 0, 0, 0, 0};
    }

    @Override
    public Container provideContainer(int id, EntityPlayer player, World world,
            int x, int y, int z) {
        return new ContainerMyProcessor(player.inventory, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Object provideGUI(int id, EntityPlayer player, World world,
            int x, int y, int z) {
        return new GUIMyProcessor(player.inventory, this);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return AxisAlignedBB.getBoundingBox(
            xCoord - 1, yCoord, zCoord - 1,
            xCoord + 2, yCoord + 6, zCoord + 2);
    }
}
```

#### Pattern B: Direct Processing (for simple/unique machines)

For machines that don't fit the module pattern, implement the logic directly in `updateEntity()`. See `TileEntityMachineMixer` at `src/main/java/com/hbm/tileentity/machine/TileEntityMachineMixer.java` as an example.

**Required interfaces:**

| Interface | Purpose | Required? |
|-----------|---------|-----------|
| `TileEntityMachineBase` | Base class (extends `TileEntityInventoryBase`) | Always |
| `IGUIProvider` | Provides Container and GUI instances | Always |
| `IEnergyReceiverMK2` | Power reception from network | Usually |
| `IFluidStandardReceiver` / `IFluidStandardTransceiver` | Fluid IO | Usually |
| `IControlReceiver` | Receives packets from GUI (recipe selection) | If recipes are selectable |
| `IUpgradeInfoProvider` | Upgrade tooltips in GUI | If upgrades are supported |
| `IFluidCopiable` | Copy/paste fluid config | Optional |

---

### 12.3 Step 3: Create the Module

If your machine is recipe-driven with standard processing (input → consume → output), extend `ModuleMachineBase`. File goes in `src/main/java/com/hbm/module/machine/`.

```java
package com.hbm.module.machine;

import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.recipes.MyProcessorRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import api.hbm.energymk2.IEnergyHandlerMK2;
import net.minecraft.item.ItemStack;

public class ModuleMyProcessor extends ModuleMachineBase {

    public ModuleMyProcessor(int index, IEnergyHandlerMK2 battery,
            ItemStack[] slots) {
        super(index, battery, slots);

        // Define slot/tank counts for this module type
        this.inputSlots = new int[4];   // 4 input slots
        this.outputSlots = new int[2];  // 2 output slots
        this.inputTanks = new FluidTank[1];
        this.outputTanks = new FluidTank[1];
    }

    @Override
    public GenericRecipes getRecipeSet() {
        return MyProcessorRecipes.INSTANCE;
    }

    // --- Builder methods ---

    public ModuleMyProcessor itemInput(int... slots) {
        this.inputSlots = slots;
        return this;
    }

    public ModuleMyProcessor itemOutput(int... slots) {
        this.outputSlots = slots;
        return this;
    }

    public ModuleMyProcessor fluidInput(FluidTank... tanks) {
        this.inputTanks = tanks;
        return this;
    }

    public ModuleMyProcessor fluidOutput(FluidTank... tanks) {
        this.outputTanks = tanks;
        return this;
    }
}
```

The module inherits from `ModuleMachineBase`:
- `update(speed, power, extraCondition, blueprint)` — the main tick method
- `canProcess(recipe, speed, power)` — checks power, input items, input fluids, output space
- `process(recipe, speed, power)` — consumes inputs, advances progress, produces output
- Auto-switch detection if recipes share an `autoSwitchGroup`

---

### 12.4 Step 4: Create the Recipe Set

Recipe sets extend `GenericRecipes<GenericRecipe>`. File goes in `src/main/java/com/hbm/inventory/recipes/`.

```java
package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class MyProcessorRecipes extends GenericRecipes<GenericRecipe> {

    // Singleton instance
    public static final MyProcessorRecipes INSTANCE = new MyProcessorRecipes();

    @Override
    public String getFileName() {
        return "hbmMyProcessor.json";  // JSON file in hbmRecipes/ folder
    }

    @Override
    public void registerDefaults() {

        // Recipe: 8 iron ingot + 1000mB water → 8 iron block
        GenericRecipe recipe1 = new GenericRecipe()
            .setup(200, 50)                  // 200 ticks, 50 HE/t
            .inputItems(new ComparableStack(Items.iron_ingot, 8))
            .inputFluids(new FluidStack(Fluids.WATER, 1000))
            .outputItems(new ComparableStack(Blocks.iron_block, 1));
        this.registerRecipe(recipe1);

        // Recipe with blueprint gating and auto-switch
        GenericRecipe recipe2 = new GenericRecipe()
            .setup(100, 25)
            .inputItems(new ComparableStack(Items.gold_ingot, 4))
            .outputItems(new ComparableStack(Items.gold_ingot, 8))
            .setPools("discover.x")
            .setGroup("autoswitch.myprocessor", this);
        this.registerRecipe(recipe2);
    }

    @Override
    public int inputItemLimit() {
        return 12;   // Up to 12 distinct item input types per recipe
    }

    @Override
    public int outputItemLimit() {
        return 4;    // Up to 4 distinct item output types per recipe
    }

    @Override
    public int inputFluidLimit() {
        return 1;    // Up to 1 input fluid per recipe
    }

    @Override
    public int outputFluidLimit() {
        return 1;    // Up to 1 output fluid per recipe
    }
}
```

**Register the recipe set** in `SerializableRecipe.registerAllHandlers()` at `src/main/java/com/hbm/inventory/recipes/loader/SerializableRecipe.java`:

```java
// Inside registerAllHandlers():
recipeHandlers.add(new MyProcessorRecipes());
```

---

### 12.5 Step 5: Create the Container

The Container manages slot layout and shift-click logic. File goes in `src/main/java/com/hbm/inventory/container/`.

```java
package com.hbm.inventory.container;

import com.hbm.inventory.SlotMachineOutput;
import com.hbm.tileentity.machine.TileEntityMachineMyProcessor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerMyProcessor extends ContainerBase {

    public ContainerMyProcessor(InventoryPlayer playerInv,
            TileEntityMachineMyProcessor te) {
        super(playerInv, te);

        // Slots 0-3: battery, blueprint, upgrades
        this.addSlotToContainer(new Slot(te, 0, 44, 17));    // Battery
        this.addSlotToContainer(new Slot(te, 1, 116, 17));   // Blueprint
        this.addSlotToContainer(new Slot(te, 2, 8, 107));     // Upgrade 1
        this.addSlotToContainer(new Slot(te, 3, 26, 107));    // Upgrade 2

        // Slots 4-15: 12 input slots (in a 4×3 grid)
        for(int i = 0; i < 12; i++) {
            this.addSlotToContainer(
                new Slot(te, 4 + i, 8 + i % 4 * 18, 35 + i / 4 * 18));
        }

        // Slot 16: output (locked)
        this.addSlotToContainer(new SlotMachineOutput(te, 16, 134, 44));

        // Player inventory (standard — always include)
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 9; j++) {
                this.addSlotToContainer(
                    new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for(int i = 0; i < 9; i++) {
            this.addSlotToContainer(
                new Slot(playerInv, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        // Standard shift-click logic — route items to appropriate slot groups
        // (see ContainerMachineAssemblyMachine for a complete example)
        return super.transferStackInSlot(player, slotIndex);
    }
}
```

**Slot types:**

| Slot Class | Purpose |
|------------|---------|
| `Slot` | Standard slot (player can place and take items) |
| `SlotMachineOutput` | Output slot (player can only take, not place) |
| `SlotUpgrade` | Upgrade-only slot |

---

### 12.6 Step 6: Create the GUI

The GUI renders the machine interface. File goes in `src/main/java/com/hbm/inventory/gui/`.

```java
package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMyProcessor;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityMachineMyProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
public class GUIMyProcessor extends GuiInfoContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        RefStrings.MODID + ":textures/gui/processing/gui_crystallizer_alt.png");

    private TileEntityMachineMyProcessor processor;

    public GUIMyProcessor(InventoryPlayer player,
            TileEntityMachineMyProcessor te) {
        super(new ContainerMyProcessor(player, te));
        this.processor = te;
        this.xSize = 176;
        this.ySize = 168;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Draw fluid tank tooltips when hovering
        this.drawFluidInfo(processor.inputTank, 26, 17, 18, 52);
        this.drawFluidInfo(processor.outputTank, 134, 17, 18, 52);

        // Draw power tooltip
        this.drawElectricityInfo(processor, 8, 17, 16, 52);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks,
            int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);

        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);

        // Power bar
        if(processor.getPower() > 0) {
            int p = (int) (processor.getPower() * 52 / processor.getMaxPower());
            drawTexturedModalRect(x + 8, y + 69 - p, 176, 52 - p, 16, p);
        }

        // Progress bar
        int progress = (int) (processor.module.progress * 27);
        drawTexturedModalRect(x + 80, y + 47, 176, 0, progress, 12);

        // Fluid tanks
        this.drawFluidTank(processor.inputTank, x + 26, y + 17, 18, 52);
        this.drawFluidTank(processor.outputTank, x + 134, y + 17, 18, 52);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Recipe selector button
        if(guiLeft + 152 <= mouseX && guiLeft + 168 <= mouseY
                && mouseX <= guiLeft + 168 && mouseY <= guiLeft + 84) {
            // Open recipe selector screen
            mc.displayGuiScreen(new GUIScreenRecipeSelector(this, processor));
        }
    }
}
```

**Place the GUI texture** at `src/main/resources/assets/hbm/textures/gui/processing/gui_crystallizer_alt.png`.

---

### 12.7 Step 7: Register Everything

#### 7a. Block Registration — `ModBlocks.java`

Edit `src/main/java/com/hbm/blocks/ModBlocks.java`. Three changes needed:

```java
// 1. DECLARE the block (near line 800, with other machine declarations)
public static Block machine_my_processor;

// 2. INITIALIZE it (in the init block, near line 2064)
machine_my_processor = new MachineMyProcessor()
    .setBlockName("machine_my_processor")
    .setHardness(5.0F).setResistance(10.0F)
    .setCreativeTab(MainRegistry.machineTab)
    .setBlockTextureName(RefStrings.MODID + ":machine_my_processor");

// 3. REGISTER it (in the register block, near line 3542)
GameRegistry.registerBlock(machine_my_processor,
    machine_my_processor.getUnlocalizedName());
```

Simpler alternative using the helper:
```java
register(machine_my_processor);
// This calls GameRegistry.registerBlock(b, ItemBlockBase.class, ...)
```

#### 7b. Tile Entity Mapping — `TileMappings.java`

Edit `src/main/java/com/hbm/tileentity/TileMappings.java`:

```java
// Add a single line to the static block (near line 193):
put(TileEntityMachineMyProcessor.class, "tileentity_my_processor");
```

#### 7c. TESR Binding — `ClientProxy.java`

If your machine has a custom renderer, edit `src/main/java/com/hbm/main/ClientProxy.java`:

```java
// Near line 321:
ClientRegistry.bindTileEntitySpecialRenderer(
    TileEntityMachineMyProcessor.class, new RenderMyProcessor());
```

If the machine uses the default block renderer (textured cube), this is not needed.

#### 7d. NEI Handler — `NEIRegistry.java`

Create a handler class in `src/main/java/com/hbm/handler/nei/NEIMyProcessorHandler.java`:

```java
package com.hbm.handler.nei;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.gui.GUIMyProcessor;
import com.hbm.inventory.recipes.MyProcessorRecipes;

public class NEIMyProcessorHandler extends NEIUniversalHandler {

    public NEIMyProcessorHandler() {
        super("My Processor", ModBlocks.machine_my_processor,
            MyProcessorRecipes.INSTANCE.getRecipes());
    }

    @Override
    public String getKey() {
        return "ntmMyProcessor";
    }

    @Override
    public void loadTransferRects() {
        super.loadTransferRects();
        // Register the clickable recipe transfer rectangle
        transferRectsGui.add(new RecipeTransferRect(
            new Rectangle(80, 47, 27, 12), "ntmMyProcessor"));
        guiGui.add(GUIMyProcessor.class);
        RecipeTransferRectHandler.registerRectsToGuis(guiGui, transferRectsGui);
    }
}
```

Then register it in `NEIRegistry.listAllHandlers()`:

```java
handlers.add(new NEIMyProcessorHandler());
```

#### 7e. Recipe Registration — `SerializableRecipe.java`

If your machine uses the module/recipe system, add to `SerializableRecipe.registerAllHandlers()`:

```java
public static void registerAllHandlers() {
    // ... existing handlers ...
    recipeHandlers.add(new MyProcessorRecipes());
    // ... more handlers ...
}
```

This ensures `MyProcessorRecipes.INSTANCE` is initialized and the JSON file is loaded.

---

### 12.8 Step 8: Localization

Add GUI name and tooltip entries to `src/main/resources/assets/hbm/lang/en_US.lang`:

```
tile.machine_my_processor.name=My Processor
```

---

### 12.9 Step 9: Adding Non-Standard Structure Shapes

If your machine needs a custom shape (not just a rectangular bounding box), override both `checkRequirement()` and `fillSpace()` to work with multiple volume zones:

```java
@Override
protected boolean checkRequirement(World world, int x, int y, int z,
        ForgeDirection dir, int o) {

    // Compute core position
    x += dir.offsetX * o;
    z += dir.offsetZ * o;

    // Main bounding box
    return MultiblockHandlerXR.checkSpace(world, x, y, z,
        getDimensions(), x, y, z, dir)

    // Extra zone: diagonal wings
    && MultiblockHandlerXR.checkSpace(world, x, y, z,
        new int[]{2, 0, 2, 2, 2, -2}, x, y, z, dir)
    && MultiblockHandlerXR.checkSpace(world, x, y, z,
        new int[]{2, 0, 2, 2, -2, 2}, x, y, z, dir);
}

@Override
public void fillSpace(World world, int x, int y, int z,
        ForgeDirection dir, int o) {

    x += dir.offsetX * o;
    z += dir.offsetZ * o;

    // Fill main bounding box
    MultiblockHandlerXR.fillSpace(world, x, y, z,
        getDimensions(), this, dir);

    // Fill extra diagonal zones
    MultiblockHandlerXR.fillSpace(world, x, y, z,
        new int[]{2, 0, 2, 2, 2, -2}, this, dir);
    MultiblockHandlerXR.fillSpace(world, x, y, z,
        new int[]{2, 0, 2, 2, -2, 2}, this, dir);
}
```

**Other overrides:**

| Method | Purpose | Example |
|--------|---------|---------|
| `getHeightOffset()` | Separate vertical offset from horizontal | `MachineExcavator` uses offset=3, heightOffset=3 |
| `getDirModified()` | Override facing direction calculation | `RBMKBase` forces `DIR_NO_LID`, `MachineLPW2` restricts rotation |
| `makeExtra()` | Mark specific dummies as IO-capable | Call in `fillSpace()` override at positions where pipes/cables should connect |

---

### 12.10 Complete File Checklist

| # | File | Path |
|---|------|------|
| 1 | Block class | `src/main/java/com/hbm/blocks/machine/MachineMyProcessor.java` |
| 2 | Tile Entity | `src/main/java/com/hbm/tileentity/machine/TileEntityMachineMyProcessor.java` |
| 3 | Module class | `src/main/java/com/hbm/module/machine/ModuleMyProcessor.java` |
| 4 | Recipe set | `src/main/java/com/hbm/inventory/recipes/MyProcessorRecipes.java` |
| 5 | Container | `src/main/java/com/hbm/inventory/container/ContainerMyProcessor.java` |
| 6 | GUI | `src/main/java/com/hbm/inventory/gui/GUIMyProcessor.java` |
| 7 | GUI texture | `src/main/resources/assets/hbm/textures/gui/processing/gui_my_processor.png` |
| 8 | NEI handler | `src/main/java/com/hbm/handler/nei/NEIMyProcessorHandler.java` |
| 9 | TESR renderer (optional) | `src/main/java/com/hbm/renderer/tileentity/RenderMyProcessor.java` |
| 10 | Lang entry | `src/main/resources/assets/hbm/lang/en_US.lang` |

**Registration edits (pre-existing files):**

| # | File | Edit |
|---|------|------|
| A | `ModBlocks.java` | Declare, init, register block |
| B | `TileMappings.java` | Add `put()` for TE class |
| C | `ClientProxy.java` | Bind TESR (if custom renderer) |
| D | `NEIRegistry.java` | Add handler to list |
| E | `SerializableRecipe.java` | Add to `registerAllHandlers()` |

---

### 12.11 Choosing the Right Pattern

| Aspect | Module-Based (Pattern A) | Direct Processing (Pattern B) |
|--------|-------------------------|-------------------------------|
| Recipe system | `GenericRecipes<GenericRecipe>` with JSON | Custom recipe map (HashMap, etc.) |
| Processing logic | Inherited from `ModuleMachineBase` | Custom in `updateEntity()` |
| Factory support | Drop-in: use same module with 4 instances | Manual slot replication |
| Best for | Standard input→process→output machines | Unique machines with custom logic |
| Examples | Assembly Machine, Chemical Plant | Mixer, Crystallizer, Cyclotron |

---

*Documentation generated from source analysis of `HBM's Nuclear Tech Mod` commit [`git rev-parse HEAD`].*
