package de.guntram.mcmod.easiercrafting.recipe;

import java.util.TreeSet;
import java.util.function.Function;

public class RecipeTreeSet<T> extends TreeSet<T> {

    // need to provide a function to extract name of the recipe
    public RecipeTreeSet(Function<T, String> getRecipeName) {
        super((a, b) -> {
            // We must treat them as Objects or a common base to compare
            String nameA = getRecipeName.apply(a);
            String nameB = getRecipeName.apply(b);

            int sameName = nameA.compareToIgnoreCase(nameB);

            if (sameName != 0) {
                return sameName;
            }

            // Fallback to hashCode to prevent TreeSet from merging
            // two different recipes with the same name.
            return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
        });
    }
}