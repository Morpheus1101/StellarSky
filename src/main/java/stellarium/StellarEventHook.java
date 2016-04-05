package stellarium;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.google.common.base.Throwables;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayer.EnumStatus;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.world.WorldEvent;
import stellarium.api.StellarSkyAPI;
import stellarium.render.SkyRenderer;
import stellarium.stellars.StellarManager;
import stellarium.stellars.layer.CelestialManager;
import stellarium.stellars.view.StellarDimensionManager;
import stellarium.world.StellarWorldProvider;

public class StellarEventHook {
	
	private static Field providerField = ReflectionHelper.findField(World.class,
			ObfuscationReflectionHelper.remapFieldNames(World.class.getName(), "provider", "field_73011_w"));
	
	static {
		try {
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(providerField, providerField.getModifiers() & ~ Modifier.FINAL);
		} catch(Exception exc) {
			Throwables.propagate(exc);
		}
	}
	
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load e)
	{
		StellarManager manager;
		if(!StellarManager.hasManager(e.world, e.world.isRemote)) {
			manager = StellarManager.loadOrCreateManager(e.world);
		} else manager = StellarManager.getManager(e.world.isRemote);
		
		if(manager.getCelestialManager() == null && (!e.world.isRemote || !manager.getSettings().serverEnabled))
			setupManager(e.world, manager);
		
		String dimName = e.world.provider.getDimensionName();
		if(!e.world.isRemote || !manager.getSettings().serverEnabled)
			if(StellarSky.proxy.dimensionSettings.hasSubConfig(dimName)) {
				StellarDimensionManager dimManager = StellarDimensionManager.loadOrCreate(e.world, manager, dimName);
				setupDimension(e.world, manager, dimManager);
			}
		
		if(e.world.isRemote && mark) {
			handleNotHaveModOnServer(e.world);
			mark = false;
		}
	}
	
	public static void setupManager(World world, StellarManager manager) {
		if(world.isRemote)
			manager.setup(StellarSky.proxy.getClientCelestialManager());
		else manager.setup(new CelestialManager(false));
	}
	
	public static void setupDimension(World world, StellarManager manager, StellarDimensionManager dimManager) {
		dimManager.setup();
		
		if(manager.getSettings().serverEnabled && dimManager.getSettings().patchProvider) {
			try {
				providerField.set(world, new StellarWorldProvider(world.provider, manager, dimManager));
			} catch (Exception exc) {
				Throwables.propagate(exc);
			}
		}
		
		if(world.isRemote)
			world.provider.setSkyRenderer(new SkyRenderer());
	}
	
	private static boolean mark = false;
	
	private static void handleNotHaveModOnServer(World world) {
		StellarManager manager = StellarManager.loadOrCreateManager(world);
		manager.handleServerWithoutMod();
		
		if(manager.getCelestialManager() == null)
		{
			StellarEventHook.setupManager(world, manager);
			
			String dimName = world.provider.getDimensionName();
			if(StellarSky.proxy.dimensionSettings.hasSubConfig(dimName)) {
				StellarDimensionManager dimManager = StellarDimensionManager.loadOrCreate(world, manager, dimName);
				StellarEventHook.setupDimension(world, manager, dimManager);
			}
		}
	}
	
	public static void markNotHave() {
		mark = true;
	}
	
	@SubscribeEvent
	public void onSleepInBed(PlayerSleepInBedEvent event) {
		if(!StellarSky.proxy.wakeManager.isEnabled() || event.entityPlayer.worldObj.isRemote) {
			return;
		}

		if(event.result == null || event.result == EnumStatus.OK || event.result == EnumStatus.NOT_POSSIBLE_NOW) {
			World worldObj = event.entityPlayer.worldObj;
			if (!StellarSky.proxy.wakeManager.canSkipTime(worldObj, StellarSkyAPI.getSkyProvider(worldObj), worldObj.getWorldTime()))
				event.result = EntityPlayer.EnumStatus.NOT_POSSIBLE_NOW;
		}
	}
	
}
