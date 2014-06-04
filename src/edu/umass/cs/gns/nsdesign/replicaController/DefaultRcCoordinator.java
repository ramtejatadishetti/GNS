package edu.umass.cs.gns.nsdesign.replicaController;

import edu.umass.cs.gns.main.GNS;
import edu.umass.cs.gns.nsdesign.Replicable;
import edu.umass.cs.gns.nsdesign.packet.AddRecordPacket;
import edu.umass.cs.gns.nsdesign.packet.Packet;
import edu.umass.cs.gns.nsdesign.packet.RemoveRecordPacket;
import edu.umass.cs.gns.replicaCoordination.ReplicaControllerCoordinator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Trivial coordinator for a single name server GNS, which executes requests without any coordination.
 *
 * Created by abhigyan on 4/8/14.
 */
public class DefaultRcCoordinator implements ReplicaControllerCoordinator{
  private int nodeID;
  private Replicable app;

  public DefaultRcCoordinator(int nodeID, Replicable app) {
    this.nodeID = nodeID;
    this.app = app;
  }

  @Override
  public int coordinateRequest(JSONObject request) {
    if (this.app == null) return -1; // replicable app not set
    try {
      Packet.PacketType type = Packet.getPacketType(request);
      switch (type) {
        // no coordination needed on any requests
        case ADD_RECORD: // set a field here to know this node received request from client, and should send confirmation
          AddRecordPacket recordPacket = new AddRecordPacket(request);
          recordPacket.setNameServerID(nodeID);
          app.handleDecision(null, recordPacket.toString(), false);
          break;
        case REMOVE_RECORD: // set a field here to know this node received request from client, and should send confirmation
          RemoveRecordPacket removePacket = new RemoveRecordPacket(request);
          removePacket.setNameServerID(nodeID);
          app.handleDecision(null, removePacket.toString(), false);
          break;

        // nothing to do for these packets
        case RC_REMOVE:
        case NEW_ACTIVE_PROPOSE:
        case GROUP_CHANGE_COMPLETE:
        case REQUEST_ACTIVES:
        case NAMESERVER_SELECTION:
        case NAME_RECORD_STATS_RESPONSE:
        case ACTIVE_ADD_CONFIRM:
        case ACTIVE_REMOVE_CONFIRM:
        case OLD_ACTIVE_STOP_CONFIRM_TO_PRIMARY:
        case NEW_ACTIVE_START_CONFIRM_TO_PRIMARY:
        case NAME_SERVER_LOAD:
          // no coordination needed for these packet types.
          app.handleDecision(null, request.toString(), false);
          break;
        case REPLICA_CONTROLLER_COORDINATION:
          // we do not expect any coordination here
          throw new UnsupportedOperationException();
        default:
          GNS.getLogger().severe("Packet type not found in coordination: " + type);
          break;
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return 0;
  }

  @Override
  public int initGroupChange(String name) {
    return 0;
  }

  @Override
  public void reset() {
    // no action needed as there is no state.
  }
}