package de.guntram.mcmod.easiercrafting.mixins;

import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantScreen.class)
public abstract class MerchantMixin extends Screen {
    @Shadow
    private int selectedIndex;

    protected MerchantMixin(Text title) {
        super(title);
    }

    @Inject(
            method = "mouseClicked",
            at = @At(value = "TAIL")
    )
    private void onTradeSelected(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        Window window = MinecraftClient.getInstance().getWindow();
        if (!ModConfig.get().enableTrading || InputUtil.isKeyPressed(window,InputUtil.GLFW_KEY_LEFT_CONTROL) || client.player.currentScreenHandler.getSlot(2).getStack().isEmpty()) return;

        // ai = leftPos, aj = topPos
        int ai = (this.width - 276) / 2;
        int aj = (this.height - 166) / 2;

        int listStartX = ai+5;
        int listEndX = ai+5 + 88;
        int listStartY = aj + 16;
        int listEndY = aj + 16 + 140;

        boolean isOverTradeTab = click.x() >= listStartX && click.x() <= listEndX &&
                click.y() >= listStartY && click.y() <= listEndY;


        System.out.println("requirement: " + (ai+5) + "/" + (aj+16) + " to " + (ai+5+88) + "/" + (aj+16+120));
        System.out.println(click.x() + "/" + click.y() + " over trade tab: " + isOverTradeTab);
        System.out.println(this.selectedIndex);
        if (!isOverTradeTab) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)){
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,2,0, SlotActionType.QUICK_MOVE,client.player);
        } else {
            if (InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_Q)){
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,2,0, SlotActionType.THROW,client.player);
                // move items back from villager
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,0,0, SlotActionType.QUICK_MOVE,client.player);
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,1,0, SlotActionType.QUICK_MOVE,client.player);

                return;
            }

            ItemStack fromStack = client.player.currentScreenHandler.getSlot(2).getStack().copy();
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,2,0, SlotActionType.PICKUP,client.player);
            for (int i=3; i<39; i++){
                ItemStack targetStack = client.player.currentScreenHandler.getSlot(i).getStack();
                if (targetStack.isEmpty()){
                    client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,i,0, SlotActionType.PICKUP,client.player);
                    break;
                }
                if (fromStack.getItem() != targetStack.getItem()) continue;
                if (targetStack.getCount() == targetStack.getMaxCount()) continue;
                fromStack.setCount(Math.max(0,targetStack.getCount()+fromStack.getCount()-targetStack.getMaxCount()));
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,i,0, SlotActionType.PICKUP,client.player);
                if (fromStack.getCount()==0) break;
            }
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,0,0, SlotActionType.QUICK_MOVE,client.player);
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,1,0, SlotActionType.QUICK_MOVE,client.player);

        }
    }
}