package de.guntram.mcmod.easiercrafting;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.List;
import java.util.Optional;

/**
 * @author taoge407
 * FUCKING mojang, removing ALL PotionUtil to reconstructing their fucking potion system, leading to restoring a new util class to implement some offenly used method
 */
public class PotionUtil {
    public static Potion getPotion(ItemStack itemStack) {
        return itemStack.get(DataComponentTypes.POTION_CONTENTS).potion().get().value();
    }

    public static List<StatusEffectInstance> getPotionEffects(ItemStack itemStack) {
        Potion p = getPotion(itemStack);
        return p.getEffects();
    }

    public static void setPotion(ItemStack itemStack, Potion potion) {
        itemStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(RegistryEntry.of(potion)));
    }

    public static void setCustomPotionEffects(ItemStack itemStack, List<StatusEffectInstance> effects) {
        PotionContentsComponent oldPCC = itemStack.get(DataComponentTypes.POTION_CONTENTS);
        PotionContentsComponent component = new PotionContentsComponent(Optional.of(RegistryEntry.of(getPotion(itemStack))), Optional.of(oldPCC.getColor()), effects);
        itemStack.set(DataComponentTypes.POTION_CONTENTS, component);
    }
}
