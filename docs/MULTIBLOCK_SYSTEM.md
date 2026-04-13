# Multiblock Machine System Documentation

**Hbm's Nuclear Tech Mod**  
**Minecraft 1.7.10 / Forge 10.13.4**

---

## Table of Contents

1. [Overview](#overview)
2. [The Modern BlockDummyable System](#the-modern-blockdummyable-system)
3. [BlockDummyable Class Reference](#blockdummyable-class-reference)
4. [MultiblockHandlerXR](#multiblockhandlerxr)
5. [Tile Entity System](#tile-entity-system)
6. [Proxy and Delegate Pattern](#proxy-and-delegate-pattern)
7. [Collision and Rendering System](#collision-and-rendering-system)
8. [Copy/Paste and NBT Persistence](#copypaste-and-nbt-persistence)
9. [Recipe Processing System](#recipe-processing-system)
10. [Assembly Machine](#assembly-machine)
11. [Chemical Factory](#chemical-factory)
12. [Creating a New Multiblock Machine](#creating-a-new-multiblock-machine)
13. [Reference: Key Files](#reference-key-files)
14. [Appendix: Dimension Array](#appendix-dimension-array)
15. [Appendix: Metadata Reference](#appendix-metadata-reference)

---

## Overview

This mod implements complex multiblock machine structures where players place a single controller block that automatically generates the surrounding "dummy" blocks to form a complete structure. The system handles:

- Automatic multiblock validation and structure formation
- Tile entity communication between dummy blocks and the master
- Power and fluid distribution across the multiblock
- Recipe processing with configurable modules
- Upgrade systems for speed and power efficiency

The modern approach uses `BlockDummyable` as an abstract base class with configurable dimensions and automatic dummy placement. This system is used for all modern machines including the Assembly Machine and Chemical Factory.

---

## The Modern BlockDummyable System

### Core Concept

When a player places a multiblock controller block:

1. The system calculates the core position based on facing direction and offset
2. Validates that all required space is clear
3. Places the actual core block
4. Automatically fills surrounding positions with directional dummy blocks

### Structure Diagram

```
         TOP VIEW
         
    ┌─────────────────────────┐
    │  D  D  D  D  D  D  D   │
    │  D  E  E  E  E  E  D   │
    │  D  E  E  E  E  E  D   │
    │  D  E  E  C  E  E  D   │
    │  D  E  E  E  E  E  D   │
    │  D  E  E  E  E  E  D   │
    │  D  D  D  D  D  D  D   │
    └─────────────────────────┘
    
    C = Core (meta >= 12)
    E = Extra connector (meta 6-11)
    D = Dummy (meta 0-5)
    
    Controller placed by player → Core offset away
```

### Metadata Encoding

The `BlockDummyable` system uses block metadata to encode structure information:

| Metadata Range | Meaning | Tile Entity |
|----------------|---------|-------------|
| 0-5 | Dummy block pointing toward core | `null` |
| 6-11 | Extra/connector block | `TileEntityProxyCombo` |
| 12-15 | Core block (actual machine) | Machine-specific TE |

**Direction Encoding for Dummy blocks:**
| Metadata | Direction | Meaning |
|----------|-----------|---------|
| 0 | DOWN | Core is below this block |
| 1 | UP | Core is above this block |
| 2 | NORTH | Core is to the north |
| 3 | SOUTH | Core is to the south |
| 4 | WEST | Core is to the west |
| 5 | EAST | Core is to the east |

**Core Metadata:**
| Metadata | Facing Direction |
|----------|-----------------|
| 12 | NORTH (2 + 10) |
| 13 | SOUTH (3 + 10) |
| 14 | WEST (4 + 10) |
| 15 | EAST (5 + 10) |

---

## BlockDummyable Class Reference

### Static Fields

```java
public static final int offset = 10;  // Adds to direction to create core metadata
public static final int extra = 6;     // Adds to direction to create extra metadata
```

### Abstract Methods (Must Implement)

```java
// Returns 6-int array defining structure size: {UP, DOWN, NORTH, SOUTH, WEST, EAST}
// Each value = blocks extending in that direction from core
public abstract int[] getDimensions();

// Distance from placed controller block to actual core block
public abstract int getOffset();
```

### Optional Override Methods

```java
// Adjust vertical offset when placing on ceiling vs floor
protected int getHeightOffset() {
    return 0;
}

// Legacy single-block machine migration support
protected boolean isLegacyMonoblock(World world, int x, int y, int z) {
    return false;
}
protected void fixLegacyMonoblock(World world, int x, int y, int z) {
    world.setBlockMetadataWithNotify(x, y, z, offset + world.getBlockMetadata(x, y, z), 3);
}

// Modify core metadata after placement (e.g., for special orientations)
protected int getMetaForCore(World world, int x, int y, int z, EntityPlayer player, int original) {
    return original;
}

// Modify placement direction based on player facing
protected ForgeDirection getDirModified(ForgeDirection dir) {
    return dir;
}
```

### Block Lifecycle Methods

#### onBlockPlacedBy()

When a player places the controller block:

```java
public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack itemStack) {
    // 1. Determine facing direction from player yaw
    ForgeDirection facingDir = ...;
    
    // 2. Calculate core position using offset
    int o = -getOffset();
    int ox = x + facingDir.offsetX * o;
    int oy = y + getHeightOffset();
    int oz = z + facingDir.offsetZ * o;
    
    // 3. Validate space is clear
    if(!checkRequirement(world, ox - dir.offsetX * o, oy, oz - dir.offsetZ * o, dir, o)) {
        // Return item to player
        return;
    }
    
    // 4. Place core and fill dummies
    if(!world.isRemote) {
        world.setBlock(ox, oy, oz, this, getMetaForCore(...), 3);
        fillSpace(world, ox - dir.offsetX * o, oy, oz - dir.offsetZ * o, dir, o);
    }
}
```

#### onNeighborBlockChange()

Triggers orphan destruction check:

```java
public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
    super.onNeighborBlockChange(world, x, y, z, block);
    destroyIfOrphan(world, x, y, z);
}
```

#### updateTick()

Random tick handles orphan destruction and player tracking:

```java
public void updateTick(World world, int x, int y, int z, Random rand) {
    // Check if tracked players are still inside
    if(!internalPlayers.isEmpty()) {
        boolean anyStillInside = false;
        for(EntityPlayer player : internalPlayers) {
            if(isPlayerInside(world, player)) {
                anyStillInside = true;
                break;
            }
        }
        if(anyStillInside) {
            world.scheduleBlockUpdate(x, y, z, this, 1); // Reschedule
        } else {
            internalPlayers.clear();
        }
    }
    
    // Clean up orphaned dummy blocks
    destroyIfOrphan(world, x, y, z);
}
```

#### destroyIfOrphan()

Dummy blocks self-destruct if their parent core disappears:

```java
private void destroyIfOrphan(World world, int x, int y, int z) {
    if(world.isRemote) return;
    
    int metadata = world.getBlockMetadata(x, y, z);
    if(metadata >= extra) metadata -= extra; // Strip extra flag
    
    ForgeDirection dir = ForgeDirection.getOrientation(metadata).getOpposite();
    Block b = world.getBlock(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
    
    // Check chunk borders to prevent false deletions
    if(b != this && world.checkChunksExist(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1)) {
        if(isLegacyMonoblock(world, x, y, z)) {
            fixLegacyMonoblock(world, x, y, z); // Promote to core
        } else {
            world.setBlockToAir(x, y, z);
        }
    }
}
```

#### findCore()

Locates the core tile entity from any position in the structure:

```java
public int[] findCore(IBlockAccess world, int x, int y, int z) {
    positions.clear();
    return findCoreRec(world, x, y, z);
}

public int[] findCoreRec(IBlockAccess world, int x, int y, int z) {
    int metadata = world.getBlockMetadata(x, y, z);
    if(metadata >= extra) metadata -= extra;
    
    // Core has metadata 0 (UNKNOWN direction)
    if(world.getBlock(x, y, z) == this && ForgeDirection.getOrientation(metadata) == ForgeDirection.UNKNOWN)
        return new int[] { x, y, z };
    
    // Check for cycles
    ThreeInts pos = new ThreeInts(x, y, z);
    if(positions.contains(pos)) return null;
    
    // Move toward core (opposite of dummy's direction)
    ForgeDirection dir = ForgeDirection.getOrientation(metadata).getOpposite();
    Block b = world.getBlock(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
    if(b != this) return null;
    
    positions.add(pos);
    return findCoreRec(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
}
```

### Helper Methods

```java
// Validate space for multiblock
protected boolean checkRequirement(World world, int x, int y, int z, ForgeDirection dir, int o) {
    return MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, 
                                          z + dir.offsetZ * o, getDimensions(), x, y, z, dir);
}

// Place all dummy blocks
protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
    MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, 
                                  z + dir.offsetZ * o, getDimensions(), this, dir);
}

// Upgrade dummy to extra connector (meta + 6)
public void makeExtra(World world, int x, int y, int z) {
    if(world.getBlock(x, y, z) != this) return;
    int meta = world.getBlockMetadata(x, y, z);
    if(meta > 5) return;
    world.setBlock(x, y, z, this, meta + extra, 3);
}

// Downgrade extra to regular dummy
public void removeExtra(World world, int x, int y, int z) {
    if(world.getBlock(x, y, z) != this) return;
    int meta = world.getBlockMetadata(x, y, z);
    if(meta <= 5 || meta >= 12) return;
    world.setBlock(x, y, z, this, meta - extra, 3);
}

// Open GUI at core position when player interacts
protected boolean standardOpenBehavior(World world, int x, int y, int z, EntityPlayer player, int guiId) {
    if(world.isRemote) return true;
    if(player.isSneaking()) return true;
    int[] pos = this.findCore(world, x, y, z);
    if(pos == null) return false;
    FMLNetworkHandler.openGui(player, MainRegistry.instance, guiId, world, pos[0], pos[1], pos[2]);
    return true;
}
```

### Dimensions Array Format

The dimensions array follows the format `{UP, DOWN, NORTH, SOUTH, WEST, EAST}`:

```java
// Example: 5x3x5 structure centered on core
return new int[] {2, 0, 2, 2, 2, 2};
//   UP=2, DOWN=0, NORTH=2, SOUTH=2, WEST=2, EAST=2

// Example: Tower structure (3 wide, 8 tall, 3 deep)
return new int[] {4, 3, 1, 1, 1, 1};
//   UP=4, DOWN=3, NORTH=1, SOUTH=1, WEST=1, EAST=1
```

---

## MultiblockHandlerXR

The `MultiblockHandlerXR` class handles space validation and dummy block placement.

### checkSpace()

Validates that all positions in the multiblock footprint are empty or replaceable:

```java
public static boolean checkSpace(World world, int x, int y, int z, 
                                  int[] dim, int ox, int oy, int oz, ForgeDirection dir) {
    if(dim == null || dim.length != 6) return false;
    
    int count = 0;
    int[] rot = rotate(dim, dir);
    
    for(int a = x - rot[4]; a <= x + rot[5]; a++) {
        for(int b = y - rot[1]; b <= y + rot[0]; b++) {
            for(int c = z - rot[2]; c <= z + rot[3]; c++) {
                if(!world.getBlock(a, b, c).isReplaceable(world, a, b, c)) {
                    return false;
                }
                count++;
                if(count > 2000) return false; // Safety limit
            }
        }
    }
    return true;
}
```

### fillSpace()

Places all dummy blocks with correct directional metadata:

```java
public static void fillSpace(World world, int x, int y, int z, 
                              int[] dim, Block block, ForgeDirection dir) {
    if(dim == null || dim.length != 6) return;
    
    int count = 0;
    int[] rot = rotate(dim, dir);
    
    // Temporarily disable orphan checks during placement
    BlockDummyable.safeRem = true;
    
    for(int a = x - rot[4]; a <= x + rot[5]; a++) {
        for(int b = y - rot[1]; b <= y + rot[0]; b++) {
            for(int c = z - rot[2]; c <= z + rot[3]; c++) {
                int meta = 0;
                
                // Axis-priority: Y first, then X, then Z
                if(b < y) {
                    meta = ForgeDirection.DOWN.ordinal();    // 1
                } else if(b > y) {
                    meta = ForgeDirection.UP.ordinal();      // 2
                } else if(a < x) {
                    meta = ForgeDirection.WEST.ordinal();     // 6
                } else if(a > x) {
                    meta = ForgeDirection.EAST.ordinal();     // 5
                } else if(c < z) {
                    meta = ForgeDirection.NORTH.ordinal();   // 3
                } else if(c > z) {
                    meta = ForgeDirection.SOUTH.ordinal();    // 4
                } else {
                    continue; // Skip origin (core position)
                }
                
                world.setBlock(a, b, c, block, meta, 3);
                count++;
                if(count > 2000) {
                    BlockDummyable.safeRem = false;
                    return;
                }
            }
        }
    }
    
    BlockDummyable.safeRem = false;
}
```

### rotate()

Rotates dimension arrays based on multiblock facing direction:

```java
public static int[] rotate(int[] dim, ForgeDirection dir) {
    if(dim == null) return null;
    
    // SOUTH (ordinal 4) is the identity/base case
    if(dir == ForgeDirection.SOUTH) return dim;
    
    if(dir == ForgeDirection.NORTH) {
        // 180° rotation: N↔S, W↔E
        return new int[] { dim[0], dim[1], dim[3], dim[2], dim[5], dim[4] };
    }
    
    if(dir == ForgeDirection.EAST) {
        // 90° clockwise: N→E, S→W, W→N, E→S
        return new int[] { dim[0], dim[1], dim[5], dim[4], dim[2], dim[3] };
    }
    
    if(dir == ForgeDirection.WEST) {
        // 90° counter-clockwise: N→W, S→E, W→S, E→N
        return new int[] { dim[0], dim[1], dim[4], dim[5], dim[3], dim[2] };
    }
    
    return dim;
}
```

**Rotation lookup table:**

| Original | →SOUTH | →NORTH | →EAST | →WEST |
|----------|--------|--------|-------|-------|
| NORTH | N | S | E | W |
| SOUTH | S | N | W | E |
| WEST | W | E | N | S |
| EAST | E | W | S | N |

---

## Tile Entity System

### TileEntityDummy

Dummy blocks use a lightweight tile entity that stores a reference to the core for orphan detection:

```java
public class TileEntityDummy extends TileEntity {
    public int targetX;
    public int targetY;
    public int targetZ;
    
    @Override
    public void updateEntity() {
        if(!this.worldObj.isRemote) {
            // Self-destruct if core is missing
            if(!(this.worldObj.getBlock(targetX, targetY, targetZ) instanceof IMultiblock)) {
                worldObj.func_147480_a(xCoord, yCoord, zCoord, false);
            }
        }
    }
    
    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("tx", targetX);
        nbt.setInteger("ty", targetY);
        nbt.setInteger("tz", targetZ);
    }
    
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        targetX = nbt.getInteger("tx");
        targetY = nbt.getInteger("ty");
        targetZ = nbt.getInteger("tz");
    }
}
```

### TileEntityProxyBase

Base class for proxy tile entities. Provides core-finding functionality:

```java
public class TileEntityProxyBase extends TileEntityLoadedBase {
    public BlockPos cachedPosition;
    
    public boolean canUpdate() { return false; }
    
    public TileEntity getTE() {
        if(worldObj == null) return null;
        
        // Check cached position first
        if(cachedPosition != null) {
            TileEntity te = Compat.getTileStandard(worldObj, cachedPosition.x, cachedPosition.y, cachedPosition.z);
            if(te != null && !(te instanceof TileEntityProxyBase)) return te;
            cachedPosition = null;
            this.markDirty();
        }
        
        // Use BlockDummyable's core finder
        if(this.getBlockType() instanceof BlockDummyable) {
            BlockDummyable dummy = (BlockDummyable) this.getBlockType();
            int[] pos = dummy.findCore(worldObj, xCoord, yCoord, zCoord);
            if(pos != null) {
                TileEntity te = Compat.getTileStandard(worldObj, pos[0], pos[1], pos[2]);
                if(te != null && !(te instanceof TileEntityProxyBase)) return te;
            }
        }
        return null;
    }
    
    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if(this.cachedPosition != null) {
            nbt.setBoolean("hasPos", true);
            nbt.setInteger("pX", this.cachedPosition.getX());
            nbt.setInteger("pY", this.cachedPosition.getY());
            nbt.setInteger("pZ", this.cachedPosition.getZ());
        }
    }
    
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if(nbt.getBoolean("hasPos")) 
            cachedPosition = new BlockPos(nbt.getInteger("pX"), nbt.getInteger("pY"), nbt.getInteger("pZ"));
    }
}
```

### TileEntityProxyCombo

The main proxy implementation that delegates functionality to the core. Uses a builder pattern:

```java
public class TileEntityProxyCombo extends TileEntityProxyBase 
        implements IEnergyReceiverMK2, IEnergyConductorMK2, ISidedInventory, 
                   IFluidReceiverMK2, IHeatSource, ICrucibleAcceptor {
    
    boolean inventory;  // Enables item I/O
    boolean power;     // Enables power transfer
    boolean conductor; // Enables power network nodes
    boolean fluid;     // Enables fluid handling
    boolean heat;      // Enables heat source
    public boolean moltenMetal; // Enables crucible
    
    // Builder methods
    public TileEntityProxyCombo inventory() { this.inventory = true; return this; }
    public TileEntityProxyCombo power() { this.power = true; return this; }
    public TileEntityProxyCombo conductor() { this.conductor = true; return this; }
    public TileEntityProxyCombo fluid() { this.fluid = true; return this; }
    public TileEntityProxyCombo heatSource() { this.heat = true; return this; }
    public TileEntityProxyCombo moltenMetal() { this.moltenMetal = true; return this; }
    
    // Example: delegate power to core
    @Override
    public long getPower() {
        if(!power) return 0;
        if(getCoreObject() instanceof IEnergyReceiverMK2) {
            return ((IEnergyReceiverMK2)getCoreObject()).getPower();
        }
        return 0;
    }
    
    // Example: delegate fluid tanks to core
    @Override
    public FluidTank[] getAllTanks() {
        if(!fluid) return EMPTY_TANKS;
        if(getCoreObject() instanceof IFluidReceiverMK2) {
            return ((IFluidReceiverMK2)getCoreObject()).getAllTanks();
        }
        return EMPTY_TANKS;
    }
}
```

### TileEntity Creation by Metadata

The block's `createNewTileEntity()` returns different tile entities based on metadata:

```java
@Override
public TileEntity createNewTileEntity(World world, int meta) {
    if(meta >= 12) {
        return new TileEntityMachineAssemblyMachine();  // Master tile entity
    }
    if(meta >= 6) {
        return new TileEntityProxyCombo().inventory().power().fluid();  // I/O proxy
    }
    return null;  // Dummy blocks (meta 0-5) have no tile entity
}
```

---

## Proxy and Delegate Pattern

### The Problem

Multiblock machines need multiple connection points for power and fluid I/O. However, some connection points (like cooling lines) should only access specific tanks, while others should access all tanks.

### IProxyDelegateProvider Interface

Allows a machine to provide position-specific delegates:

```java
public interface IProxyDelegateProvider {
    /**
     * Returns a delegate object for the given position, or null to use
     * the master tile entity directly.
     */
    Object getDelegateForPosition(int x, int y, int z);
}
```

### TileEntityProxyDyn

Extends TileEntityProxyCombo with dynamic delegation:

```java
public class TileEntityProxyDyn extends TileEntityProxyCombo {
    
    @Override
    public Object getCoreObject() {
        Object o = super.getTile(); // Get master tile entity
        
        // Ask master for position-specific delegate
        if(o instanceof IProxyDelegateProvider) {
            Object delegate = ((IProxyDelegateProvider) o)
                .getDelegateForPosition(xCoord, yCoord, zCoord);
            if(delegate != null) return delegate;
        }
        
        return o;
    }
}
```

### Delegate Implementation Example

```java
public class CoolantOnlyDelegate implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2 {
    private final TileEntityMachineChemicalFactory factory;
    
    public CoolantOnlyDelegate(TileEntityMachineChemicalFactory factory) {
        this.factory = factory;
    }
    
    @Override public long getPower() { return factory.getPower(); }
    @Override public long getMaxPower() { return factory.getMaxPower(); }
    
    // Limited: Only coolant tanks!
    @Override public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { factory.water };
    }
    @Override public FluidTank[] getSendingTanks() {
        return new FluidTank[] { factory.lps };
    }
    @Override public FluidTank[] getAllTanks() {
        return factory.getAllTanks();
    }
}
```

### Position Matching

```java
@Override
public Object getDelegateForPosition(int x, int y, int z) {
    // Define coolant line positions
    if(coolantLine == null) {
        coolantLine = new DirPos[] {
            new DirPos(xCoord + 1, yCoord, zCoord + 2),
            new DirPos(xCoord - 1, yCoord, zCoord + 2),
            new DirPos(xCoord + 1, yCoord, zCoord - 2),
            new DirPos(xCoord - 1, yCoord, zCoord - 2),
        };
    }
    
    // Check if this position is on coolant line
    for(DirPos pos : coolantLine) {
        if(pos.compare(x, y, z)) {
            return this.coolantDelegate; // Return limited delegate
        }
    }
    
    return null; // Use full machine for standard I/O
}
```

### Communication Flow

```
External Pipe/Cable
        │
        ▼
TileEntityProxyDyn (at position)
        │
        ▼ getCoreObject()
   ┌────┴────┐
   │ Master  │
   │ TileEntity │
   └────┬────┘
        │ getDelegateForPosition(x,y,z)
        ▼
   ┌────┴────┐
   │ Position │
   │ matches? │
   └──┬──┬───┘
      │  │
   Yes │  │ No
      │  │
      ▼  ▼
  Delegate  Master TE
  (limited) (full)
```

---

## Collision and Rendering System

### Custom Collision Boxes

For complex structures spanning multiple blocks, `BlockDummyable` supports custom collision geometry:

```java
public List<AxisAlignedBB> bounding = new ArrayList<>();

// Enable custom collision
public boolean useDetailedHitbox() {
    return !bounding.isEmpty();
}
```

### Adding Collision Boxes

```java
// Example: Launch pad collision
bounding.add(AxisAlignedBB.getBoundingBox(-1.5, 0, -1.5, -0.5, 1, -0.5));
bounding.add(AxisAlignedBB.getBoundingBox(0.5, 0, -1.5, 1.5, 1, -0.5));
bounding.add(AxisAlignedBB.getBoundingBox(-1.5, 0, 0.5, -0.5, 1, 1.5));
bounding.add(AxisAlignedBB.getBoundingBox(0.5, 0, 0.5, 1.5, 1, 1.5));
bounding.add(AxisAlignedBB.getBoundingBox(-0.5, 0.5, -1.5, 0.5, 1, 1.5));
bounding.add(AxisAlignedBB.getBoundingBox(-1.5, 0.5, -0.5, 1.5, 1, 0.5));
```

### Collision List Building

```java
@Override
public void addCollisionBoxesToList(World world, int x, int y, int z, 
        AxisAlignedBB entityBounding, List list, Entity entity) {
    
    // Skip if entity is tracked player inside multiblock
    if(!internalPlayers.isEmpty() && internalPlayers.contains(entity))
        return;
    
    // Find core for rotation
    int[] pos = this.findCore(world, x, y, z);
    if(pos == null) return;
    
    // Calculate rotation from core metadata
    ForgeDirection rot = ForgeDirection.getOrientation(
        world.getBlockMetadata(pos[0], pos[1], pos[2]) - offset
    ).getRotation(ForgeDirection.UP);
    
    // Add each rotated AABB
    for(AxisAlignedBB aabb : this.bounding) {
        AxisAlignedBB boxlet = getAABBRotationOffset(aabb, pos[0] + 0.5, pos[1], pos[2] + 0.5, rot);
        if(entityBounding.intersectsWith(boxlet)) {
            list.add(boxlet);
        }
    }
}
```

### AABB Rotation

```java
public static AxisAlignedBB getAABBRotationOffset(AxisAlignedBB aabb, 
        double x, double y, double z, ForgeDirection dir) {
    AxisAlignedBB newBox = null;
    
    if(dir == ForgeDirection.NORTH) 
        newBox = AxisAlignedBB.getBoundingBox(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
    if(dir == ForgeDirection.EAST)  
        newBox = AxisAlignedBB.getBoundingBox(-aabb.maxZ, aabb.minY, aabb.minX, -aabb.minZ, aabb.maxY, aabb.maxX);
    if(dir == ForgeDirection.SOUTH) 
        newBox = AxisAlignedBB.getBoundingBox(-aabb.maxX, aabb.minY, -aabb.maxZ, -aabb.minX, aabb.maxY, -aabb.minZ);
    if(dir == ForgeDirection.WEST)  
        newBox = AxisAlignedBB.getBoundingBox(aabb.minZ, aabb.minY, -aabb.maxX, aabb.maxZ, aabb.maxY, -aabb.minX);
    
    if(newBox != null) {
        newBox.offset(x, y, z);
        return newBox;
    }
    return AxisAlignedBB.getBoundingBox(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ)
        .offset(x + 0.5, y + 0.5, z + 0.5);
}
```

### Ray Tracing Override

For accurate block interaction with custom hitboxes:

```java
@Override
public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, 
        Vec3 startVec, Vec3 endVec) {
    
    int[] pos = this.findCore(world, x, y, z);
    if(pos == null) return super.collisionRayTrace(world, x, y, z, startVec, endVec);
    
    ForgeDirection rot = ForgeDirection.getOrientation(
        world.getBlockMetadata(pos[0], pos[1], pos[2]) - offset
    ).getRotation(ForgeDirection.UP);
    
    for(AxisAlignedBB aabb : this.bounding) {
        AxisAlignedBB boxlet = getAABBRotationOffset(aabb, pos[0] + 0.5, pos[1], pos[2] + 0.5, rot);
        MovingObjectPosition intercept = boxlet.calculateIntercept(startVec, endVec);
        if(intercept != null) {
            return new MovingObjectPosition(x, y, z, intercept.sideHit, intercept.hitVec);
        }
    }
    return null;
}
```

---

## Copy/Paste and NBT Persistence

### ICopiable Interface

Enables copy/paste functionality for machine configuration:

```java
public interface ICopiable {
    NBTTagCompound getSettings(World world, int x, int y, int z);
    void pasteSettings(NBTTagCompound nbt, int index, World world, EntityPlayer player, int x, int y, int z);
    default String[] infoForDisplay(World world, int x, int y, int z) { return null; }
}
```

### Block-Level Implementation

```java
@Override
public NBTTagCompound getSettings(World world, int x, int y, int z) {
    int[] pos = findCore(world, x, y, z);
    TileEntity tile = world.getTileEntity(pos[0], pos[1], pos[2]);
    if(tile instanceof ICopiable) {
        return ((ICopiable) tile).getSettings(world, pos[0], pos[1], pos[2]);
    }
    return null;
}

@Override
public void pasteSettings(NBTTagCompound nbt, int index, World world, EntityPlayer player, int x, int y, int z) {
    int[] pos = findCore(world, x, y, z);
    if(pos == null) return;
    TileEntity tile = world.getTileEntity(pos[0], pos[1], pos[2]);
    if(tile instanceof ICopiable) {
        ((ICopiable) tile).pasteSettings(nbt, index, world, player, pos[0], pos[1], pos[2]);
    }
}
```

### INBTBlockTransformable Interface

Handles metadata transformation during world generation from NBT structures:

```java
public interface INBTBlockTransformable {
    int transformMeta(int meta, int coordBaseMode);
    default Block transformBlock(Block block) { return block; }
}
```

The `coordBaseMode` values: 0=South, 1=West, 2=North, 3=East

---

## Recipe Processing System

### ModuleMachineBase

The base class for all recipe processing modules:

```java
public abstract class ModuleMachineBase {
    public int index;
    public IEnergyHandlerMK2 battery;
    public ItemStack[] slots;
    public int[] inputSlots;
    public int[] outputSlots;
    public FluidTank[] inputTanks;
    public FluidTank[] outputTanks;
    
    // Running state
    public String recipe;        // Current recipe name
    public double progress;      // 0.0 to 1.0
    public boolean didProcess;   // Processing occurred this tick
    public boolean markDirty;    // Needs save
    
    public ModuleMachineBase(int index, IEnergyHandlerMK2 battery, ItemStack[] slots) { ... }
}
```

### update() Method

Called each tick to process recipes:

```java
public void update(double speed, double power, boolean extraCondition, ItemStack blueprint) {
    GenericRecipe recipe = getRecipe();
    
    // Pooled recipes require matching blueprint
    if(recipe != null && recipe.isPooled() && !recipe.isPartOfPool(ItemBlueprints.grabPool(blueprint))) {
        this.didProcess = false;
        this.progress = 0F;
        this.recipe = "null";
        return;
    }
    
    this.setupTanks(recipe);
    this.didProcess = false;
    this.markDirty = false;
    
    if(extraCondition && this.canProcess(recipe, speed, power)) {
        this.process(recipe, speed, power);
        this.didProcess = true;
    } else {
        this.progress = 0F;
    }
}
```

### canProcess() Validation

```java
public boolean canProcess(GenericRecipe recipe, double speed, double power) {
    if(recipe == null) return false;
    
    // Auto-switch: check for different matching recipe input
    if(recipe.autoSwitchGroup != null && slots[inputSlots[0]] != null) {
        List<GenericRecipe> group = this.getRecipeSet().autoSwitchGroups.get(recipe.autoSwitchGroup);
        for(GenericRecipe nextRec : group) {
            if(nextRec.inputItem != null && nextRec.inputItem[0].matchesRecipe(slots[inputSlots[0]], true)) {
                this.recipe = nextRec.getInternalName();
                return false; // Switch and retry next tick
            }
        }
    }
    
    // Power check
    if(power != 1 && battery.getPower() < recipe.power * power) return false;
    if(power == 1 && battery.getPower() < recipe.power) return false;
    
    // Input availability
    if(!hasInput(recipe)) return false;
    
    // Output capacity
    return canFitOutput(recipe);
}
```

### process() Completion

```java
public void process(GenericRecipe recipe, double speed, double power) {
    // Consume power
    this.battery.setPower(this.battery.getPower() - 
        (power == 1 ? recipe.power : (long)(recipe.power * power)));
    
    // Advance progress
    double step = Math.min(speed / recipe.duration, 1D);
    this.progress += step;
    
    // Check completion
    if(this.progress >= 1D) {
        consumeInput(recipe);
        produceItem(recipe);
        
        // Continuous processing or reset
        if(this.canProcess(recipe, speed, power)) 
            this.progress -= 1D;
        else 
            this.progress = 0D;
    }
}
```

### Recipe Definition

```java
GenericRecipe recipe = new GenericRecipe("recipe_id")
    .setup(60, 100)                          // duration=60 ticks, power=100 HE/t
    .inputItems(new OreDictStack(IRON.ingot()))  // 1 iron ingot
    .inputFluids(new FluidStack(Fluids.WATER, 1000))  // 1000 mB water
    .outputItems(new ItemStack(ModItems.plate_iron))  // 1 iron plate
    .outputFluids(new FluidStack(Fluids.STEAM, 500)); // 500 mB steam
```

### JSON Recipe Format

```json
{
  "recipes": [
    {
      "name": "chem.hydrogen",
      "inputItem": [["dict", "coal", 1]],
      "inputFluid": [["water", 8000, 0]],
      "outputFluid": [["hydrogen", 500, 0]],
      "duration": 20,
      "power": 400,
      "icon": ["gas_full", "hydrogen"]
    }
  ]
}
```

### Auto-Switch Groups

Recipes can be grouped for automatic switching based on input:

```java
this.register(new GenericRecipe("ass.plateiron")
    ...
    .setGroup("autoswitch.plates", this));

this.register(new GenericRecipe("ass.plategold")
    ...
    .setGroup("autoswitch.plates", this));
```

When processing, if the first input slot contains a different matching recipe's input, it automatically switches recipes.

---

## Assembly Machine

The Assembly Machine is a single-module manufacturing block that processes items using power and optional fluids.

### Structure

| Property | Value |
|----------|-------|
| Dimensions | `{2, 0, 1, 1, 1, 1}` |
| Core Offset | 1 block |
| Footprint | 5x3x3 blocks |

### Block Definition

```java
public class MachineAssemblyMachine extends BlockDummyable {
    @Override
    public int[] getDimensions() { return new int[] {2, 0, 1, 1, 1, 1}; }
    @Override
    public int getOffset() { return 1; }
    
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        if(meta >= 12) return new TileEntityMachineAssemblyMachine();
        if(meta >= 6) return new TileEntityProxyCombo().inventory().power().fluid();
        return null;
    }
    
    @Override
    protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);
        // Add connector blocks in 3x3 pattern around core
        for(int i = -1; i <= 1; i++) for(int j = -1; j <= 1; j++) {
            if(i != 0 || j != 0) this.makeExtra(world, x + i, y, z + j);
        }
    }
}
```

### Inventory Slots (17 Total)

| Slot | Position | Purpose |
|------|----------|---------|
| 0 | Battery bay | Any battery item charges the machine |
| 1 | Schematic slot | `ItemBlueprints` enables pooled recipes |
| 2-3 | Upgrade slots | SPEED, POWER, or OVERDRIVE upgrades |
| 4-15 | Input grid | 12-slot ingredient input (4x3 grid) |
| 16 | Output slot | Crafting output |

### Fluid Tanks

| Tank | Capacity | Purpose |
|------|----------|---------|
| Input | 4,000 mB | Fluid ingredient consumption |
| Output | 4,000 mB | Fluid product output |

Both tanks resize dynamically based on recipe requirements.

### Power System

| Property | Value |
|----------|-------|
| Base maxPower | 100,000 |
| Recipe-scaled max | `recipe.power * 100` |
| Power floor | `max(currentPower, maxPower, 100_000)` |

### Upgrade Effects

| Upgrade | Max Level | Speed Effect | Power Effect |
|---------|-----------|-------------|--------------|
| SPEED | 3 | `+33%` per level | `+100%` per level consumption |
| POWER | 3 | None | `-25%` per level consumption |
| OVERDRIVE | 3 | `+100%` per level | `+333%` per level consumption |

### Connection Points

The machine has 12 connection points arranged in a cross pattern around the core:

```
    [-1,-2] [0,-2] [1,-2]
    [-2,-1] [-2,0] [-2,1]
    
    [2,-1]  [2,0]  [2,1]
    [-1,2]  [0,2]  [1,2]
```

Each connection point subscribes to power, input fluid (if present), and provides output fluid (if filled).

### Animation System

The Assembly Machine features animated arms during processing:

**AssemblerArm Structure:**
- 2 independent arms
- 4 joints each: Pivot, Arm, Piston, Striker
- State machine: `ASSUME_POSITION` → `EXTEND_STRIKER` → `RETRACT_STRIKER`

**Ring Animation:**
- Central rotating dial
- Random speed variations
- Speed up during processing, slow when idle

### Audio System

| Sound | Trigger | Volume |
|-------|---------|--------|
| Electric motor loop | While processing | 0.5 base |
| Assembler start | Ring reaches target | 0.25 |
| Assembler strike | Hammer contact | 0.5 |
| Assembler stop | Processing ends | 0.25 |

---

## Chemical Factory

The Chemical Factory is a large multi-module machine for complex chemical processing.

### Structure

| Property | Value |
|----------|-------|
| Dimensions | `{2, 0, 2, 2, 2, 2}` |
| Core Offset | 2 blocks |
| Footprint | 5x3x5 blocks |
| Parallel Modules | 4 |

### Processing Modules

Each of the 4 modules has:
- **3 item input slots**
- **3 item output slots**
- **3 fluid input tanks** (24,000 mB each)
- **3 fluid output tanks** (24,000 mB each)

### Tank Comparison

| Factory | Tank Capacity | Fluid Tanks/Module |
|---------|---------------|-------------------|
| Assembly Machine | 4,000 mB | 2 (1 in, 1 out) |
| Chemical Factory | 24,000 mB | 6 (3 in, 3 out) |

### Delegate Pattern

The Chemical Factory uses `IProxyDelegateProvider` to separate coolant from recipe fluids:

```java
public class DelegateChemicalFactory implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2 {
    private final TileEntityMachineChemicalFactory factory;
    
    // Full power access
    @Override public long getPower() { return factory.getPower(); }
    @Override public long getMaxPower() { return factory.getMaxPower(); }
    
    // Limited: Only coolant tanks!
    @Override public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { factory.water };
    }
    @Override public FluidTank[] getSendingTanks() {
        return new FluidTank[] { factory.lps };
    }
    @Override public FluidTank[] getAllTanks() {
        return factory.getAllTanks();
    }
}
```

### Fluid Consolidation

The Chemical Factory optimizes tank space by moving matching fluids:

```java
for(FluidTank in : inputTanks) {
    for(FluidTank out : outputTanks) {
        if(out.getTankType() == in.getTankType()) {
            int toMove = Math.min(in.getMaxFill() - in.getFill(), out.getFill(), 50);
            in.setFill(in.getFill() + toMove);
            out.setFill(out.getFill() - toMove);
        }
    }
}
```

### Recipe Types

- **Chemicals**: Hydrogen, CO2, biogas, rocket fuel
- **Materials**: Polymers, rubber, kevlar, bakelite
- **Nuclear**: UF6, PUF6, yellowcake
- **Batteries**: Lead, lithium, sodium, quantum packs

---

## Creating a New Multiblock Machine

### Step 1: Create Block Definition

```java
public class MachineMyFactory extends BlockDummyable {
    
    public static final String name = "my_factory";
    
    public MachineMyFactory() {
        super(Material.iron);
        setUnlocalizedName(name);
        setCreativeTab(CreativeTabs.tabDecorations);
    }
    
    @Override
    public int[] getDimensions() {
        return new int[] {2, 0, 1, 1, 1, 1}; // 5x3x3 structure
    }
    
    @Override
    public int getOffset() {
        return 1; // Core is 1 block from controller
    }
    
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        if(meta >= 12) return new TileEntityMachineMyFactory();
        if(meta >= 6) return new TileEntityProxyCombo().inventory().power().fluid();
        return null;
    }
    
    @Override
    protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);
        // Add connector blocks
        for(int i = -1; i <= 1; i++) for(int j = -1; j <= 1; j++) {
            if(i != 0 || j != 0) this.makeExtra(world, x + i, y, z + j);
        }
    }
    
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        return this.standardOpenBehavior(world, x, y, z, player, 0);
    }
}
```

### Step 2: Create Tile Entity

```java
public class TileEntityMachineMyFactory extends TileEntityMachineBase 
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, 
                   IProxyDelegateProvider, IGUIProvider {
    
    public long power;
    public long maxPower = 1_000_000;
    
    public FluidTank inputTank;
    public FluidTank outputTank;
    
    public ModuleMachineAssembler assemblerModule;
    public UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);
    
    public TileEntityMachineMyFactory() {
        super(17); // 17 inventory slots
        this.inputTank = new FluidTank(Fluids.NONE, 4000);
        this.outputTank = new FluidTank(Fluids.NONE, 4000);
        
        this.assemblerModule = new ModuleMachineAssembler(0, this, slots)
            .itemInput(4).itemOutput(16)
            .fluidInput(inputTank).fluidOutput(outputTank);
    }
    
    @Override
    public void updateEntity() {
        if(!worldObj.isRemote) {
            // Power from battery
            this.power = Library.chargeTEFromItems(slots, 0, power, maxPower);
            
            // Upgrade management
            upgradeManager.checkSlots(slots, 2, 3);
            
            // Subscribe to networks
            for(DirPos pos : getConPos()) {
                this.trySubscribe(worldObj, pos);
            }
            
            // Calculate upgrade modifiers
            double speed = 1.0 + Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3) / 3.0;
            double pow = 1.0 - Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3) * 0.25;
            
            // Process recipe
            assemblerModule.update(speed, pow, true, slots[1]);
        }
    }
    
    public DirPos[] getConPos() {
        return new DirPos[] {
            new DirPos(xCoord + 2, yCoord, zCoord - 1, Library.POS_X),
            new DirPos(xCoord + 2, yCoord, zCoord, Library.POS_X),
            new DirPos(xCoord + 2, yCoord, zCoord + 1, Library.POS_X),
            new DirPos(xCoord - 2, yCoord, zCoord - 1, Library.NEG_X),
            new DirPos(xCoord - 2, yCoord, zCoord, Library.NEG_X),
            new DirPos(xCoord - 2, yCoord, zCoord + 1, Library.NEG_X),
            // ... add remaining positions
        };
    }
    
    // IProxyDelegateProvider implementation
    @Override
    public Object getDelegateForPosition(int x, int y, int z) {
        return null; // Use full machine for all positions
    }
    
    // IGUIProvider implementation
    @Override
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerMachineMyFactory(player.inventory, this);
    }
    
    @Override @SideOnly(Side.CLIENT)
    public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUIMachineMyFactory(player.inventory, this);
    }
    
    // Power interface
    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    
    // Fluid interface
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] {inputTank}; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] {outputTank}; }
    @Override public FluidTank[] getAllTanks() { return new FluidTank[] {inputTank, outputTank}; }
}
```

### Step 3: Create Recipes

```java
public class MyRecipes extends GenericRecipes<GenericRecipe> {
    public static MyRecipes INSTANCE = new MyRecipes();
    
    @Override public int inputItemLimit() { return 12; }
    @Override public int inputFluidLimit() { return 1; }
    @Override public int outputItemLimit() { return 1; }
    @Override public int outputFluidLimit() { return 1; }
    @Override public String getFileName() { return "hbmMyFactory.json"; }
    @Override public GenericRecipe instantiateRecipe(String name) { return new GenericRecipe(name); }
    
    @Override
    public void registerDefaults() {
        register(new GenericRecipe("my.example")
            .setup(60, 100)
            .inputItems(new OreDictStack(IRON.ingot()))
            .outputItems(new ItemStack(ModItems.plate_iron, 1)));
    }
}
```

### Step 4: Register

```java
// In ModBlocks.java
public static BlockMachine my_factory;

// In init()
my_factory = new MachineMyFactory().setHardness(6.0F).setStepSound(Block.soundTypeMetal);

// In register()
GameRegistry.registerBlock(my_factory, ItemBlock.class, "my_factory");
GameRegistry.registerTileEntity(TileEntityMachineMyFactory.class, "my_factory");

// In registerRecipes()
MyRecipes.INSTANCE.loadRecipes();
```

---

## Reference: Key Files

### Core Multiblock System

| File | Description |
|------|-------------|
| `BlockDummyable.java` | Base class for multiblock blocks |
| `MultiblockHandlerXR.java` | Space validation and dummy placement |
| `TileEntityDummy.java` | Lightweight tile for dummy blocks |
| `TileEntityProxyBase.java` | Base proxy with core-finding logic |
| `TileEntityProxyCombo.java` | Main proxy with capability delegation |
| `TileEntityProxyDyn.java` | Dynamic proxy with position delegates |

### Assembly Machine

| File | Description |
|------|-------------|
| `MachineAssemblyMachine.java` | Block definition |
| `TileEntityMachineAssemblyMachine.java` | Master tile entity |
| `ContainerMachineAssemblyMachine.java` | Inventory container |
| `GUIMachineAssemblyMachine.java` | Client GUI |
| `AssemblyMachineRecipes.java` | Recipe definitions |

### Recipe System

| File | Description |
|------|-------------|
| `GenericRecipes.java` | Base recipe container |
| `GenericRecipe.java` | Recipe definition class |
| `ModuleMachineBase.java` | Processing module base class |
| `ModuleMachineAssembler.java` | Assembly machine module |

### Interfaces

| Interface | Package | Purpose |
|-----------|---------|---------|
| `IProxyDelegateProvider` | `tileentity` | Position-aware delegation |
| `IEnergyReceiverMK2` | `api` | Power reception |
| `IFluidReceiverMK2` | `api` | Fluid reception |
| `IFluidStandardTransceiverMK2` | `api` | Fluid I/O |
| `IGUIProvider` | `tileentity` | GUI container/GUI provision |
| `ICopiable` | `interfaces` | Copy/paste support |
| `INBTBlockTransformable` | `interfaces` | NBT structure transformation |

---

## Appendix: Dimension Array

Dimensions define the multiblock bounds as `{UP, DOWN, NORTH, SOUTH, WEST, EAST}`:

```
      ┌─────────────────┐
      │     +Y (UP)      │
      │        N         │
      ├────────O─────────┤
      │ W     ⬤     E    │   ⬤ = Core block
      │     -Y           │
      │   (DOWN)         │
      └─────────────────┘
```

| Direction | Index | Meaning |
|-----------|-------|---------|
| UP | 0 | Blocks extending above core |
| DOWN | 1 | Blocks extending below core |
| NORTH | 2 | Blocks extending north of core |
| SOUTH | 3 | Blocks extending south of core |
| WEST | 4 | Blocks extending west of core |
| EAST | 5 | Blocks extending east of core |

---

## Appendix: Metadata Reference

| Range | Meaning | Tile Entity |
|-------|---------|-------------|
| 0-5 | Dummy direction | `null` |
| 6-11 | Extra connector | `TileEntityProxyCombo` |
| 12-15 | Core block | Machine-specific TE |

**Dummy Direction Metadata:**
| Value | Direction |
|-------|-----------|
| 0 | DOWN |
| 1 | UP |
| 2 | NORTH |
| 3 | SOUTH |
| 4 | WEST |
| 5 | EAST |

**Core Metadata:**
| Value | Facing |
|-------|--------|
| 12 | NORTH |
| 13 | SOUTH |
| 14 | WEST |
| 15 | EAST |

---

*Document generated for Hbm's Nuclear Tech GIT-NTNH*  
*Minecraft 1.7.10 / Forge 10.13.4*
