/*
 * Minecraft Forge
 * Copyright (c) 2016-2022.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.client.model.pipeline;

import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.common.ForgeConfig;

public class ForgeBlockModelRenderer extends BlockModelRenderer
{
    private final ThreadLocal<VertexLighterFlat> lighterFlat;
    private final ThreadLocal<VertexLighterSmoothAo> lighterSmooth;
    private final ThreadLocal<VertexBufferConsumer> consumerFlat = ThreadLocal.withInitial(VertexBufferConsumer::new);
    private final ThreadLocal<VertexBufferConsumer> consumerSmooth = ThreadLocal.withInitial(VertexBufferConsumer::new);

    public ForgeBlockModelRenderer(BlockColors colors)
    {
        super(colors);
        lighterFlat = ThreadLocal.withInitial(() -> new VertexLighterFlat(colors));
        lighterSmooth = ThreadLocal.withInitial(() -> new VertexLighterSmoothAo(colors));
    }

    @Override
    public boolean renderModelFlat(IBlockDisplayReader world, IBakedModel model, BlockState state, BlockPos pos, MatrixStack matrixStack, IVertexBuilder buffer, boolean checkSides, Random rand, long seed, int combinedOverlayIn, IModelData modelData)
    {
        if(ForgeConfig.CLIENT.experimentalForgeLightPipelineEnabled.get())
        {
            VertexBufferConsumer consumer = consumerFlat.get();
            consumer.setBuffer(buffer);

            VertexLighterFlat lighter = lighterFlat.get();
            lighter.setParent(consumer);
            lighter.setTransform(matrixStack.last());

            return render(lighter, world, model, state, pos, matrixStack, checkSides, rand, seed, modelData);
        }
        else
        {
            return super.renderModelFlat(world, model, state, pos, matrixStack, buffer, checkSides, rand, seed, combinedOverlayIn, modelData);
        }
    }

    @Override
    public boolean renderModelSmooth(IBlockDisplayReader world, IBakedModel model, BlockState state, BlockPos pos, MatrixStack matrixStack, IVertexBuilder buffer, boolean checkSides, Random rand, long seed, int combinedOverlayIn, IModelData modelData)
    {
        if(ForgeConfig.CLIENT.experimentalForgeLightPipelineEnabled.get())
        {
            VertexBufferConsumer consumer = consumerSmooth.get();
            consumer.setBuffer(buffer);

            VertexLighterSmoothAo lighter = lighterSmooth.get();
            lighter.setParent(consumer);
            lighter.setTransform(matrixStack.last());

            return render(lighter, world, model, state, pos, matrixStack, checkSides, rand, seed, modelData);
        }
        else
        {
            return super.renderModelSmooth(world, model, state, pos, matrixStack, buffer, checkSides, rand, seed, combinedOverlayIn, modelData);
        }
    }

    public static boolean render(VertexLighterFlat lighter, IBlockDisplayReader world, IBakedModel model, BlockState state, BlockPos pos, MatrixStack matrixStack, boolean checkSides, Random rand, long seed, IModelData modelData)
    {
        lighter.setWorld(world);
        lighter.setState(state);
        lighter.setBlockPos(pos);
        boolean empty = true;
        rand.setSeed(seed);
        List<BakedQuad> quads = model.getQuads(state, null, rand, modelData);
        if(!quads.isEmpty())
        {
            lighter.updateBlockInfo();
            empty = false;
            for(BakedQuad quad : quads)
            {
                quad.pipe(lighter);
            }
        }
        for(Direction side : Direction.values())
        {
            rand.setSeed(seed);
            quads = model.getQuads(state, side, rand, modelData);
            if(!quads.isEmpty())
            {
                if(!checkSides || Block.shouldRenderFace(state, world, pos, side))
                {
                    if(empty) lighter.updateBlockInfo();
                    empty = false;
                    for(BakedQuad quad : quads)
                    {
                        quad.pipe(lighter);
                    }
                }
            }
        }
        lighter.resetBlockInfo();
        return !empty;
    }
}
