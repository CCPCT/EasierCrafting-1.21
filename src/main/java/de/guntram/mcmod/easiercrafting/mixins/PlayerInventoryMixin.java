package de.guntram.mcmod.easiercrafting.mixins;

import de.guntram.mcmod.easiercrafting.EasierCrafting;
import de.guntram.mcmod.easiercrafting.InventoryAccessor;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin implements InventoryAccessor {

    // Shadow the internal list (the name 'main' is used in most mappings)
    @Final
    @Shadow
    private DefaultedList<ItemStack> main;

    @Override
    public List<ItemStack> easierCrafting_Reloaded$getCompatMain() {
        return this.main;
    }

    @Unique
    private boolean update = false;

    @Inject(method = "insertStack(ILnet/minecraft/item/ItemStack;)Z", at = @At("RETURN"))
    private void onInsertStack(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            update = true; // Update in 1 tick
        }
    }

    @Inject(method = "updateItems", at = @At("RETURN"))
    private void onUpdate(CallbackInfo ci) {
        if (update) {
            update = false;
            System.out.println("picked up sth");
            EasierCrafting.updateRecipe();
        }
    }

}
