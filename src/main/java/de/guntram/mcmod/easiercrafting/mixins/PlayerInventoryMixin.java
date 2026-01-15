package de.guntram.mcmod.easiercrafting.mixins;

import de.guntram.mcmod.easiercrafting.InventoryAccessor;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin implements InventoryAccessor {

    // Shadow the internal list (the name 'main' is used in most mappings)
    @Final
    @Shadow
    private DefaultedList<ItemStack> main;

    @Override
    public List<ItemStack> getCompatMain() {
        return this.main;
    }
}
