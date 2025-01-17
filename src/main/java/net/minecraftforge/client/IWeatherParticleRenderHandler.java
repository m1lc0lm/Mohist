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

package net.minecraftforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;

/**
 * Call {@link net.minecraft.client.world.DimensionRenderInfo#setWeatherParticleRenderHandler(net.minecraftforge.client.IWeatherParticleRenderHandler)}, obtained from a {@link ClientWorld} with an implementation of this to override all weather particle rendering with your own.
 * This handles ground particles that can be seen when it's raining (splash/smoke particles).
 * This also includes playing rain sounds.
 */
@FunctionalInterface
public interface IWeatherParticleRenderHandler {
    void render(int ticks, ClientWorld world, Minecraft mc, ActiveRenderInfo activeRenderInfoIn);
}
