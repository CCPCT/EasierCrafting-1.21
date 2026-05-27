package de.guntram.mcmod.easiercrafting.recipe;

import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;

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
//            LOGGER.warning("Using fall back comparison method for {}, {}",
//                    a.display().result().resolveForFirstStack(AbstractRecipeBook.EMPTY_CONTEXT).getItem().getDescriptionId(),
//                    b.display().result().resolveForFirstStack(AbstractRecipeBook.EMPTY_CONTEXT).getItem().getDescriptionId());
            return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
        });
    }
}