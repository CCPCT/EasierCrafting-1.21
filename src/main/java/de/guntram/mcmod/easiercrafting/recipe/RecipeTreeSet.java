package de.guntram.mcmod.easiercrafting.recipe;

import de.guntram.mcmod.easiercrafting.EasierCrafting;
import de.guntram.mcmod.easiercrafting.recipebook.AbstractRecipeBook;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import net.minecraft.recipe.RecipeDisplayEntry;

import java.util.TreeSet;
import java.util.function.Function;

public class RecipeTreeSet extends ObjectAVLTreeSet<RecipeDisplayEntry> {

    public RecipeTreeSet() {
        /* compare method:
         * a<b -> -1
         * a=b -> 0
         * a>b -> 1
         */
        super((a, b) -> {
            // Reference equality check (fastest possible exit)
            if (a == b) return 0;

            int result = Integer.compare(a.id().index(),b.id().index());
            if (result!=0){
                return result;
            }

            // Fallback for different objects with same name
            // Note: System.identityHashCode is fine, but if T is a RecipeDisplay,
            // a unique ID from the registry is even faster if available.
            EasierCrafting.getGeneralLogger().warn("Using fall back comparison method for {}, {}", a.display().result().getFirst(AbstractRecipeBook.EMPTY_CONTEXT).getName().getString(), b.display().result().getFirst(AbstractRecipeBook.EMPTY_CONTEXT).getName().getString());
            return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
        });
    }
}