package de.guntram.mcmod.easiercrafting.recipe;

import java.util.List;
import java.util.Optional;

public record LoomRecipe(
        String name,
        String serverIp,
        String baseBanner,
        List<BannerStep> steps
) {
    public record BannerStep(
            String dye,     // Stored as color name (e.g., "red")
            String pattern  // Stored as ID (e.g., "minecraft:bricks")
    ) {}

    /**
     * Converts a live ItemStack into a LoomRecipe object.
     */
    public static Optional<LoomRecipe> fromItemStack(net.minecraft.item.ItemStack stack, String ip) {
        var component = stack.get(net.minecraft.component.DataComponentTypes.BANNER_PATTERNS);
        if (component == null) return Optional.empty();

        List<BannerStep> steps = new java.util.ArrayList<>();
        for (var layer : component.layers()) {
            // Get the raw name ("red", "blue") instead of the localized name
            steps.add(new BannerStep(layer.color().name(), layer.pattern().getIdAsString()));
        }

        return Optional.of(new LoomRecipe(
                null,
                ip,
                net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString(),
                steps
        ));
    }
}