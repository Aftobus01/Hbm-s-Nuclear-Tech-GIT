package com.hbm.items.weapon.grenade;

import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.entity.grenade.EntityGrenadeLemon;
import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.weapon.ItemGenericGrenade;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm.util.Vec3NT;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class ItemGrenadeLemon extends ItemGenericGrenade {

	public ItemGrenadeLemon(int fuse) {
		super(fuse);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if(!player.capabilities.isCreativeMode) {
			--stack.stackSize;
		}
		world.playSoundAtEntity(player, "hbm:weapon.throwLemon", 1.0F, 1.0F);
		if(!world.isRemote) {
			world.spawnEntityInWorld(new EntityGrenadeLemon(world, player));
		}
		return stack;
	}

	@Override
	public void explode(Entity grenade, EntityLivingBase thrower, World world, double x, double y, double z) {
		ExplosionVNT vnt = new ExplosionVNT(world, x, y, z, 4.5F, thrower);
		vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 15F));
		vnt.setPlayerProcessor(new PlayerProcessorStandard());
		vnt.setSFX(new ExplosionEffectWeapon(15, 3.75F, 1.5F));
		vnt.explode();

		EntityFireLingering fire = new EntityFireLingering(world).setArea(9, 3).setDuration(300).setType(EntityFireLingering.TYPE_DIESEL);
		fire.setPosition(x, y, z);
		world.spawnEntityInWorld(fire);

		for(int dx = -3; dx <= 3; dx++) for(int dy = -3; dy <= 3; dy++) for(int dz = -3; dz <= 3; dz++) {
			int bx = (int) Math.floor(x) + dx; int by = (int) Math.floor(y) + dy; int bz = (int) Math.floor(z) + dz;
			if(world.getBlock(bx, by, bz).isAir(world, bx, by, bz)) for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
				if(world.getBlock(bx + dir.offsetX, by + dir.offsetY, bz + dir.offsetZ).isFlammable(world, bx + dir.offsetX, by + dir.offsetY, bz + dir.offsetZ, dir.getOpposite())) {
					world.setBlock(bx, by, bz, Blocks.fire);
					break;
				}
			}
		}

		ItemStack frag = ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.INC, EnumGrenadeFuze.S3);
		Vec3NT vec = new Vec3NT(0.25, 0, 0);
		vec.rotateAroundYDeg(world.rand.nextDouble() * 360);

		for(int i = 0; i < 3; i++) {
			EntityGrenadeUniversal triplet = new EntityGrenadeUniversal(world, frag).setTrail(EntityGrenadeUniversal.TRAIL_TRIPLET);
			triplet.setPosition(x, y, z);
			triplet.setThrower(thrower);
			triplet.motionX = vec.xCoord;
			triplet.motionY = 0.75D;
			triplet.motionZ = vec.zCoord;
			world.spawnEntityInWorld(triplet);
			vec.rotateAroundYDeg(120);
		}
	}

	@Override
	public int getMaxTimer() {
		return 99999;
	}

	@Override
	public double getBounceMod() {
		return 0.75D;
	}
}
