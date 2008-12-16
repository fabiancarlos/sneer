package snapps.gis.ui.tests;

import static wheel.lang.Environments.my;
import snapps.gis.ui.LocationGui;
import sneer.kernel.container.Containers;
import wheel.lang.Environments;
import wheel.lang.Threads;

public class LocationGuiMain  {

	public static void main(String[] args) throws Exception {
		Environments.runWith(Containers.newContainer(), new Runnable() { @Override public void run() { 
			my(LocationGui.class);
			Threads.sleepWithoutInterruptions(40000);
		}});
	}
}
