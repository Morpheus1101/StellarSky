package stellarium.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import stellarapi.api.StellarAPIReference;
import stellarapi.api.celestials.ICelestialObject;
import stellarapi.api.celestials.IEffectorType;
import stellarium.StellarSky;
import stellarium.api.ICelestialRenderer;
import stellarium.client.ClientSettings;
import stellarium.stellars.StellarManager;
import stellarium.stellars.layer.CelestialManager;
import stellarium.world.StellarDimensionManager;

public class SkyRenderCelestial implements ICelestialRenderer {
	
	private ClientSettings settings;
	private StellarRenderer renderer;
	private boolean updated = false;
	
	public SkyRenderCelestial() {
		this.settings = StellarSky.proxy.getClientSettings();
		this.renderer = new StellarRenderer();
		
		CelestialManager celManager = StellarSky.proxy.getClientCelestialManager();
		renderer.refreshRenderer();
		settings.checkDirty();
		celManager.reloadClientSettings(this.settings);
		this.onSettingsUpdated(Minecraft.getMinecraft());
	}
	
	public void renderCelestial(Minecraft mc, float[] skycolor, float weathereff, float partialTicks) {
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		
		CelestialManager manager = StellarSky.proxy.getClientCelestialManager();

		if(settings.checkDirty())
		{
			manager.reloadClientSettings(this.settings);
			this.updated = false;
		}
		
		if(!this.updated)
			this.onSettingsUpdated(mc);
		
		renderer.render(new StellarRenderInfo(mc, Tessellator.instance, skycolor, weathereff, partialTicks), manager.getLayers());
	}
	
	private void onSettingsUpdated(Minecraft mc) {
		//Initialization update
		World world = mc.theWorld;
		
		if(world != null) {
			StellarManager manager = StellarManager.getManager(true);
			if(manager.getCelestialManager() != null) {
				this.updated = true;
				manager.update(world.getWorldTime());
				StellarDimensionManager dimManager = StellarDimensionManager.get(world);
				if(dimManager != null)
					dimManager.update(world, world.getWorldTime());
				
				manager.updateClient(StellarSky.proxy.getClientSettings());
			}
		}
	}
	

	@Override
	public void renderSunriseSunsetEffect(Minecraft mc, float[] colors, float partialTicks) {
        Tessellator tessellator1 = Tessellator.instance;
		
        ICelestialObject object = StellarAPIReference.getEffectors(mc.theWorld, IEffectorType.Light).getPrimarySource();
        
		GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glPushMatrix();
        GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(-(float)object.getCurrentHorizontalPos().x, 0.0F, 0.0F, 1.0F);
        GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
        float f6 = colors[0];
        float f7 = colors[1];
        float f8 = colors[2];
        float f9, f10, f11;

        if (mc.gameSettings.anaglyph)
        {
            f9 = (f6 * 30.0F + f7 * 59.0F + f8 * 11.0F) / 100.0F;
            f10 = (f6 * 30.0F + f7 * 70.0F) / 100.0F;
            f11 = (f6 * 30.0F + f8 * 70.0F) / 100.0F;
            f6 = f9;
            f7 = f10;
            f8 = f11;
        }

        tessellator1.startDrawing(6);
        tessellator1.setColorRGBA_F(f6, f7, f8, colors[3]);
        tessellator1.addVertex(0.0D, 100.0D, 0.0D);
        byte b0 = 16;
        tessellator1.setColorRGBA_F(colors[0], colors[1], colors[2], 0.0F);

        for (int j = 0; j <= b0; ++j)
        {
            f11 = (float)j * (float)Math.PI * 2.0F / (float)b0;
            float f12 = MathHelper.sin(f11);
            float f13 = MathHelper.cos(f11);
            tessellator1.addVertex((double)(f12 * 120.0F), (double)(f13 * 120.0F), (double)(-f13 * 40.0F * colors[3]));
        }

        tessellator1.draw();
        GL11.glPopMatrix();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

}
