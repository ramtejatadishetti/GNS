package edu.umass.cs.gns.packet.paxospacket;

import edu.umass.cs.gns.paxos.Ballot;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: abhigyan
 * Date: 8/2/13
 * Time: 10:13 AM
 * To change this template use File | Settings | File Templates.
 */
public class StatePacket extends Packet{

  public Ballot b;
  public int slotNumber;
  public String state;

  public StatePacket(Ballot b, int slotNumber, String state) {
    this.b = b;
    this.slotNumber = slotNumber;
    this.state = state;
  }


  @Override
  public JSONObject toJSONObject() throws JSONException {
    return null;

  }

}
