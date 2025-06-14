package com.hbm.blocks;

import java.util.ArrayList;
import java.util.Random;

import com.hbm.items.ModItems;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockCrop extends BlockBush implements IGrowable {

	protected int maxGrowthStage = 7;

	@SideOnly(Side.CLIENT)
	protected IIcon[] blockIcons;

	public BlockCrop() {
		setTickRandomly(true);
		float f = 0.5F;
		setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, 0.25F, 0.5F + f);
		setHardness(0.0F);
		setStepSound(soundTypeGrass);
		disableStats();
	}

	@Override
	protected boolean canPlaceBlockOn(Block block) {
		return block == Blocks.farmland && this != ModBlocks.crop_strawberry;
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		if(this == ModBlocks.crop_strawberry) {
			return false;
		}
		return super.canPlaceBlockAt(world, x, y, z);
	}

	public void incrementGrowStage(World world, Random rand, int x, int y, int z) {
		int growStage = world.getBlockMetadata(x, y, z) + MathHelper.getRandomIntegerInRange(rand, 2, 5);
		if(growStage > maxGrowthStage) {
			growStage = maxGrowthStage;
		}
		world.setBlockMetadataWithNotify(x, y, z, growStage, 2);
	}

	@Override
	public Item getItemDropped(int meta, Random rand, int fortune) {
		if(this == ModBlocks.crop_strawberry) {
			return null;
		}
		if(this == ModBlocks.crop_coffee) {
			return ModItems.bean_raw;
		}
		if(this == ModBlocks.crop_tea) {
			return meta == 7 ? ModItems.tea_leaf : ModItems.teaseeds;
		}
		return Item.getItemFromBlock(this);
	}

	@Override
	public int getRenderType() {
		return 1;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int growthStage) {
		return blockIcons[growthStage];
	}

	protected void checkAndDropBlock(World world, int x, int y, int z) {
		if(!this.canBlockStay(world, x, y, z)) {
			if(this != ModBlocks.crop_strawberry) {
				this.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
			}
			world.setBlock(x, y, z, Blocks.air, 0, 2);
		}
	}

	@Override
	public boolean canBlockStay(World world, int x, int y, int z) {
		if(this == ModBlocks.crop_strawberry) {
			return false;
		}
		return world.getBlock(x, y - 1, z).canSustainPlant(world, x, y - 1, z, ForgeDirection.UP, this);
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random rand) {
		if(this == ModBlocks.crop_strawberry) {
			System.out.println("Removing strawberry crop at " + x + "," + y + "," + z + " in biome: " + world.getBiomeGenForCoords(x, z).biomeName);
			world.setBlockToAir(x, y, z);
			return;
		}
		super.updateTick(world, x, y, z, rand);
		int growStage = world.getBlockMetadata(x, y, z) + 1;
		if(growStage > 7) {
			growStage = 7;
		}
		world.setBlockMetadataWithNotify(x, y, z, growStage, 2);
	}

	@Override
	public boolean func_149851_a(World world, int x, int y, int z, boolean p_149851_5_) {
		return this != ModBlocks.crop_strawberry && world.getBlockMetadata(x, y, z) != 7;
	}

	@Override
	public boolean func_149852_a(World world, Random rand, int x, int y, int z) {
		return this != ModBlocks.crop_strawberry;
	}

	@Override
	public void func_149853_b(World world, Random rand, int x, int y, int z) {
		if(this != ModBlocks.crop_strawberry) {
			incrementGrowStage(world, rand, x, y, z);
		}
	}

	@Override
	public int quantityDropped(int meta, int fortune, Random rand) {
		if(this == ModBlocks.crop_strawberry) {
			return 0;
		}
		if(meta == 7) {
			return 4;
		} else {
			return meta / 2;
		}
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
		ArrayList<ItemStack> ret = new ArrayList<>();
		if(this == ModBlocks.crop_strawberry) {
			return ret;
		}
		ret.addAll(super.getDrops(world, x, y, z, metadata, fortune));
		if(this == ModBlocks.crop_tea && metadata >= 7) {
			for(int i = 0; i < 3 + fortune; ++i) {
				if(world.rand.nextInt(15) <= metadata) {
					ret.add(new ItemStack(ModItems.teaseeds, 1, 0));
				}
			}
		}
		return ret;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister parIIconRegister) {
		blockIcons = new IIcon[maxGrowthStage + 2];
		blockIcons[0] = parIIconRegister.registerIcon(getTextureName() + "_1");
		blockIcons[1] = parIIconRegister.registerIcon(getTextureName() + "_1");
		blockIcons[2] = parIIconRegister.registerIcon(getTextureName() + "_2");
		blockIcons[3] = parIIconRegister.registerIcon(getTextureName() + "_2");
		blockIcons[4] = parIIconRegister.registerIcon(getTextureName() + "_3");
		blockIcons[5] = parIIconRegister.registerIcon(getTextureName() + "_3");
		blockIcons[6] = parIIconRegister.registerIcon(getTextureName() + "_4");
		blockIcons[7] = parIIconRegister.registerIcon(getTextureName() + "_5");
		blockIcons[8] = parIIconRegister.registerIcon(getTextureName() + "_5");
	}
}
