package de.guntram.mcmod.easiercrafting.modConfig;

import de.guntram.mcmod.easiercrafting.recipebook.AbstractRecipeBook;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;


public class ConfigScreen extends Screen {

    protected ConfigScreen() {
        super(Component.translatable("easiercrafting.config.title"));
    }

    public static Screen getConfigScreen(Screen parent) {
        ModConfig.load();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("easiercrafting.config.title"))
                .setSavingRunnable(ModConfig::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory generalTab = builder.getOrCreateCategory(Component.translatable("easiercrafting.config.general"));
        ConfigCategory visualTab = builder.getOrCreateCategory(Component.literal("visual"));
        ConfigCategory recipeTab = builder.getOrCreateCategory(Component.literal("recipe"));


        // General settings
        generalTab.addEntry(entryBuilder.startBooleanToggle(Component.literal("Enable Mod"), ModConfig.get().modEnabled)
                .setSaveConsumer(newValue -> ModConfig.get().modEnabled = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startBooleanToggle(Component.translatable("easiercrafting.config.autofocus"), ModConfig.get().autoFocusSearch)
                .setTooltip(Component.translatable("easiercrafting.config.tt.autofocus"))
                .setSaveConsumer(newValue -> ModConfig.get().autoFocusSearch = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startIntField(Component.translatable("easiercrafting.config.autoupdate"), ModConfig.get().autoUpdateRecipeTimer)
                .setTooltip(Component.translatable("easiercrafting.config.tt.autoupdate"))
                .setMin(0).setMax(30)
                .setSaveConsumer(newValue -> ModConfig.get().autoUpdateRecipeTimer = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startBooleanToggle(Component.translatable("easiercrafting.config.allowinternalbutton"), ModConfig.get().allowRecipeBook)
                .setTooltip(Component.translatable("easiercrafting.config.tt.allowinternalbutton"))
                .setSaveConsumer(newValue -> ModConfig.get().allowRecipeBook = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startBooleanToggle(Component.translatable("easiercrafting.config.guiright"), ModConfig.get().showGuiRight)
                .setTooltip(Component.translatable("easiercrafting.config.tt.guiright"))
                .setSaveConsumer(newValue -> ModConfig.get().showGuiRight = newValue)
                .build());

        recipeTab.addEntry(entryBuilder.startBooleanToggle(Component.translatable("easiercrafting.config.specialrecipes"), ModConfig.get().allowGeneratedRecipes)
                .setTooltip(Component.translatable("easiercrafting.config.tt.specialrecipes"))
                .setSaveConsumer(newValue -> ModConfig.get().allowGeneratedRecipes = newValue)
                .build());

        recipeTab.addEntry(entryBuilder.startIntField(Component.translatable("easiercrafting.config.maxenchants"), ModConfig.get().maxEnchantsAllowedForRepair)
                .setTooltip(Component.translatable("easiercrafting.config.tt.maxenchants"))
                .setMin(0).setMax(10)
                .setSaveConsumer(newValue -> ModConfig.get().maxEnchantsAllowedForRepair = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startBooleanToggle(Component.translatable("easiercrafting.config.categorize"), ModConfig.get().categorizeRecipes)
                .setTooltip(Component.translatable("easiercrafting.config.tt.categorize"))
                .setSaveConsumer(newValue -> ModConfig.get().categorizeRecipes = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startIntField(Component.translatable("easiercrafting.config.fadeouttime"), ModConfig.get().fadeOutTime)
                .setTooltip(Component.translatable("easiercrafting.config.tt.fadeouttime"))
                .setMin(0).setMax(20)
                .setSaveConsumer(newValue -> ModConfig.get().fadeOutTime = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startBooleanToggle(Component.translatable("easiercrafting.config.showallrecipes"), ModConfig.get().showAllRecipes)
                .setTooltip(Component.translatable("easiercrafting.config.tt.showallrecipes"))
                .setSaveConsumer(newValue -> ModConfig.get().showAllRecipes = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Component.literal("Refill fuel for furnaces"), ModConfig.get().refillFuel)
                .setSaveConsumer(newValue -> ModConfig.get().refillFuel = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show background on recipe book"), ModConfig.get().recipeBackground)
                .setSaveConsumer(newValue -> ModConfig.get().recipeBackground = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startIntField(Component.literal("Spacing between item display"), ModConfig.get().itemDisplaySpacing)
                .setTooltip(Component.literal("how many pixels between item displays in recipes"))
                .setMin(0).setMax(5)
                .setSaveConsumer(newValue -> {
                    ModConfig.get().itemDisplaySpacing = newValue;
                    AbstractRecipeBook.displayItemSize = AbstractRecipeBook.ITEM_SIZE+ModConfig.get().itemDisplaySpacing;
                })
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Component.literal("Enable quick trade"), ModConfig.get().enableTrading)
                .setSaveConsumer(newValue -> ModConfig.get().enableTrading = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startIntField(Component.literal("Maximum item per row"), ModConfig.get().itemsPerRow)
                .setMin(2).setMax(20).setDefaultValue(9)
                .setSaveConsumer(newValue -> ModConfig.get().itemsPerRow = newValue)
                .build());

        return builder.build();
    }
}
