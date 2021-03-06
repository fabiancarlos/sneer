package sneer.bricks.pulp.blinkinglights;

import basis.brickness.Brick;
import basis.lang.exceptions.FriendlyException;
import sneer.bricks.pulp.reactive.collections.ListSignal;

@Brick
public interface BlinkingLights {

	ListSignal<Light> lights();
	Light prepare(LightType type);

	void turnOnIfNecessary(Light light, FriendlyException e);
	void turnOnIfNecessary(Light light, FriendlyException e, int timeout);
	void turnOnIfNecessary(Light light, String caption, Throwable t);
	void turnOnIfNecessary(Light light, String caption, String helpMessage);
	void turnOnIfNecessary(Light light, String caption, String helpMessage, Throwable t);
	void turnOnIfNecessary(Light light, String caption, String helpMessage, Throwable t, int timeout);

	Light turnOn(LightType type, String caption, String helpMessage);
	Light turnOn(LightType type, String caption, String helpMessage, int timeToLive);
	Light turnOn(LightType type, String caption, String helpMessage, Throwable t);
	Light turnOn(LightType type, String caption, String helpMessage, Throwable t, int timeToLive);

	void turnOffIfNecessary(Light light);
}
