package de.guntram.mcmod.easiercrafting;

//import de.guntram.mcmod.easiercrafting.Loom.LoomRecipeRegistry;
import de.guntram.mcmod.easiercrafting.modConfig.ModConfig;
import de.guntram.mcmod.easiercrafting.recipe.RecipeHandler;
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

//        File localRecipes = extractBundledFile("localrecipes.zip");
//
//
//        extractBundledFile("loomrecipes.zip");
//        extractConfigFileContents("loomrecipes.zip", "loomrecipes");
//        LoomRecipeRegistry.loadRecipeCollection(LoomRecipeRegistry.getRecipeCollectionPath());
    }

    public static <T> String recipeDisplayName(T recipe) {
        if (recipe instanceof RecipeDisplayEntry entry){
            return entry.display().result().getFirst(RecipeHandler.getEmptyContext()).toString();
        }
        if (recipe instanceof RecipeResultCollection entry){
            String display = entry.getAllRecipes().getFirst().toString();
            if (display.startsWith(MODID+":")) {
                display = I18n.translate(display);
                if (display.startsWith(MODID+":")) {
                    display=display.substring(MODID.length()+1);
                }
            } else {
                display = entry.getAllRecipes().getFirst().display().result().getFirst(RecipeHandler.getEmptyContext()).getName().getString();
            }
            return display;
        }
        if (recipe instanceof CuttingRecipeDisplay.GroupEntry<?> entry){
            return entry.recipe().optionDisplay().getFirst(RecipeHandler.getWorldContext()).getName().getString();
        }

        return "No Name :/";
    }

    
    private void extractConfigFileContents(String filename, String outputDirName) {
        
        File configDir = ConfigurationProvider.getSuggestedFile(MODID).getParentFile();
        File inputFile = new File(configDir, filename);
        try {
            if (inputFile.exists()) {
                File outputDir = new File(configDir, outputDirName);
                outputDir.mkdirs();
                if (outputDir.isDirectory()) {
                    ZipInputStream zipStream = new ZipInputStream(new FileInputStream(inputFile));
                    ZipEntry entry;
                    while ((entry = zipStream.getNextEntry())!=null) {
                        File outputFile = new File(outputDir, entry.getName());
                        if (!(outputFile.exists())) {
                            extractZipEntry(zipStream, outputFile);
                        }
                    }
                }
            }
        } catch (IOException ex) {
        }
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

}
