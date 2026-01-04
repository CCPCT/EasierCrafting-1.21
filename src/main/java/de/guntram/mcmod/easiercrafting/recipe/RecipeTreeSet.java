package de.guntram.mcmod.easiercrafting.recipe;

import java.util.TreeSet;

import net.minecraft.recipe.RecipeDisplayEntry;

import static de.guntram.mcmod.easiercrafting.EasierCrafting.recipeDisplayName;

public class RecipeTreeSet<T> extends TreeSet<T> {

    public RecipeTreeSet() {
        super((a, b) -> {
            // We must treat them as Objects or a common base to compare
            String nameA = recipeDisplayName(a);
            String nameB = recipeDisplayName(b);

            int sameName = nameA.compareToIgnoreCase(nameB);

            if (sameName != 0) {
                return sameName;
            }

            // Fallback to hashCode to prevent TreeSet from merging
            // two different recipes with the same name.
            return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
        });
    }

    public void addRecipeEntry(T entry) {
        this.add(entry); // No cast needed, perfectly safe
    }
}