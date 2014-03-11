package edu.umass.cs.gns.nio;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
@author V. Arun
 */

/* This class exists primarily as a GNS wrapper around NIOTransport. NIOTransport 
 * is for general-purpose NIO byte stream communication between numbered nodes as 
 * specified by the NodeConfig interface and a data processing worker as specified 
 * by the DataProcessingWorker interface. 
 * 
 * The GNS-specific functions include (1) delay emulation, (2) short-circuiting 
 * local sends by directly sending it to packetDemux, and (3) adding GNS-specific 
 * headers to NIO's byte stream abstraction, and (4) supporting a crazy number of 
 * redundant public methods to do different kinds of sends. These methods exist 
 * only for backwards compatibility.  
 * 
 * 
 */
public class GNSNIOTransport extends NIOTransport {
	
	Timer timer = new Timer();
	
	public GNSNIOTransport(int id, NodeConfig nodeConfig, JSONMessageWorker worker) throws IOException {
		super(id, nodeConfig, worker); // Switched order of the latter two arguments
	}
	public void addPacketDemultiplexer(PacketDemultiplexer pd) {
		((JSONMessageWorker)this.worker).addPacketDemultiplexer(pd);
	}
	
	/********************Start of send methods*****************************************/
	/* A sequence of ugly public methods that are essentially redundant ways of 
	 * invoking send() on NIOTransport. They exist primarily for backwards compatibility
	 * and must be cleaned up to a smaller number that is really needed. None of these 
	 * methods actually sends on a socket. That is done by a single private method that
	 * invokes the underlying send in the parent.
	 * 
	 * These methods are undocumented because it is not clear which classes are designed
	 * to call which of these methods. They have been copied over from the older NioServer
	 * and need to be documented or overhauled completely.
	 */
	public int sendToIDs(Set<Integer> destIDs, JSONObject jsonData) throws IOException {
		return sendToIDs(destIDs, jsonData, -1);
	}

	public int sendToIDs(short[] destIDs, JSONObject jsonData) throws IOException {
		return sendToIDs(destIDs, jsonData, -1);
	}

	public int sendToIDs(short[]destIDs, JSONObject jsonData, int excludeID) throws IOException {
		TreeSet<Integer> IDs = new TreeSet<Integer>();
		for (int destID: destIDs) {
			IDs.add((int)destIDs[destID]);
		}
		return sendToIDs(IDs, jsonData, excludeID);
	}

    public int sendToIDs(Set<Integer> destIDs, JSONObject jsonData, int excludeID) throws IOException {
    	int written=0;
		for (int destID:destIDs) {
			if (destID == excludeID) continue;
			written += sendToID(destID, jsonData);
		}
		return written;
	}

    /* FIXME: This method returns a meaningless value. Need to get 
     * return value from task scheduled in the future, so we need
     * to use Executor instead of Timer in GNSDelayEmulator.
     */
	public int sendToID(int id, JSONObject jsonData) throws IOException {
		GNSDelayEmulator.sendWithDelay(timer, this, id, jsonData);
		return jsonData.length(); 
	}

	/* This method adds a header only if a socket channel is used to send to
	 * a remote node, otherwise it hands over the message directly to the worker.
	 */
	public int sendToIDActual(int destID, JSONObject jsonData) throws IOException {
		int written = 0;
		if(destID==this.myID) {
			ArrayList<JSONObject> jsonArray = new ArrayList<JSONObject>();
			jsonArray.add(jsonData);
			NIOInstrumenter.incrSent(); // instrumentation
			((JSONMessageWorker)worker).processJSONMessages(jsonArray);
			written = jsonData.length();
		}
		else {
			String headeredMsg = JSONMessageWorker.prependHeader(jsonData.toString());
			written = this.sendUnderlying(destID, headeredMsg.getBytes());
		}
		return written;
	}
	/********************End of public send methods*****************************************/	
	
	/* This method is really redundant. But it exists so that there is one place where
	 * all NIO sends actually happen given the maddening number of different public send
	 * methods above. Do NOT add more gunk to this method.
	 */
	private int sendUnderlying(int id, byte[] data) throws IOException {
		return this.send(id, data);
	}
	private static JSONObject JSONify(int msgNum, String s) throws JSONException{
		return new JSONObject("{\"msg\" : \"" + s + "\" , \"msgNum\" : " + msgNum + "}");
	}
	

