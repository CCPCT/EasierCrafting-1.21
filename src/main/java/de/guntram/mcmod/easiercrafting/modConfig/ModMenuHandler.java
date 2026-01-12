package de.guntram.mcmod.easiercrafting.modConfig;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.guntram.mcmod.easiercrafting.EasierCrafting;


public class ModMenuHandler implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        EasierCrafting.getGeneralLogger().info("Opened easiercrafting config screen!");
        return ConfigScreen::getConfigScreen;
    }
}
