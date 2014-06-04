package edu.umass.cs.gns.replicaCoordination.multipaxos.paxosutil;

import edu.umass.cs.gns.nio.BasicPacketDemultiplexer;
import edu.umass.cs.gns.nsdesign.packet.Packet;
import edu.umass.cs.gns.replicaCoordination.multipaxos.PaxosManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
@author V. Arun
 */
/* Needed to get NIO to send paxos packets to PaxosManager */
public class PaxosPacketDemultiplexer extends BasicPacketDemultiplexer {

	private final PaxosManager paxosManager;

	public PaxosPacketDemultiplexer(PaxosManager pm) {
		paxosManager = pm;
	}

	public boolean handleJSONObject(JSONObject jsonMsg) {
    boolean isPacketTypeFound = true;

		try {
			Packet.PacketType type = Packet.getPacketType(jsonMsg);
			switch (type) {
			case PAXOS_PACKET:
				paxosManager.handleIncomingPacket(jsonMsg);
				break;
      default:
        isPacketTypeFound = false;
        break;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
    return isPacketTypeFound;
	}

	/**
	 * @param args
	 */
	 public static void main(String[] args) {
		// TODO Auto-generated method stub

	 }

}