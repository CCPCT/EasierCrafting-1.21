package de.guntram.mcmod.easiercrafting.mixins;

import de.guntram.mcmod.easiercrafting.EasierCrafting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class PlayerScreenHandlerMixin {
    @Inject(method = "onSlotClick", at = @At("RETURN"))
    private void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (!player.getWorld().isClient()) return;
        EasierCrafting.updateRecipe();
    }
}
