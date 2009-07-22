package sneer.bricks.softwaresharing;

import java.util.List;

public interface BrickInfo {
	
	public enum Status {
		NEW,
		DIFFERENT,
		DIVERGING,
		CURRENT, 
		REJECTED
	}
	
	String name();
	List<BrickVersion> versions();
	
	Status status();
	boolean isSnapp();
}
