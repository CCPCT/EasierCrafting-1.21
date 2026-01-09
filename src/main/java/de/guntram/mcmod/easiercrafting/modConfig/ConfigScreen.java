package de.guntram.mcmod.easiercrafting.modConfig;

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

        ConfigCategory generalTab = builder.getOrCreateCategory(Text.translatable("easiercrafting.config.general"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // General settings
        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.translatable("easiercrafting.config.autofocus"), ModConfig.get().autoFocusSearch)
                .setTooltip(Text.translatable("easiercrafting.config.tt.autofocus"))
                .setSaveConsumer(newValue -> ModConfig.get().autoFocusSearch = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startIntField(Text.translatable("easiercrafting.config.autoupdate"), ModConfig.get().autoUpdateRecipeTimer)
                .setTooltip(Text.translatable("easiercrafting.config.tt.autoupdate"))
                .setMin(0).setMax(30)
                .setSaveConsumer(newValue -> ModConfig.get().autoUpdateRecipeTimer = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.translatable("easiercrafting.config.allowinternalbutton"), ModConfig.get().allowRecipeBook)
                .setTooltip(Text.translatable("easiercrafting.config.tt.allowinternalbutton"))
                .setSaveConsumer(newValue -> ModConfig.get().allowRecipeBook = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.translatable("easiercrafting.config.guiright"), ModConfig.get().showGuiRight)
                .setTooltip(Text.translatable("easiercrafting.config.tt.guiright"))
                .setSaveConsumer(newValue -> ModConfig.get().showGuiRight = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.translatable("easiercrafting.config.specialrecipes"), ModConfig.get().allowGeneratedRecipes)
                .setTooltip(Text.translatable("easiercrafting.config.tt.specialrecipes"))
                .setSaveConsumer(newValue -> ModConfig.get().allowGeneratedRecipes = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startIntField(Text.translatable("easiercrafting.config.maxenchants"), ModConfig.get().maxEnchantsAllowedForRepair)
                .setTooltip(Text.translatable("easiercrafting.config.tt.maxenchants"))
                .setMin(0).setMax(10)
                .setSaveConsumer(newValue -> ModConfig.get().maxEnchantsAllowedForRepair = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.translatable("easiercrafting.config.categorize"), ModConfig.get().categorizeRecipes)
                .setTooltip(Text.translatable("easiercrafting.config.tt.categorize"))
                .setSaveConsumer(newValue -> ModConfig.get().categorizeRecipes = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startIntField(Text.translatable("easiercrafting.config.fadeouttime"), ModConfig.get().fadeOutTime)
                .setTooltip(Text.translatable("easiercrafting.config.tt.fadeouttime"))
                .setMin(0).setMax(20)
                .setSaveConsumer(newValue -> ModConfig.get().fadeOutTime = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Text.translatable("easiercrafting.config.showallrecipes"), ModConfig.get().showAllRecipes)
                .setTooltip(Text.translatable("easiercrafting.config.tt.showallrecipes"))
                .setSaveConsumer(newValue -> ModConfig.get().showAllRecipes = newValue)
                .build());


        return builder.build();
    }
}
