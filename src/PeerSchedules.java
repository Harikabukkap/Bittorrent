import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

public class PeerSchedules {

	// Declare the timers for the three scheduled tasks
	Timer timer1;
	Timer timer2;
	Timer timer3;

	// Declare class variables for peerProcess object 
	// and one each for the time intervals and number of preferred neighbours
	peerProcess pp;
	int p;
	int m;
	int k;

	//  class constructor
	public PeerSchedules (peerProcess pp) {

		this.pp = pp;

		Map<String, String> comProp = CommonPeerConfig.retrieveCommonConfig();

		// Retrieve the common peer configuration values for scheduling tasks from common config file
		int m = Integer.parseInt(comProp.get("OptimisticUnchokingInterval"));
		int k = Integer.parseInt(comProp.get("NumberOfPreferredNeighbors"));
		int p = Integer.parseInt(comProp.get("UnchokingInterval"));

		// schedule the determining k preferred neighbours task every p seconds
		timer1 = new Timer();
		timer1.scheduleAtFixedRate(new SelectKPreferredNeighbours(this.pp, k), 0, p*1000);

		// schedule the determining optimistically unchoked neighbour task every m seconds
		timer2 = new Timer();
		timer2.scheduleAtFixedRate(new SelectOptUnchokedNeighbour(this.pp), 0, m*1000);

		// schedule the determining shut down process of schedulers task every 4 seconds
		timer3 = new Timer();
		timer3.scheduleAtFixedRate(new DetermineShutDownProcess(this.pp), 0, 4*1000);
	}

	/**
	 * Determines k preferred neighbors every p seconds
	 */
	class SelectKPreferredNeighbours extends TimerTask {

		peerProcess pp;
		int k;

		public SelectKPreferredNeighbours (peerProcess pp, int k) {
			this.pp = pp;
			this.k=k;
		}    	

