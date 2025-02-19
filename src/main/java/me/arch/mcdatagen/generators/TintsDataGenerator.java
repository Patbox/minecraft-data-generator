package me.arch.mcdatagen.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.arch.mcdatagen.util.EmptyRenderBlockView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.world.FoliageColors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;

import java.util.*;

public class TintsDataGenerator implements IDataGenerator {

    public static class BiomeTintColors {
        Map<Integer, List<Biome>> grassColoursMap = new HashMap<>();
        Map<Integer, List<Biome>> foliageColoursMap = new HashMap<>();
        Map<Integer, List<Biome>> waterColourMap = new HashMap<>();
    }

    public static BiomeTintColors generateBiomeTintColors(Registry<Biome> biomeRegistry) {
        BiomeTintColors colors = new BiomeTintColors();

        biomeRegistry.forEach(biome -> {
            int biomeGrassColor = biome.getGrassColorAt(0.0, 0.0);
            int biomeFoliageColor = biome.getFoliageColor();
            int biomeWaterColor = biome.getWaterColor();

            colors.grassColoursMap.computeIfAbsent(biomeGrassColor, k -> new ArrayList<>()).add(biome);
            colors.foliageColoursMap.computeIfAbsent(biomeFoliageColor, k -> new ArrayList<>()).add(biome);
            colors.waterColourMap.computeIfAbsent(biomeWaterColor, k -> new ArrayList<>()).add(biome);
        });
        return colors;
    }

    public static Map<Integer, Integer> generateRedstoneTintColors() {
        Map<Integer, Integer> resultColors = new HashMap<>();

        for (int redstoneLevel : RedstoneWireBlock.POWER.getValues()) {
            int color = RedstoneWireBlock.getWireColor(redstoneLevel);
            resultColors.put(redstoneLevel, color);
        }
        return resultColors;
    }

    @Environment(EnvType.CLIENT)
    private static int getBlockColor(Block block, BlockColors blockColors) {
        return blockColors.getColor(block.getDefaultState(), EmptyRenderBlockView.INSTANCE, BlockPos.ORIGIN, 0xFFFFFF);
    }

    @Environment(EnvType.CLIENT)
    public static Map<Block, Integer> generateConstantTintColors() {
        Map<Block, Integer> resultColors = new HashMap<>();
        BlockColors blockColors = MinecraftClient.getInstance().getBlockColors();

        resultColors.put(Blocks.BIRCH_LEAVES, FoliageColors.getBirchColor());
        resultColors.put(Blocks.SPRUCE_LEAVES, FoliageColors.getSpruceColor());

        resultColors.put(Blocks.LILY_PAD, getBlockColor(Blocks.LILY_PAD, blockColors));
        resultColors.put(Blocks.ATTACHED_MELON_STEM, getBlockColor(Blocks.ATTACHED_MELON_STEM, blockColors));
        resultColors.put(Blocks.ATTACHED_PUMPKIN_STEM, getBlockColor(Blocks.ATTACHED_PUMPKIN_STEM, blockColors));

        //not really constant, depend on the block age, but kinda have to be handled since textures are literally white without them
        resultColors.put(Blocks.MELON_STEM, getBlockColor(Blocks.MELON_STEM, blockColors));
        resultColors.put(Blocks.PUMPKIN_STEM, getBlockColor(Blocks.PUMPKIN_STEM, blockColors));

        return resultColors;
    }

    private static JsonObject encodeBiomeColorMap(Registry<Biome> biomeRegistry, Map<Integer, List<Biome>> colorsMap) {
        JsonArray resultColorsArray = new JsonArray();
        for (var entry : colorsMap.entrySet()) {
            JsonObject entryObject = new JsonObject();

            JsonArray keysArray = new JsonArray();
            for (Biome biome : entry.getValue()) {
                Identifier registryKey = biomeRegistry.getKey(biome).orElseThrow().getValue();
                keysArray.add(registryKey.getPath());
            }

            entryObject.add("keys", keysArray);
            entryObject.addProperty("color", entry.getKey());
            resultColorsArray.add(entryObject);
        }

        JsonObject resultObject = new JsonObject();
        resultObject.add("data", resultColorsArray);
        return resultObject;
    }

    private static JsonObject encodeRedstoneColorMap(Map<Integer, Integer> colorsMap) {
        JsonArray resultColorsArray = new JsonArray();
        for (var entry : colorsMap.entrySet()) {
            JsonObject entryObject = new JsonObject();

            JsonArray keysArray = new JsonArray();
            keysArray.add(entry.getKey());

            entryObject.add("keys", keysArray);
            entryObject.addProperty("color", entry.getValue());
            resultColorsArray.add(entryObject);
        }

        JsonObject resultObject = new JsonObject();
        resultObject.add("data", resultColorsArray);
        return resultObject;
    }

    private static JsonObject encodeBlocksColorMap(Registry<Block> blockRegistry, Map<Block, Integer> colorsMap) {
        JsonArray resultColorsArray = new JsonArray();
        for (var entry : colorsMap.entrySet()) {
            JsonObject entryObject = new JsonObject();

            JsonArray keysArray = new JsonArray();
            Identifier registryKey = blockRegistry.getKey(entry.getKey()).orElseThrow().getValue();
            keysArray.add(registryKey.getPath());

            entryObject.add("keys", keysArray);
            entryObject.addProperty("color", entry.getValue());
            resultColorsArray.add(entryObject);
        }

        JsonObject resultObject = new JsonObject();
        resultObject.add("data", resultColorsArray);
        return resultObject;
    }

    @Override
    public String getDataName() {
        return "tints";
    }

    @Override
    public JsonObject generateDataJson() {
        DynamicRegistryManager registryManager = DynamicRegistryManager.BUILTIN.get();
        Registry<Biome> biomeRegistry = registryManager.get(Registry.BIOME_KEY);
        Registry<Block> blockRegistry = registryManager.get(Registry.BLOCK_KEY);

        BiomeTintColors biomeTintColors = generateBiomeTintColors(biomeRegistry);
        Map<Integer, Integer> redstoneColors = generateRedstoneTintColors();
        Map<Block, Integer> constantTintColors = Collections.emptyMap();

        EnvType currentEnvironment = FabricLoader.getInstance().getEnvironmentType();
        if (currentEnvironment == EnvType.CLIENT) {
            constantTintColors = generateConstantTintColors();
        }

        JsonObject resultObject = new JsonObject();

        resultObject.add("grass", encodeBiomeColorMap(biomeRegistry, biomeTintColors.grassColoursMap));
        resultObject.add("foliage", encodeBiomeColorMap(biomeRegistry, biomeTintColors.foliageColoursMap));
        resultObject.add("water", encodeBiomeColorMap(biomeRegistry, biomeTintColors.waterColourMap));

        resultObject.add("redstone", encodeRedstoneColorMap(redstoneColors));
        resultObject.add("constant", encodeBlocksColorMap(blockRegistry, constantTintColors));

        return resultObject;
    }
}
