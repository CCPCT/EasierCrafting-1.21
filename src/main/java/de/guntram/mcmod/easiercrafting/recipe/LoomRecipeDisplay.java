package de.guntram.mcmod.easiercrafting.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jspecify.annotations.NonNull;

import java.util.List;

public record LoomRecipeDisplay(
        List<SlotDisplay> dye,
        List<String> pattern,
        SlotDisplay result,
        SlotDisplay craftingStation
) implements RecipeDisplay {

    // 1. Define the MapCodec for serialization (JSON/Data packs)
    public static final MapCodec<RepairCraftingRecipeDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(
                    SlotDisplay.CODEC.listOf().fieldOf("ingredients").forGetter(RepairCraftingRecipeDisplay::ingredients),
                    SlotDisplay.CODEC.fieldOf("result").forGetter(RepairCraftingRecipeDisplay::result),
                    SlotDisplay.CODEC.fieldOf("crafting_station").forGetter(RepairCraftingRecipeDisplay::craftingStation)
            ).apply(instance, RepairCraftingRecipeDisplay::new)
    );

    // 2. Define the StreamCodec for network packets (Server-to-Client synchronization)
    public static final StreamCodec<RegistryFriendlyByteBuf, RepairCraftingRecipeDisplay> STREAM_CODEC = StreamCodec.composite(
            SlotDisplay.STREAM_CODEC.apply(ByteBufCodecs.list()), RepairCraftingRecipeDisplay::ingredients,
            SlotDisplay.STREAM_CODEC, RepairCraftingRecipeDisplay::result,
            SlotDisplay.STREAM_CODEC, RepairCraftingRecipeDisplay::craftingStation,
            RepairCraftingRecipeDisplay::new
    );

    // 3. Instantiate the specific RecipeDisplay Type registry token
    public static final RecipeDisplay.Type<RepairCraftingRecipeDisplay> TYPE = new RecipeDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

    // 4. Satisfy the interface requirements via overriding
    @Override
    public RecipeDisplay.@NonNull Type<RepairCraftingRecipeDisplay> type() {
        return TYPE;
    }

    @Override
    public boolean isEnabled(@NonNull FeatureFlagSet enabledFeatures) {
        return this.result().isEnabled(enabledFeatures);
    }
}
