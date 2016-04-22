package stellarium.stellars.display.eqcoord;

import java.util.List;

import com.google.common.collect.Lists;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import stellarium.render.ICelestialObjectRenderer;
import stellarium.stellars.display.DisplayElement;
import stellarium.stellars.display.IDisplayRenderCache;

public class DisplayEqCoord extends DisplayElement {

	public boolean displayEnabled;
	public int displayFrag;
	public double displayAlpha;
	public double[] displayLatitudeColor;
	public double[] displayLongitudeColor;
	
	private Property propDisplayEnabled, propDisplayAlpha, propDisplayFrag;
	private Property propDisplayLatitudeColor, propDisplayLongitudeColor;
	
	@Override
	public void setupConfig(Configuration config, String category) {
		config.setCategoryComment(category, "Configurations for Display of Equatorial Coordinate Grid.");
		config.setCategoryLanguageKey(category, "config.category.display.eqcoord");
		config.setCategoryRequiresMcRestart(category, false);
		
		List<String> propNameList = Lists.newArrayList();
		
        propDisplayEnabled=config.get(category, "Display_Enabled", false);
        propDisplayEnabled.comment="Set to true to enable display of equatorial coordinates.";
        propDisplayEnabled.setRequiresMcRestart(false);
        propDisplayEnabled.setLanguageKey("config.property.display.enabled");
        propNameList.add(propDisplayEnabled.getName());
        
        propDisplayAlpha=config.get(category, "Display_Alpha", 0.2);
        propDisplayAlpha.comment="Alpha(Brightness) of the display.";
        propDisplayAlpha.setRequiresMcRestart(false);
        propDisplayAlpha.setLanguageKey("config.property.display.alpha");
        propNameList.add(propDisplayAlpha.getName());
        
        propDisplayFrag=config.get(category, "Display_Fragments_Number", 32);
        propDisplayFrag.comment="Number of fragments of display grids in horizontal direction.";
        propDisplayFrag.setRequiresMcRestart(false);
        propDisplayFrag.setLanguageKey("config.property.display.eqcoord.fragments");
        propNameList.add(propDisplayFrag.getName());
        
        propDisplayLatitudeColor=config.get(category, "Display_Latitude_Color", new double[] {1.0, 0.0, 0.0});
        propDisplayLatitudeColor.comment = "Color factor for latitude, the grid tends to have this color when latitude gets bigger.";
        propDisplayLatitudeColor.setIsListLengthFixed(true);
        propDisplayLatitudeColor.setRequiresMcRestart(false);
        propDisplayLatitudeColor.setLanguageKey("config.property.display.eqcoord.color.latitude");
        propNameList.add(propDisplayLatitudeColor.getName());

        propDisplayLongitudeColor=config.get(category, "Display_Longitude_Color", new double[] {0.0, 1.0, 0.0});
        propDisplayLatitudeColor.comment = "Color factor for longitude, the grid tends to have this color when longitude gets bigger.";
        propDisplayLongitudeColor.setIsListLengthFixed(true);
        propDisplayLongitudeColor.setRequiresMcRestart(false);
        propDisplayLongitudeColor.setLanguageKey("config.property.display.eqcoord.color.longitude");
        propNameList.add(propDisplayLongitudeColor.getName());
        
        config.setCategoryPropertyOrder(category, propNameList);
	}

	@Override
	public void loadFromConfig(Configuration config, String category) {
		this.displayEnabled = propDisplayEnabled.getBoolean();
		this.displayAlpha = propDisplayAlpha.getDouble();
		this.displayFrag = propDisplayFrag.getInt();
		this.displayLatitudeColor = propDisplayLatitudeColor.getDoubleList();
		this.displayLongitudeColor = propDisplayLongitudeColor.getDoubleList();
	}

	@Override
	public void saveToConfig(Configuration config, String category) {
		// TODO save to configuration; not done till now		
	}
	
	@Override
	public String getID() {
		return "Equatorial_Coordinate_Grid";
	}

	@Override
	public DisplayElement copy() {
		DisplayEqCoord copied = new DisplayEqCoord();
		copied.displayAlpha = this.displayAlpha;
		copied.displayEnabled = this.displayEnabled;
		copied.displayFrag = this.displayFrag;
		copied.displayLatitudeColor = this.displayLatitudeColor;
		copied.displayLongitudeColor = this.displayLongitudeColor;
		return copied;
	}

	@Override
	public IDisplayRenderCache generateCache() {
		return new DisplayEqCoordCache();
	}

	@SideOnly(Side.CLIENT)
	@Override
	public ICelestialObjectRenderer getRenderer() {
		return new DisplayEqCoordRenderer();
	}

}
