package me.arch.mcdatagen.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;
import org.jetbrains.annotations.Nullable;

public enum EmptyRenderBlockView implements BlockRenderView {
    INSTANCE;

    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    public BlockState getBlockState(BlockPos pos) {
        return Blocks.AIR.getDefaultState();
    }

    public FluidState getFluidState(BlockPos pos) {
        return Fluids.EMPTY.getDefaultState();
    }

    public int getBottomY() {
        return 0;
    }

    public int getHeight() {
        return 0;
    }


    @Override
    public float getBrightness(Direction direction, boolean shaded) {
        return 0.0f;
    }

    @Override
    public LightingProvider getLightingProvider() {
        return null;
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver colorResolver) {
        DynamicRegistryManager registryManager = DynamicRegistryManager.BUILTIN.get();
        Registry<Biome> biomeRegistry = registryManager.get(Registry.BIOME_KEY);
        Biome plainsBiome = biomeRegistry.get(BiomeKeys.PLAINS);

        return colorResolver.getColor(plainsBiome, pos.getX(), pos.getY());
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos) {
        return type == LightType.SKY ? getMaxLightLevel() : 0;
    }

    @Override
    public int getBaseLightLevel(BlockPos pos, int ambientDarkness) {
        return ambientDarkness;
    }
}
