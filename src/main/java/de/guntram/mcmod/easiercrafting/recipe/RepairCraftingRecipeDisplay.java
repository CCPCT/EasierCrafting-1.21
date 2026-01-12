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

    @Override
    public Serializer<? extends RecipeDisplay> serializer() {
        return null;
    }
}
