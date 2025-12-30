package de.guntram.mcmod.easiercrafting;

import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * @author taoge407
 * A Util to register recipe in class Registry, cause method RecipeType.register() could not change the namespace of recipe.
 */
public class RecipeUtil {
    public static RecipeType<Recipe<?>> register(String namespace, String id) {
        return Registry.register(Registries.RECIPE_TYPE, Identifier.of(namespace, id), new RecipeType<>() {
            public String toString() {
                return namespace + ":" + id;
            }
        });
    }
}
