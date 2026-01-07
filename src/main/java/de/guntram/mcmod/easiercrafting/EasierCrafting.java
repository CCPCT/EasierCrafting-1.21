package de.guntram.mcmod.easiercrafting;

//import de.guntram.mcmod.easiercrafting.Loom.LoomRecipeRegistry;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import de.guntram.mcmod.fabrictools.ConfigurationProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.recipe.display.CuttingRecipeDisplay;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EasierCrafting implements ClientModInitializer 
{
    static public final String MODID="easiercrafting";
    static public final String MODNAME="EasierCrafting";

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        System.out.println("[EasierCrafting] Loaded");
    }
    
    private File extractBundledFile(String name) {
        File extractedFile = new File(ConfigurationProvider.getSuggestedFile(MODID).getParentFile(), name);
        if (!extractedFile.exists()) {
            extractBundledFile(extractedFile);
        }
        return extractedFile;
    }
    
    private void extractBundledFile(File target) {
        InputStream is;
        try {
            CodeSource src = EasierCrafting.class.getProtectionDomain().getCodeSource();
            if (src != null) {
                URL jar = src.getLocation();
                is = jar.openStream();
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equalsIgnoreCase(target.getName())) {
                        extractZipEntry(zis, target);
                        break;
                    }
                }
                zis.close();
            }
        } catch (IOException ex) {
        }
    }

    private void extractZipEntry(ZipInputStream zipStream, File outputFile) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputFile);
            byte[] buf=new byte[16384];
            int length;
            while ((length=zipStream.read(buf, 0, buf.length))>=0) {
                fos.write(buf, 0, length);
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static String getModid() {
        return MODID;
    }
    public static String getModName() {
        return MODNAME;
    }

}
