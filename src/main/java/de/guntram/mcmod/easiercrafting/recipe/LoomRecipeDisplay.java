package de.guntram.mcmod.easiercrafting.recipe;

import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;

import java.util.List;

public record LoomRecipeDisplay(
        List<SlotDisplay> dye,
        List<String> pattern,
        SlotDisplay result,
        SlotDisplay craftingStation
) implements RecipeDisplay {

    @Override
    public Serializer<? extends RecipeDisplay> serializer() {
        return null;
    }
}
