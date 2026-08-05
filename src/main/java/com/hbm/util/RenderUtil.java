package com.hbm.util;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import net.minecraft.client.renderer.Tessellator;

public class RenderUtil {

	public static float[] getLerpedArray(float[] matrixA, float[] matrixB, float progress) {
		float[] result = new float[16];
		for (int i = 0; i < 16; i++) {
			result[i] = matrixA[i] + (matrixB[i] - matrixA[i]) * progress;
		}
		return result;
	}

	public static FloatBuffer lerpMatrix(float[] matrixA, float[] matrixB, float progress) {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
		float[] blendedValue = getLerpedArray(matrixA, matrixB, progress);

		for (int i = 0; i < 16; i++) {
			buffer.put(blendedValue[i]);
		}

		buffer.flip();
		return buffer;
	}

	public static void renderBlock(Tessellator tessellator) {
		renderBlock(tessellator, 0, 1);
	}

	public static void renderBlock(Tessellator tessellator, double uvMin, double uvMax) {
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(-0.5, +0.5, -0.5, uvMax, uvMax);
		tessellator.addVertexWithUV(+0.5, +0.5, -0.5, uvMin, uvMax);
		tessellator.addVertexWithUV(+0.5, -0.5, -0.5, uvMin, uvMin);
		tessellator.addVertexWithUV(-0.5, -0.5, -0.5, uvMax, uvMin);

		tessellator.addVertexWithUV(-0.5, +0.5, +0.5, uvMax, uvMax);
		tessellator.addVertexWithUV(-0.5, +0.5, -0.5, uvMin, uvMax);
		tessellator.addVertexWithUV(-0.5, -0.5, -0.5, uvMin, uvMin);
		;
		tessellator.addVertexWithUV(-0.5, -0.5, +0.5, uvMax, uvMin);

		tessellator.addVertexWithUV(+0.5, +0.5, +0.5, uvMax, uvMax);
		tessellator.addVertexWithUV(-0.5, +0.5, +0.5, uvMin, uvMax);
		tessellator.addVertexWithUV(-0.5, -0.5, +0.5, uvMin, uvMin);
		tessellator.addVertexWithUV(+0.5, -0.5, +0.5, uvMax, uvMin);

		tessellator.addVertexWithUV(+0.5, +0.5, -0.5, uvMax, uvMax);
		tessellator.addVertexWithUV(+0.5, +0.5, +0.5, uvMin, uvMax);
		tessellator.addVertexWithUV(+0.5, -0.5, +0.5, uvMin, uvMin);
		tessellator.addVertexWithUV(+0.5, -0.5, -0.5, uvMax, uvMin);

		tessellator.addVertexWithUV(-0.5, -0.5, -0.5, uvMax, uvMax);
		tessellator.addVertexWithUV(+0.5, -0.5, -0.5, uvMin, uvMax);
		tessellator.addVertexWithUV(+0.5, -0.5, +0.5, uvMin, uvMin);
		tessellator.addVertexWithUV(-0.5, -0.5, +0.5, uvMax, uvMin);

		tessellator.addVertexWithUV(+0.5, +0.5, -0.5, uvMax, uvMax);
		tessellator.addVertexWithUV(-0.5, +0.5, -0.5, uvMin, uvMax);
		tessellator.addVertexWithUV(-0.5, +0.5, +0.5, uvMin, uvMin);
		tessellator.addVertexWithUV(+0.5, +0.5, +0.5, uvMax, uvMin);
		tessellator.draw();
	}

}
