package de.guntram.mcmod.easiercrafting.mixins;

import de.guntram.mcmod.easiercrafting.EasierCrafting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class PlayerScreenHandlerMixin {
    @Inject(method = "clicked", at = @At("RETURN"))
    private void onSlotClick(int slotIndex, int buttonNum, ContainerInput containerInput, Player player, CallbackInfo ci) {
        if (!player.level().isClientSide()) return;
        EasierCrafting.updateRecipe();
    }
}
