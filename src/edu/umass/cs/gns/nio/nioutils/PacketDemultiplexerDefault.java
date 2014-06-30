package edu.umass.cs.gns.nio.nioutils;

import org.json.JSONObject;

import edu.umass.cs.gns.nio.AbstractPacketDemultiplexer;

/**
@author V. Arun
 */

/* Default packet multiplexer simply prints the received JSON object.
 * Note: we have both a DataProcessingWorker and BasicPacketDemultiplexer
 * as the former is for a byte stream and independent of the GNS. The
 * PacketMultiplexer interface is for processing JSON messages. A 
 * GNS-specific DataProcessingWorker would include a GNS-specific
 * packet demultiplexer. This default class is used just for testing.
 */
public class PacketDemultiplexerDefault extends AbstractPacketDemultiplexer {

	@Override
  public boolean handleJSONObject(JSONObject jsonObject) {
    incrPktsRcvd();
    //System.out.println("Received pkt: " + jsonObject);
    return false; // WARNING: Do not change this to true. It could break the GNS by not trying any other PDs.
  }
}