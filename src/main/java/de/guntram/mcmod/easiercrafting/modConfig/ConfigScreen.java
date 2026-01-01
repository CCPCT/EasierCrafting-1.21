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
        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Auto focus search text"), ModConfig.getInstance().autoFocusSearch)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.getInstance().autoFocusSearch = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startIntField(Text.literal("Auto update recipe timer"), ModConfig.getInstance().autoUpdateRecipeTimer)
                .setDefaultValue(2)
                .setMin(0).setMax(30)
                .setSaveConsumer(newValue -> ModConfig.getInstance().autoUpdateRecipeTimer = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Allow MC internal recipe book"), ModConfig.getInstance().allowRecipeBook)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.getInstance().allowRecipeBook = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show GUI right of inventory"), ModConfig.getInstance().showGuiRight)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.getInstance().showGuiRight = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Allow special recipes"), ModConfig.getInstance().allowGeneratedRecipes)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.getInstance().allowGeneratedRecipes = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startIntField(Text.literal("Max. enchants"), ModConfig.getInstance().maxEnchantsAllowedForRepair)
                .setDefaultValue(0)
                .setMin(0).setMax(10)
                .setSaveConsumer(newValue -> ModConfig.getInstance().maxEnchantsAllowedForRepair = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.literal("Categorize recipes"), ModConfig.getInstance().categorizeRecipes)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.getInstance().categorizeRecipes = newValue)
                .build());


        generalTab.addEntry(entryBuilder.startIntField(Text.literal("Loom Click Speed"), ModConfig.getInstance().loomClickSpeed)
                .setDefaultValue(3)
                .setMin(0).setMax(20)
                .setSaveConsumer(newValue -> ModConfig.getInstance().loomClickSpeed = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startIntField(Text.literal("Fadeout Time"), ModConfig.getInstance().fadeOutTime)
                .setDefaultValue(500)
                .setMin(0).setMax(2000)
                .setSaveConsumer(newValue -> ModConfig.getInstance().fadeOutTime = newValue)
                .build());



        return builder.build();
    }
}
