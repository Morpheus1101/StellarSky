package stellarium.command;

import net.minecraft.command.CommandTime;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import stellarium.api.ISkyProvider;
import stellarium.api.StellarSkyAPI;

public class FixedCommandTime extends CommandTime {
	
	@Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (args.length > 1)
        {
            long i;

            if (args[0].equals("set"))
            {
                if (args[1].equals("day"))
                {
                    i = this.getModifiedTimeByAngle(sender.getEntityWorld(), 10.0);
                }
                else if (args[1].equals("night"))
                {
                    i = this.getModifiedTimeByOffset(sender.getEntityWorld(), 0.5);
                }
                else
                {
                    i = parseIntWithMin(sender, args[1], 0);
                }

                this.setTime(sender, i);
                func_152373_a(sender, this, "commands.time.set", new Object[] {Long.valueOf(i)});
                return;
            }

            if (args[0].equals("add"))
            {
                i = parseIntWithMin(sender, args[1], 0);
                this.addTime(sender, i);
                func_152373_a(sender, this, "commands.time.added", new Object[] {Long.valueOf(i)});
                return;
            }
        }

        throw new WrongUsageException("commands.time.usage", new Object[0]);
    }
	
	/**
     * Set the time in the server object.
     */
    protected void setTime(ICommandSender p_71552_1_, long p_71552_2_)
    {
        for (int j = 0; j < MinecraftServer.getServer().worldServers.length; ++j)
        {
            MinecraftServer.getServer().worldServers[j].setWorldTime(p_71552_2_);
        }
    }

    /**
     * Adds (or removes) time in the server object.
     */
    protected void addTime(ICommandSender p_71553_1_, long p_71553_2_)
    {
        for (int j = 0; j < MinecraftServer.getServer().worldServers.length; ++j)
        {
            WorldServer worldserver = MinecraftServer.getServer().worldServers[j];
            worldserver.setWorldTime(worldserver.getWorldTime() + p_71553_2_);
        }
    }
	
	public long getModifiedTimeByAngle(World world, double angle) {
		if(!StellarSkyAPI.hasSkyProvider(world)) {
			return (long) (angle / 180.0 * 24000);
		}
		
		long time = world.getWorldTime();
    	ISkyProvider skyProvider = StellarSkyAPI.getSkyProvider(world);
    	double wakeDayOffset = skyProvider.dayOffsetUntilSunReach(angle);
		double currentDayOffset = skyProvider.getDaytimeOffset(time);
		double dayLength = skyProvider.getDayLength();

    	double modifiedWorldTime = time + (-wakeDayOffset - currentDayOffset) * dayLength;
    	while(modifiedWorldTime < time)
    		modifiedWorldTime += dayLength;
    	
    	return (long) modifiedWorldTime;
	}
	
	public long getModifiedTimeByOffset(World world, double timeOffset) {
		if(!StellarSkyAPI.hasSkyProvider(world)) {
			return (long) (timeOffset * 24000);
		}
		
		long time = world.getWorldTime();
    	ISkyProvider skyProvider = StellarSkyAPI.getSkyProvider(world);
    	double wakeDayOffset = timeOffset;
		double currentDayOffset = skyProvider.getDaytimeOffset(time);
		double dayLength = skyProvider.getDayLength();

    	double modifiedWorldTime = time + (-wakeDayOffset - currentDayOffset) * dayLength;
    	while(modifiedWorldTime < time)
    		modifiedWorldTime += dayLength;
    	
    	return (long) modifiedWorldTime;
	}

}