	/* The test code here is mostly identical to that of NIOTransport but tests
	 * JSON messages, headers, and delay emulation features. Need to test it with 
	 * the rest of GNS.
	 */
	public static void main(String[] args) {
		int msgNum=0;
		int port = 2000;
		int nNodes=100;
		SampleNodeConfig snc = new SampleNodeConfig(port);
		snc.localSetup(nNodes);
		JSONMessageWorker[] workers = new JSONMessageWorker[nNodes+1];
		for(int i=0; i<nNodes+1; i++) workers[i] = new JSONMessageWorker(new DefaultPacketDemultiplexer());
		GNSNIOTransport[] niots = new GNSNIOTransport[nNodes];
		
		try {
			int smallNNodes = 2;
			for(int i=0; i<smallNNodes; i++) {
				niots[i] = new GNSNIOTransport(i, snc, workers[i]);
				new Thread(niots[i]).start();
			}			
			
			/*************************************************************************/
			/* Test a few simple hellos. The sleep is there to test 
			 * that the successive writes do not "accidentally" benefit
			 * from concurrency, i.e., to check that OP_WRITE flags will
			 * be set correctly.
			 */
			niots[1].sendToIDActual(0, JSONify(msgNum++, "Hello from 1 to 0"));
			niots[0].sendToIDActual(1, JSONify(msgNum++, "Hello back from 0 to 1"));
			niots[0].sendToIDActual(1, JSONify(msgNum++, "Second hello back from 0 to 1"));
			try {Thread.sleep(1000);} catch(Exception e){e.printStackTrace();}
			niots[0].sendToIDActual(1, JSONify(msgNum++, "Third hello back from 0 to 1"));
			niots[1].sendToIDActual(0, JSONify(msgNum++, "Thank you for all the hellos back from 1 to 0"));
			/*************************************************************************/
			
			int seqTestNum=1;
			Thread.sleep(2000);
			System.out.println("\n\n\nBeginning test of " + seqTestNum + " random, sequential messages");
			Thread.sleep(1000);
			
			/*************************************************************************/
			//Create the remaining nodes up to nNodes
			for(int i=smallNNodes; i<nNodes; i++) {
				niots[i] = new GNSNIOTransport(i, snc, workers[i]);
				new Thread(niots[i]).start();
			}			
			
			// Test a random, sequential communication pattern
			for(int i=0; i<nNodes*seqTestNum;i++) {
				int k = (int)(Math.random()*nNodes);
				int j = (int)(Math.random()*nNodes);
				System.out.println("Message " + i + " with msgNum " + msgNum);
				niots[k].sendToIDActual(j, JSONify(msgNum++, "Hello from " + k + " to " + j));
			}

			int oneToOneTestNum=1;
			int concTestNum=25;
			/*************************************************************************/
			Thread.sleep(1000);
			System.out.println("\n\n\nBeginning test of " + oneToOneTestNum*nNodes + 
					" random, concurrent, 1-to-1 messages with emulated delays");
			Thread.sleep(1000);
			/*************************************************************************/
			// Test a random, concurrent communication pattern with emulated delays
			ScheduledExecutorService execpool = Executors.newScheduledThreadPool(5);
			class TX extends TimerTask {
				GNSNIOTransport sndr=null;
				private int rcvr=-1;
				int msgNum=-1;
				TX(int i, int id, GNSNIOTransport[] n, int m) {
					sndr = n[i];
					rcvr = id;
					msgNum = m;
				}
				TX(GNSNIOTransport niot, int id, int m) {
					sndr = niot;
					rcvr = id;
					msgNum = m;
				}
				public void run() {
					try {
						sndr.sendToIDActual(rcvr, JSONify(msgNum, "Hello from " + sndr.myID + " to " + rcvr));
					} catch(IOException e) {
						e.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
			GNSDelayEmulator.emulateDelays();

			GNSNIOTransport concurrentSender = new GNSNIOTransport(nNodes, snc, workers[nNodes]);
			new Thread(concurrentSender).start();
			for(int i=0; i<nNodes*oneToOneTestNum; i++) {
				TX task = new TX(concurrentSender, 0, msgNum++);
				System.out.println("Scheduling random message " + i + " with msgNum " + msgNum);
				execpool.schedule(task, 0, TimeUnit.MILLISECONDS);
			}
			
			/*************************************************************************/
			Thread.sleep(1000);
			System.out.println("\n\n\nBeginning test of " + concTestNum*nNodes + " random, concurrent, " +
			" any-to-any messages with emulated delays");
			Thread.sleep(1000);
			/*************************************************************************/			
			
			for(int i=0; i<nNodes*concTestNum; i++) {
				int k = (int)(Math.random()*nNodes);
				int j = (int)(Math.random()*nNodes);
				//long millis = (long)(Math.random()*1000);
				TX task = new TX(k, j, niots, msgNum++);
				System.out.println("Scheduling random message " + i + " with msgNum " + msgNum);
				execpool.schedule(task, 0, TimeUnit.MILLISECONDS);
			}

			/*************************************************************************/

			Thread.sleep(2000);
			System.out.println("\n\n\nPrinting overall stats:");
			System.out.println((new NIOInstrumenter()));	
			boolean pending=false;
			for(int i=0; i<nNodes; i++) {
				if(niots[i].getPendingSize() > 0) {
					System.out.println("Pending messages at node " + i + " : " + niots[i].getPendingSize());
					pending=true;
				}
			}
			assert(pending==false && NIOInstrumenter.getMissing()==0) : "Unsent pending messages in NIO";
			if(!pending && NIOInstrumenter.getMissing()==0) System.out.println("SUCCESS: no pending messages!");

	} catch (IOException e) {
		e.printStackTrace();
	} catch(Exception e) {
		e.printStackTrace();
	}
	}
}