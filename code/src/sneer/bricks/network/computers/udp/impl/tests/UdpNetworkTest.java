package sneer.bricks.network.computers.udp.impl.tests;

import static basis.environments.Environments.my;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.junit.Test;

import basis.lang.Consumer;
import basis.util.concurrent.Latch;

import sneer.bricks.network.computers.udp.UdpNetwork;
import sneer.bricks.network.computers.udp.UdpNetwork.UdpSocket;
import sneer.bricks.software.folderconfig.testsupport.BrickTestBase;



public class UdpNetworkTest extends BrickTestBase {
	

	@Test(timeout = 2000)
	public void packetsBackAndForth() throws IOException {
		UdpNetwork subject = my(UdpNetwork.class);
		
		int portNumber1 = 1111;
		int portNumber2 = 2222;
		final UdpSocket s1 = subject.openSocket(portNumber1);
		final UdpSocket s2 = subject.openSocket(portNumber2);
		
		startUppercaseEchoOn(s1);

		final Latch latch = new Latch();
		final StringBuffer replies = new StringBuffer(); 
		s2.initReceiver(new Consumer<DatagramPacket>() { @Override public void consume(DatagramPacket packet) {
			replies.append(unmarshal(packet));
			if (replies.toString().equals("HI THERE MY FRIENDS "))
				latch.open();
		}});
		
		s2.send(marshal("hi ", portNumber1));
		s2.send(marshal("there ", portNumber1));
		s2.send(marshal("my ", portNumber1));
		s2.send(marshal("friends ", portNumber1));
		latch.waitTillOpen();
		
		s1.crash();
		s2.crash();
	}

	
	private String unmarshal(DatagramPacket packet) {
		return new String(packet.getData(), 0, packet.getLength());
	}
	
	
	private DatagramPacket marshal(String string, int portNumber) throws SocketException {
		byte[] bytes = string.getBytes();
		return new DatagramPacket(bytes, bytes.length, new InetSocketAddress("localhost", portNumber));
	}

	
	private void startUppercaseEchoOn(final UdpSocket s) {
		s.initReceiver(new Consumer<DatagramPacket>() { @Override public void consume(DatagramPacket packet) {
			convertToUppercase(packet);
			try {
				s.send(packet);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}});
	}
	
	
	private void convertToUppercase(DatagramPacket packet) {
		String upper = new String(packet.getData(), 0, packet.getLength()).toUpperCase(); 
		System.arraycopy(upper.getBytes(), 0, packet.getData(), 0, upper.length());
	}
	
}
