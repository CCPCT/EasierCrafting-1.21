package de.guntram.mcmod.easiercrafting.modConfig;

import de.guntram.mcmod.easiercrafting.recipebook.AbstractRecipeBook;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {

    protected ConfigScreen() {
        super(Text.translatable("easiercrafting.config.title"));
    }

    public static Screen getConfigScreen(Screen parent) {
        ModConfig.load();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("easiercrafting.config.title"))
                .setSavingRunnable(ModConfig::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory generalTab = builder.getOrCreateCategory(Text.translatable("easiercrafting.config.general"));
        ConfigCategory visualTab = builder.getOrCreateCategory(Text.literal("visual"));
        ConfigCategory recipeTab = builder.getOrCreateCategory(Text.literal("recipe"));


        // General settings
        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable Mod"), ModConfig.get().modEnabled)
                .setSaveConsumer(newValue -> ModConfig.get().modEnabled = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startBooleanToggle(Text.translatable("easiercrafting.config.autofocus"), ModConfig.get().autoFocusSearch)
                .setTooltip(Text.translatable("easiercrafting.config.tt.autofocus"))
                .setSaveConsumer(newValue -> ModConfig.get().autoFocusSearch = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startIntField(Text.translatable("easiercrafting.config.autoupdate"), ModConfig.get().autoUpdateRecipeTimer)
                .setTooltip(Text.translatable("easiercrafting.config.tt.autoupdate"))
                .setMin(0).setMax(30)
                .setSaveConsumer(newValue -> ModConfig.get().autoUpdateRecipeTimer = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startBooleanToggle(Text.translatable("easiercrafting.config.allowinternalbutton"), ModConfig.get().allowRecipeBook)
                .setTooltip(Text.translatable("easiercrafting.config.tt.allowinternalbutton"))
                .setSaveConsumer(newValue -> ModConfig.get().allowRecipeBook = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startBooleanToggle(Text.translatable("easiercrafting.config.guiright"), ModConfig.get().showGuiRight)
                .setTooltip(Text.translatable("easiercrafting.config.tt.guiright"))
                .setSaveConsumer(newValue -> ModConfig.get().showGuiRight = newValue)
                .build());

        recipeTab.addEntry(entryBuilder.startBooleanToggle(Text.translatable("easiercrafting.config.specialrecipes"), ModConfig.get().allowGeneratedRecipes)
                .setTooltip(Text.translatable("easiercrafting.config.tt.specialrecipes"))
                .setSaveConsumer(newValue -> ModConfig.get().allowGeneratedRecipes = newValue)
                .build());

        recipeTab.addEntry(entryBuilder.startIntField(Text.translatable("easiercrafting.config.maxenchants"), ModConfig.get().maxEnchantsAllowedForRepair)
                .setTooltip(Text.translatable("easiercrafting.config.tt.maxenchants"))
                .setMin(0).setMax(10)
                .setSaveConsumer(newValue -> ModConfig.get().maxEnchantsAllowedForRepair = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startBooleanToggle(Text.translatable("easiercrafting.config.categorize"), ModConfig.get().categorizeRecipes)
                .setTooltip(Text.translatable("easiercrafting.config.tt.categorize"))
                .setSaveConsumer(newValue -> ModConfig.get().categorizeRecipes = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startIntField(Text.translatable("easiercrafting.config.fadeouttime"), ModConfig.get().fadeOutTime)
                .setTooltip(Text.translatable("easiercrafting.config.tt.fadeouttime"))
                .setMin(0).setMax(20)
                .setSaveConsumer(newValue -> ModConfig.get().fadeOutTime = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startBooleanToggle(Text.translatable("easiercrafting.config.showallrecipes"), ModConfig.get().showAllRecipes)
                .setTooltip(Text.translatable("easiercrafting.config.tt.showallrecipes"))
                .setSaveConsumer(newValue -> ModConfig.get().showAllRecipes = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Refill fuel for furnaces"), ModConfig.get().refillFuel)
                .setSaveConsumer(newValue -> ModConfig.get().refillFuel = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show background on recipe book"), ModConfig.get().recipeBackground)
                .setSaveConsumer(newValue -> ModConfig.get().recipeBackground = newValue)
                .build());

        visualTab.addEntry(entryBuilder.startIntField(Text.literal("Spacing between item display"), ModConfig.get().itemDisplaySpacing)
                .setTooltip(Text.literal("how many pixels between item displays in recipes"))
                .setMin(0).setMax(5)
                .setSaveConsumer(newValue -> {
                    ModConfig.get().itemDisplaySpacing = newValue;
                    AbstractRecipeBook.displayItemSize = AbstractRecipeBook.itemSize+ModConfig.get().itemDisplaySpacing;
                })
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Improve villager trading"), ModConfig.get().enableTrading)
                .setSaveConsumer(newValue -> ModConfig.get().enableTrading = newValue)
                .build());

        return builder.build();
    }
}
