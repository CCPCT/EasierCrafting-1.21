package de.guntram.mcmod.easiercrafting.modConfig;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {

    protected ConfigScreen() {
        super(Text.literal("Easier Crafting Config"));
    }

    public static Screen getConfigScreen(Screen parent) {
        ModConfig.load();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Easier Crafting Config"))
                .setSavingRunnable(ModConfig::save);

        ConfigCategory generalTab = builder.getOrCreateCategory(Text.literal("General"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // General settings
        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Auto focus search text"), ModConfig.get().autoFocusSearch)
                .setSaveConsumer(newValue -> ModConfig.get().autoFocusSearch = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startIntField(Text.literal("Auto update recipe timer"), ModConfig.get().autoUpdateRecipeTimer)
                .setTooltip(Text.of("How long after craft will recipes be updated, in game ticks"))
                .setMin(0).setMax(30)
                .setSaveConsumer(newValue -> ModConfig.get().autoUpdateRecipeTimer = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Allow MC internal recipe book"), ModConfig.get().allowRecipeBook)
                .setSaveConsumer(newValue -> ModConfig.get().allowRecipeBook = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show GUI right of inventory"), ModConfig.get().showGuiRight)
                .setSaveConsumer(newValue -> ModConfig.get().showGuiRight = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Allow special recipes"), ModConfig.get().allowGeneratedRecipes)
                .setTooltip(Text.of("Allow mod generated recipes, such as repair item and dye shulker box"))
                .setSaveConsumer(newValue -> ModConfig.get().allowGeneratedRecipes = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startIntField(Text.literal("Max. enchants"), ModConfig.get().maxEnchantsAllowedForRepair)
                .setTooltip(Text.of("Max level of enchanted item allowed in recipe"))
                .setMin(0).setMax(10)
                .setSaveConsumer(newValue -> ModConfig.get().maxEnchantsAllowedForRepair = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Categorize recipes"), ModConfig.get().categorizeRecipes)
                .setTooltip(Text.of("use vanilla recipe book categorises to categorize recipes"))
                .setSaveConsumer(newValue -> ModConfig.get().categorizeRecipes = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startIntField(Text.literal("Fadeout Time"), ModConfig.get().fadeOutTime)
                .setTooltip(Text.of("How long to hide recipes after updating"))
                .setMin(0).setMax(20)
                .setSaveConsumer(newValue -> ModConfig.get().fadeOutTime = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show All Recipes"), ModConfig.get().showAllRecipes)
                .setTooltip(Text.of("Show all recipes on search, even those you cannot craft yet"))
                .setSaveConsumer(newValue -> ModConfig.get().showAllRecipes = newValue)
                .build());


        return builder.build();
    }
}