		@Override
		public void run() {

			try {

				System.out.println("Executing selection of K preferred neighbours task.");

				// Obtain the list of interested peers for the owner peer and select the k preferred neighbours from them
				List<PeerManager> listOfInterestedPeers = PeerManager.interestedPeers;

				// Sort the list of peers in the interestedList using the peer comparator class based on download rates
				Collections.sort(listOfInterestedPeers, new Comparator<PeerManager>(){
					public int compare(PeerManager pm1, PeerManager pm2) {
						return (new Long(pm1.getPeerDownloadRate())).compareTo(new Long(pm2.getPeerDownloadRate()));
					}
				});

				// if interested list is non empty, select k peers which have the highest download rate
				if (listOfInterestedPeers != null) {

					System.out.println("The size of list of interestedPeers is: " + listOfInterestedPeers.size());

					// Instantiate unchoke and choke peers synchronized lists
					pp.listOfUnchokedPeers = Collections.synchronizedList(new ArrayList<PeerManager>());
					pp.listOfchokedPeers = Collections.synchronizedList(new ArrayList<PeerManager>());

					int neighbours = 0;

					// Iterator through the interestedList of peers for owner peer
					for(PeerManager pm : listOfInterestedPeers) {

						// If the interested peer has been initialized
						if (pm.getIsPeerInitialized()) {

							// if <k preferred neighbours have been determined
							if (neighbours > k) {

								System.out.println("Start choking peer: " + pm.getPeerId());

								// add the selected interested peer to the chokeList of owner peer
								pp.listOfchokedPeers.add(pm);

								// if the selected interested peer is not previously choked
								if (!pm.isChoked()) {

									// set the choked value for it to be true
									pm.setChoked(true);

									// if the selected interested peer is not optimisticallyUnchokedPeer
									if (!pm.optimisticallyUnchokedPeer) {

										System.out.println("Send choke message to peer: " + pm.getPeerId());

										try {

											// send choke message to it
											pm.sendChokeMessage();
										} catch (IOException e) {

											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								}
							}

							// if k preferred neighbours have already been selected
							else {

								System.out.println("Start unchoking peer: " + pm.getPeerId());

								// Add the peer to the unchoke list of the owner peer
								pp.listOfUnchokedPeers.add(pm);

								// if the selected interested peer is choked previously
								if (pm.isChoked()) {

									// unchoke it
									pm.setChoked(false);

									// if the selected interested peer is not optimisticallyUnchokedPeer
									if (!pm.optimisticallyUnchokedPeer) {

										System.out.println("Send unchoke message to peer: " + pm.getPeerId());

										try {
											// send unchoke message to it
											pm.sendUnchokeMessage();

										} catch (IOException e) {

											// TODO Auto-generated catch block
											e.printStackTrace();
										} // now expect recieve message
									}
								}
								pp.logging( pm.getPeerId() + " has been selected as one of the k preferred neighbours for peer "+ PeerManager.ownerId);
							}
						}
						// decrement the number of neighbours
						neighbours--;
					}
				};
			} catch (Exception e) {

				System.out.println(e.getMessage());
			}
		}
	}

	/**
	 * Determine optimistically unchocked neighbour every m seconds.
	 */
	class SelectOptUnchokedNeighbour extends TimerTask {

		// Declare variable for previousOptUnchokedPeer
		PeerManager previousOptUnchokedPeer;

		peerProcess pp;

		public SelectOptUnchokedNeighbour(peerProcess pp) {
			this.pp = pp;
			this.previousOptUnchokedPeer = null; 
		}    	

		@Override
		public void run(){

			System.out.println("Entered selection of Optimistically Unchoked neighbours.");

			int chokeListSize = 0;
			if( pp.listOfchokedPeers!= null){
			// if choke list is empty
			if ((chokeListSize = pp.listOfchokedPeers.size()) == 0) {

				// if previousOptimisticallyUnchokedPeer is not null
				if (previousOptUnchokedPeer != null) {

					// set the value of the previous optimistically Unchoked peer to false
					previousOptUnchokedPeer.optimisticallyUnchokedPeer = false;

					// if previous optimistically unchoked peer has been choked
					if (previousOptUnchokedPeer.isChoked()) {

						System.out.println("Send choke message from optimistically unchoked neighbour.");

						try {

							// send choked message from it to the owner peer
							previousOptUnchokedPeer.sendChokeMessage();
						} catch (IOException e) {

							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				// set the previous Optimistically unchoked peer to null
				previousOptUnchokedPeer = null;


			}

			// if owner peer chokeList is non null
			else {

				System.out.println("Size of list of choked peers is :" + chokeListSize);

				// Randomly select a peer from the choked peers list of currently executing owner peer thread   
				PeerManager peerManager = pp.listOfchokedPeers.remove(ThreadLocalRandom.current().nextInt(0, chokeListSize));

				System.out.println("Randomly selected optimistic peer: "+ peerManager.getPeerId());

				// if a peer is obtained by random selection 
				if (peerManager != null) {

					// if it has not been selected previously
					if (peerManager != previousOptUnchokedPeer) {

						// Set the value of optimistically unchoked peer as true for the peer
						peerManager.optimisticallyUnchokedPeer = true;

						try {
							// send an unchoke message to the optimistically selected peer
							peerManager.sendUnchokeMessage();
						} catch (IOException e1) {

							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						// if previousOptimisticallyUnchokedPeer
						if (previousOptUnchokedPeer != null) {

							// set the value of the previous optimistically unchoked peer to false
							previousOptUnchokedPeer.optimisticallyUnchokedPeer = false;

							// if previous optimisticallyUnchokedPeer has been choked already
							if (previousOptUnchokedPeer.isChoked()) {

								try {

									System.out.println("Send choke message from the previous Optimistic neighbour");

									// send choke message from it to the owner peer
									previousOptUnchokedPeer.sendChokeMessage();

								} catch (IOException e) {

									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}
						}

						// reassign the previousOptimisticallyUnchokedPeer value to the current selected peer
						previousOptUnchokedPeer = peerManager;

						System.out.println( peerManager.getPeerId() + " has been selected as optimistic unchoked neighbour for peer "+ PeerManager.ownerId);
						pp.logging(peerManager.getPeerId() + " has been selected as optimistic unchoked neighbour for peer "+ PeerManager.ownerId);

					}
				}
			}
			}
		}
	}


	/**
	 * Determine when to shutdown...
	 */
	class DetermineShutDownProcess extends TimerTask {

		peerProcess pp;

		public DetermineShutDownProcess(peerProcess pp) {
			this.pp = pp;
		}    	

		@Override
		public void run() {

			// Obtain the bitField of owner peer
			byte[] ownerBitField = PeerManager.getOwnerBitField();
			System.out.println("Bit fields of owner peer: " + Arrays.toString(ownerBitField));

			// if peersList size of owner peer equals the total number of peers in peerInfo config file
			if (peerProcess.listOfPeers.size() == CommonPeerConfig.retrievePeerInfo().size()) {

				// set the shut down flag to true
				boolean shutDownFlag = true;

				// for every peerThread in the peersList
				for (PeerRunnable pt : peerProcess.listOfPeers) {

					// Obtain the bitField message of each peer
					byte[] peerBitFieldMsg = pt.retrievePeerConnected().getbitFieldMessageOfPeer();

					// if the owner and client peer do not have the same bitField messages
					if (Arrays.equals(peerBitFieldMsg, ownerBitField) == false) {

						// dont shut down yet
						shutDownFlag = false;
						break;
					}
				}

				// if all the peers in the owner peersList have same bitField message as the owner
				if (shutDownFlag) {

					Iterator<PeerRunnable> iter = peerProcess.listOfPeers.iterator();

					// for every peerThread in the peersList
					while(iter.hasNext()) {

						PeerRunnable pt = iter.next();

						// set the Peerthread toStop to true
						pt.toStop = true;

						try {

							// before exiting check if peerSocket is already closed, if no close it
							if(pt.peerSocket.isClosed());
							else
							{	
								System.out.println("***************************closing socket for peer:"+ pt.retrievePeerConnected().peerId+ " "+ PeerManager.ownerId);
								pt.peerSocket.close();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}

					}

					// Start cancelling the scheduled tasks
					System.out.println("Shut down the scheduled timer tasks..");

					// Terminate the timers, discarding any currently scheduled tasks.
					// Removes all cancelled tasks from this timer's task queue.
					timer1.cancel();
					timer1.purge();

					timer2.cancel();
					timer2.purge();

					timer3.cancel();
					timer3.purge();

				}
			}
		}
	}
}