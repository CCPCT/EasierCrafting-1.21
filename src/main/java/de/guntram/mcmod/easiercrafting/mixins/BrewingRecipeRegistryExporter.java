package de.guntram.mcmod.easiercrafting.mixins;

import de.guntram.mcmod.easiercrafting.BrewingRecipe;
import de.guntram.mcmod.easiercrafting.BrewingRecipeRegistryCache;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingRecipeRegistry.Builder.class)
public class BrewingRecipeRegistryExporter {

    @Inject(method = "registerItemRecipe", at = @At("RETURN"))
    private void addItemRecipe(Item input, Item ingredient, Item output, CallbackInfo ci) {
        BrewingRecipeRegistryCache.add(new BrewingRecipe<>(false, new ItemStack(input), new ItemStack(ingredient), new ItemStack(output)));
    }

    @Inject(method = "registerPotionRecipe", at=@At("RETURN"))
    private static void addPotionRecipe(RegistryEntry<Potion> input, Item ingredient, RegistryEntry<Potion> output, CallbackInfo ci) {
        BrewingRecipeRegistryCache.add(new BrewingRecipe<>(true, PotionContentsComponent.createStack(Items.POTION, input),
                                                new ItemStack(ingredient), PotionContentsComponent.createStack(Items.POTION, output)));
        // PotionUtil.setPotion(new ItemStack(Items.POTION), output)
    }
}
