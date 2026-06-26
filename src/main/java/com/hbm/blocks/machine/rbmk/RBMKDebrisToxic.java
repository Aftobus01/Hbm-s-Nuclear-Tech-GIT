package com.hbm.blocks.machine.rbmk;

import java.util.Random;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionHandler.PollutionType;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toclient.AuxParticlePacketNT;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class RBMKDebrisToxic extends RBMKDebris {

	@Override
	public int tickRate(World world) {
		return 20;
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random rand) {

		if(!world.isRemote) {

			PollutionHandler.incrementPollution(world, x, y, z, PollutionType.POISON, 0.5F);

			for(int i = 0; i < 2; i++) {
				NBTTagCompound data = new NBTTagCompound();
				data.setString("type", "tower");
				data.setFloat("lift", 8F);
				data.setFloat("base", 1.5F);
				data.setFloat("max", 5F);
				data.setInteger("life", 400 + rand.nextInt(200));
				data.setInteger("color", 0x0A0A0A);
				data.setDouble("posX", x + 0.5 + rand.nextGaussian() * 0.5);
				data.setDouble("posY", y + 2);
				data.setDouble("posZ", z + 0.5 + rand.nextGaussian() * 0.5);
				PacketThreading.createAllAroundThreadedPacket(new AuxParticlePacketNT(data, x + 0.5, y + 2, z + 0.5), new TargetPoint(world.provider.dimensionId, x + 0.5, y + 2, z + 0.5, 75));
				MainRegistry.proxy.effectNT(data);
			}

			if(rand.nextInt(240) == 0) {
				int meta = world.getBlockMetadata(x, y, z);
				if(meta < 15) {
					world.setBlockMetadataWithNotify(x, y, z, meta + 1, 2);
				} else {
					world.setBlock(x, y, z, Blocks.air);
					return;
				}
			}

			world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
		}
	}

	public void onBlockAdded(World world, int x, int y, int z) {
		super.onBlockAdded(world, x, y, z);
		world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
	}
}
