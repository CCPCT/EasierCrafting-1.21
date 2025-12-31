package de.guntram.mcmod.easiercrafting;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.display.SlotDisplay;

public class RecipeTreeSet extends TreeSet<RecipeDisplayEntry> {
    // set of crafting display recipes for same cat
    // compare to prevent same recipe?
    public RecipeTreeSet() {
        super(new Comparator<RecipeDisplayEntry>() {
            @Override
            public int compare(RecipeDisplayEntry a, RecipeDisplayEntry b) {
                // Use your existing helper for the result name
                int sameName = EasierCrafting.recipeDisplayName(a)
                        .compareToIgnoreCase(EasierCrafting.recipeDisplayName(b));

                if (sameName != 0) {
                    return sameName;
                }

                // If names are the same, Stonecutter recipes are sub-sorted by their input ingredient name
//                if (a.getType() == RecipeType.STONECUTTING && b.getType() == RecipeType.STONECUTTING) {
//                    return compareFirstIngredient(a, b);
//                }

                // Fallback to avoid merging different recipes with the same display name
                // In 1.20.4, use the hashcode or a unique property since getId() is moved to RecipeHolder
                return Integer.compare(a.hashCode(), b.hashCode());
            }

            private int compareFirstIngredient(RecipeDisplayEntry a, RecipeDisplayEntry b) {
                if (a.craftingRequirements().isEmpty() || b.craftingRequirements().isEmpty()) return 0;
                String nameA = a.craftingRequirements().get().getFirst().toString();
                String nameB = b.craftingRequirements().get().getFirst().toString();
                return nameA.compareToIgnoreCase(nameB);
            }

        });
    }
}