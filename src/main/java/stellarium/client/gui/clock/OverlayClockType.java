package stellarium.client.gui.clock;

import stellarium.client.gui.IGuiOverlayType;
import stellarium.client.gui.IRawHandler;
import stellarium.client.gui.pos.EnumHorizontalPos;
import stellarium.client.gui.pos.EnumVerticalPos;

public class OverlayClockType implements IGuiOverlayType<OverlayClock, ClockSettings> {

	@Override
	public OverlayClock generateElement() {
		return new OverlayClock();
	}

	@Override
	public ClockSettings generateSettings() {
		return new ClockSettings();
	}

	@Override
	public String getName() {
		return "Clock";
	}

	@Override
	public EnumHorizontalPos defaultHorizontalPos() {
		return EnumHorizontalPos.LEFT;
	}

	@Override
	public EnumVerticalPos defaultVerticalPos() {
		return EnumVerticalPos.UP;
	}


	@Override
	public boolean accepts(EnumHorizontalPos horizontal, EnumVerticalPos vertical) {
		return vertical == EnumVerticalPos.UP;
	}
	

	@Override
	public IRawHandler generateRawHandler() {
		return null;
	}

}
