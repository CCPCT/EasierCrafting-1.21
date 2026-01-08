package de.guntram.mcmod.easiercrafting.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;

import java.util.List;

public record RepairCraftingRecipeDisplay(
        List<SlotDisplay> ingredients,
        SlotDisplay result,
        SlotDisplay craftingStation
) implements RecipeDisplay {

    // 1.21 requires a MapCodec for registration if you want it to sync via vanilla packets
    public static final MapCodec<RepairCraftingRecipeDisplay> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    SlotDisplay.CODEC.listOf().fieldOf("ingredients").forGetter(RepairCraftingRecipeDisplay::ingredients),
                    SlotDisplay.CODEC.fieldOf("result").forGetter(RepairCraftingRecipeDisplay::result),
                    SlotDisplay.CODEC.fieldOf("crafting_station").forGetter(RepairCraftingRecipeDisplay::craftingStation)
            ).apply(instance, RepairCraftingRecipeDisplay::new)
    );

    @Override
    public Serializer<? extends RecipeDisplay> serializer() {
        return null;
    }
}
