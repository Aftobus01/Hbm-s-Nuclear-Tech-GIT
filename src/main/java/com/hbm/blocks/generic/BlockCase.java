package com.hbm.blocks.generic;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class BlockCase extends Block {

	public static int renderID = RenderingRegistry.getNextAvailableRenderId();

	public BlockCase(Material mat) {
		super(mat);
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public int getLightOpacity() {
		return 0;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
	}

	@Override
	public int getRenderType() {
		return renderID;
	}
}
