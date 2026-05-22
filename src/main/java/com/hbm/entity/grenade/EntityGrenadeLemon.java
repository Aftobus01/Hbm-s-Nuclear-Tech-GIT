package com.hbm.entity.grenade;

import org.apache.logging.log4j.Level;

import com.hbm.config.GeneralConfig;
import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.hbm.main.MainRegistry;
import com.hbm.util.Vec3NT;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class EntityGrenadeLemon extends EntityGrenadeBouncyBase {

	public static final int DW_BOUNCES = 4;

	private int impactTimer = 0;
	public double prevSpin;
	public double spin;

	public EntityGrenadeLemon(World world) {
		super(world);
	}

	public EntityGrenadeLemon(World world, EntityLivingBase living) {
		super(world, living);
	}

	public EntityGrenadeLemon(World world, double x, double y, double z) {
		super(world, x, y, z);
	}

	@Override
	public void onUpdate() {
		double prevMX = this.motionX;
		double prevMY = this.motionY;
		double prevMZ = this.motionZ;

		super.onUpdate();

		if(this.isDead) return;

		if(!worldObj.isRemote) {
			if(this.impactTimer < 10) {
				this.impactTimer++;
			}

			boolean hitX = (prevMX > 0.01 && this.motionX < -0.01) || (prevMX < -0.01 && this.motionX > 0.01);
			boolean hitZ = (prevMZ > 0.01 && this.motionZ < -0.01) || (prevMZ < 0.01 && this.motionZ > 0.01);
			boolean hitY = (prevMY > 0.05 && this.motionY < 0) || (prevMY < -0.05 && this.motionY > 0);

			if(hitX || hitY || hitZ) {
				if(this.impactTimer >= 10) {
					this.explode();
				}
				this.dataWatcher.updateObject(DW_BOUNCES, this.getBounces() + 1);
			}
		} else {
			this.prevSpin = this.spin;

			if(this.getBounces() <= 0) {
				this.spin += 15;
			} else {
				this.spin += Math.min(15, new Vec3NT(lastTickPosX - posX, 0, lastTickPosZ - posZ).lengthVector() * 50);
			}

			if(this.spin >= 360) {
				this.prevSpin -= 360;
				this.spin -= 360;
			}
		}
	}

	@Override
	protected float func_70182_d() {
		return 1.0F;
	}

	public int getBounces() {
		return this.dataWatcher.getWatchableObjectInt(DW_BOUNCES);
	}

	@Override
	public void explode() {
		this.setDead();

		World world = this.worldObj;
		double x = this.posX;
		double y = this.posY;
		double z = this.posZ;
		EntityLivingBase thrower = this.getThrower();

		ExplosionVNT vnt = new ExplosionVNT(world, x, y, z, 3F, thrower);
		vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 10F));
		vnt.setPlayerProcessor(new PlayerProcessorStandard());
		vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
		vnt.explode();

		EntityFireLingering fire = new EntityFireLingering(world).setArea(6, 2).setDuration(200).setType(EntityFireLingering.TYPE_DIESEL);
		fire.setPosition(x, y, z);
		world.spawnEntityInWorld(fire);

		for(int dx = -2; dx <= 2; dx++) for(int dy = -2; dy <= 2; dy++) for(int dz = -2; dz <= 2; dz++) {
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

		if(GeneralConfig.enableExtendedLogging) {
			String s = "null";
			if(thrower != null && thrower instanceof EntityPlayer) s = ((EntityPlayer) thrower).getDisplayName();
			MainRegistry.logger.log(Level.INFO, "[GREN] Set off grenade at " + ((int) posX) + " / " + ((int) posY) + " / " + ((int) posZ) + " by " + s + "!");
		}
	}

	@Override
	protected int getMaxTimer() {
		return 99999;
	}

	@Override
	protected double getBounceMod() {
		return 0.5D;
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataWatcher.addObject(DW_BOUNCES, new Integer(0));
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);
		nbt.setInteger("bounces", this.getBounces());
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
		this.dataWatcher.updateObject(DW_BOUNCES, nbt.getInteger("bounces"));
	}
}
