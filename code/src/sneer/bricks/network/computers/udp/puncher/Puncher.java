package sneer.bricks.network.computers.udp.puncher;

import java.io.IOException;
import java.net.InetSocketAddress;

import basis.brickness.Brick;

import sneer.bricks.identity.seals.Seal;

@Brick
public interface Puncher {
	InetSocketAddress rendezvous(Seal seal) throws IOException;
}
