package edu.umass.cs.gns.nameserver;

import edu.umass.cs.gns.main.GNS;
import edu.umass.cs.gns.packet.Transport;
import org.json.JSONObject;

/**
 * This class listens on a UDP port for messages (Lookups, Updates, Add, Remove)
 * from clients and 
 * 
 * @author abhigyan
 * 
 */
public class NSListenerUDP extends Thread
{
	public static Transport udpTransport;

	public NSListenerUDP()
	{
//		udpTransport = new Transport(NameServer.nodeID,
//				ConfigFileInfo.getLNSUdpPort(NameServer.nodeID),
//				NameServer.timer);
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				JSONObject incomingJSON = udpTransport.readPacket();
				NameServer.nsDemultiplexer.handleJSONObject(incomingJSON);
			} catch (Exception e)
			{
				GNS.getLogger().fine("Exception in thread: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}


}

