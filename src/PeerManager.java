import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/*
 * Class to encapsulate the Peer managing functionality.
 */
public class PeerManager {


	// static structures and declarations
	public static final byte[] HANDSHAKE_HEADER = "P2PFILESHARINGPROJ".getBytes();
	public static final byte[] ZERO_BITS = {0,0,0,0,0,0,0,0,0,0};

	public enum MessageTypes{
		CHOKE((byte)0), 
		UNCHOKE((byte)1), 
		INTERESTED((byte)2), 
		NOT_INTERESTED((byte)3), 
		HAVE((byte)4), 
		BITFIELD((byte)5), 
		REQUEST((byte)6), 
		PIECE((byte)7);

		byte messageValue = -1;

		private MessageTypes(byte b){
			this.messageValue = b;
		}
	}

    public static MessageTypes getMsgType(byte[] msgStat) {

        String s = Arrays.toString(msgStat);
        
        for (MessageTypes actMsgType : MessageTypes.values()) {
        
            if (actMsgType.messageValue == msgStat[4]) {
                return actMsgType;
            }
        }
        return null;
    }
	public static byte[] getHandShakeMessage(int toPeerId) throws IOException {
		return ByteArrayManipulation.mergeByteArrays(ByteArrayManipulation.mergeByteArrays(
				HANDSHAKE_HEADER, ZERO_BITS), ByteArrayManipulation.intToByteArray(toPeerId));
	}

	public static byte[] getOriginalMessage(String payload, MessageTypes msgType) throws IOException {

		int l = payload.getBytes().length;
		byte[] msgL = ByteArrayManipulation.intToByteArray(l + 2 -1); // plus one for message type
		return ByteArrayManipulation.mergeByteArrays(msgL,
				ByteArrayManipulation.mergeByteArray(msgType.messageValue, payload.getBytes()));
	}

	public static byte[] getOriginalMessage(MessageTypes msgType) throws IOException {

		byte[] msgL = ByteArrayManipulation.intToByteArray(1+1-1); // plus one for message type
		return ByteArrayManipulation.mergeByte(msgL, msgType.messageValue);
	}

	public static byte[] getOriginalMessage(byte[] payload, MessageTypes msgType) throws IOException {

		byte[] msgL = ByteArrayManipulation.intToByteArray(payload.length + 1 -1 + 1); // plus one for message type
		return ByteArrayManipulation.mergeByteArrays(ByteArrayManipulation.mergeByte(msgL, msgType.messageValue), payload);
	}

	public static byte[] readOriginalMessage(InputStream in, MessageTypes bitfield) {

		byte[] lengthByte = new byte[4];
		int read = 0-1;
		byte[] data = null;

		try {

			read = in.read(lengthByte);

			if (read != 4) {
				System.out.println("Incorrent message length.");
			}

			int dataLength = ByteArrayManipulation.byteArrayToInt(lengthByte);

			//read msg type
			byte[] msgType = new byte[1];

			in.read(msgType);
			//System.out.println("hey:"+msgType[0]);
			if (msgType[0] != bitfield.messageValue) {



				System.out.println("Incorrect message type sent"+ Arrays.toString(msgType) + " "+  PeerManager.ownerId+ " "+ bitfield.messageValue);


			} 

			else {


				int actualDataLength = dataLength - 1;
				data = new byte[actualDataLength];
				//System.out.println(in + " "+ Arrays.toString(data) + " " + actualDataLength);
				data = ByteArrayManipulation.readBytes(in, data, actualDataLength);
			}

		} catch (IOException e) {

			System.out.println("Could not read length of actual message");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println("returning in readoriginal");
		return data;
	}

	// Initialize optimisticallyUnchokedPeer with false. 
	// Initialize the client Peer value with false.

	public boolean optimisticallyUnchokedPeer = false;
	public boolean clientValue = false;


	// Initialize ownerId to zero.
	public static int ownerId = 0;

	// Map to maintain list of Peers with successful handshakes. 
	private static Map<Integer, Boolean> handshakeSucess = new HashMap<Integer, Boolean>();

	// Variable for peer id.
	// Variable to store the index of the requested piece of file.

	public int peerId;
	public int indexOfRequestedPiece;


	// Initialize the downloadRate value of peer to zero.
	public long downloadRate = 0;

	// Flag to indicate if the Peer is initialized.
	public Boolean isPeerInitialized = false;

	

	// Socket variables
	private Socket socket = null;
	private OutputStream output = null;
	private InputStream input = null;

	// Array of bytes of the file being shared.
	// Array of bytes to store the Bit fields of owner peer.
	// Array of bytes to store the Bit fields of Requested byte.
	
	private final static byte[] fileBitField;
	private final static byte[] ownerBitField;
	private final static byte[] requestedBitField;

	// Array of bytes to store Bit field message of the peer.
	private byte[] bitFieldMesssageOfPeer = null;

	// Array of bytes to store data shared between peers.
	public static byte[] sharedDataArr = null;

	// Get the global logger.
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	// List to maintain the interested peers.
	public static List<PeerManager> interestedPeers = Collections.synchronizedList(new ArrayList<PeerManager>());

	// Map to maintain the not interested peers.
	public static Map<Integer, PeerManager> notinterestedPeers = Collections.synchronizedMap(new HashMap<Integer,PeerManager>());
	public static Map<Integer, PeerManager> chokedPeers = Collections.synchronizedMap(new HashMap<Integer, PeerManager>());
	public static Map<Integer, PeerManager> unchokedPeers = Collections.synchronizedMap(new HashMap<Integer, PeerManager>());

	// Map to maintain peer ID and requestTime pair values.
	// Map to maintain peer ID and downloadTime pair values.
	public static Map<Integer, Long> peerRequestTime = Collections.synchronizedMap(new HashMap<Integer, Long>());
	public static Map<Integer, Long> peerDownloadTime = Collections.synchronizedMap(new HashMap<Integer, Long>());

	// Declare a reentrant lock -- see this 
	private final ReentrantLock lock;


	public synchronized long getPeerDownloadRate() {
		return downloadRate;
	}

	public synchronized void setPeerDownloadRate(long rate) {
		downloadRate = -rate;
	}

	public void setClientValue(boolean value) {
		clientValue = value;
	}


	public synchronized  int getindexOfRequestedPiece() {
		return indexOfRequestedPiece;
	}

	public synchronized  void setindexOfRequestedPiece(int index) {
		indexOfRequestedPiece = index;
	}


	public void setPeerId(int id) {
		this.peerId = id;
	}

	public synchronized int getPeerId() {
		return peerId;
	}

	public static byte[] getFileBitfield() {
		return fileBitField;
	}

	public static synchronized byte[] getOwnerBitField() {
		return ownerBitField;
	}

	public boolean getIsPeerInitialized(){
		return isPeerInitialized;
	}

	public synchronized  void setisPeerInitialized(boolean status){
		isPeerInitialized = true;
		notify();
	}

	public synchronized void waitToInitialize() {
		if (isPeerInitialized == false){
			do {
				try {
					System.out.println("Waiting to be initialized...");
					wait(1000);
				} catch (Exception ex) {
					System.out.println("Exception encountered while waiting to be initialized." + ex.getMessage());
				}
			} while(isPeerInitialized == false);
		}
	}



	public static synchronized byte[] getRequestedBitField() {
		return requestedBitField;
	}

	public static synchronized void setOwnerBitFieldIndex(int index, int i) {
		ownerBitField[index] |= (1 << (7 - i));
	}

	public static synchronized void setIndexOfPieceRequested(int index, int indexFromRight) {
		requestedBitField[index] |= (1 << indexFromRight);
	}

	public static synchronized  void resetIndexOfPieceRequested(int index, int indexFromLeft) {
		requestedBitField[index] &= ~(1 << (7 - indexFromLeft));
	}

	public synchronized byte[]  getbitFieldMessageOfPeer(){
		return bitFieldMesssageOfPeer;
	}

	// One time execution block at the beginning for setting the bit fields of owner peer.
	static {

		// Obtain the fileName, fileSize and pieceSize from commonConfig file.
		String fileName1 = CommonPeerConfig.retrieveCommonConfig().get("FileName");
		int fileSize = Integer.parseInt(CommonPeerConfig.retrieveCommonConfig().get("FileSize"));
		int pieceSize = Integer.parseInt(CommonPeerConfig.retrieveCommonConfig().get("PieceSize"));
		
		// Calculate numOfPieces from fileSize and pieceSize.
		long numOfPieces = 0;
		if (fileSize % pieceSize == 0) {
			numOfPieces = fileSize / pieceSize;
		} else {
			numOfPieces = fileSize / pieceSize + 1;
		}


		// Initialize owner, file and requested bit field byte array sizes as numOfPieces/8.
		ownerBitField = new byte[(int) Math.ceil(numOfPieces / 8.0f)];
		fileBitField = new byte[(int) Math.ceil(numOfPieces / 8.0f)];
		requestedBitField = new byte[(int) Math.ceil(numOfPieces / 8.0f)];

		// Initialize sharedDataArr of bytes with size as FileSize.
		sharedDataArr = new byte[Integer.parseInt(CommonPeerConfig.retrieveCommonConfig().get("FileSize"))];
		
		/*File newDir = new File("peer_"+PeerManager.ownerId);
        if(!newDir.isDirectory()){
            newDir.mkdir(); //create directory if doesn't exist
        }*/
		
		File dir = new File(".");
	    File[] matchingFiles = dir.listFiles(new FileFilter() {
	        @Override
	        public boolean accept(File pathname) {
	            String regex = "peer_\\d+";
	            Pattern p = Pattern.compile(regex);
	            Matcher m = p.matcher(pathname.getName());
	            return m.matches();
	        }
	    });
	    
	    String folder = null;
	    for(File f : matchingFiles) {
	    	File f2 = new File("."+File.separator+ f+ File.separator+fileName1);
	    	System.out.println(fileName1);
	    	if (f2.exists()) {
	    		folder = f.getName();
	    	}
	    }
        
	    System.out.println(folder);
	    String fileName = folder + File.separator + fileName1;
	    
		// Create a file
		File f = new File(fileName);

		// if the mentioned file exists fill the ownerBitField array of bytes
		if (f.exists()) 
		{
			// error if file size mismatch
			if (f.length() != fileSize) {

				System.out.println("FIle seize dont match .. check please" + fileSize + "make it this ---> " + f.length());
				System.exit(-1);
			} 
			else {

				FileInputStream fileInputStream = null;

				try {

					// read from the file into the sharedDataArr of bytes
					fileInputStream = new FileInputStream(f);
					fileInputStream.read(sharedDataArr);
					fileInputStream.close();

				} catch (Exception e) {
					e.printStackTrace();
				}

			}

			// if numOfPieces exactly divisible by 8, set all bytes
			if ((numOfPieces % 8.0) != 0){
		// get the number of bits set in the last byte from the remainder of numOfPieces % 8
				int numLastByteBitsSet = (int) numOfPieces % 8;

				// set all bytes to one.
				Arrays.fill(ownerBitField, (byte) 255); 

				// reset spare bits of last byte to zero.
				ownerBitField[ownerBitField.length - 1] = 0; 

				// Set only the numLastByteBitsSet bits of the last byte
				for (;numLastByteBitsSet != 0;numLastByteBitsSet--) {
					ownerBitField[ownerBitField.length - 1] |= (1 << (8 - numLastByteBitsSet));
				}

			} 
			else {

			
				Arrays.fill(ownerBitField, (byte) 255);
			}
		}

		// if file does not exist, fill the fileBitField array of bytes
		else {

			// if numOfPieces exactly divisible by 8, set all bytes 
			if ((numOfPieces % 8.0) != 0) {

				// get the number of bits set in the last byte from the remainder of numOfPieces % 8
				int numLastByteBitsSet = (int) numOfPieces % 8;

				// set all bytes to one.
				Arrays.fill(fileBitField, (byte) 255); 

				// reset spare bits of last byte to zero.
				fileBitField[fileBitField.length - 1] = 0; 

				// Set only the numLastByteBitsSet bits of the last byte
				for (;numLastByteBitsSet != 0;numLastByteBitsSet--) {
					fileBitField[fileBitField.length - 1] |= (1 << (8 - numLastByteBitsSet));
				
				}

			} 

			else {


				Arrays.fill(fileBitField, (byte) 255);

			}
		}

	}

	// PeerManager constructor.
	public PeerManager(Socket psocket) {

		this.socket = psocket;
		// -- see this
		this.lock = new ReentrantLock();
		try {
			// obtain output stream and input stream for passed peer socket
			output = new BufferedOutputStream(socket.getOutputStream());
			input = new BufferedInputStream(socket.getInputStream());
		} catch (Exception ex) {
			System.out.println("Socket Exception thrown."+ex.getMessage());
		}



	}
	
	public void closeSocket() {
        try {
        	System.out.println("************closing sock for peer "+ PeerManager.ownerId);
            socket.close();
        } catch (IOException e) {
            System.out.println("Exeception encountered while closing the socket."+e.getMessage());
        }
    }

    @Override
    public void finalize() {
        this.closeSocket();
    }

	//public synchronized void sndHandshakeMessage() throws IOException { -- see this
	public synchronized void sndHandshakeMessage() throws  IOException {
			// Send the handshake to the peer.
			synchronized (handshakeSucess) {

				// Create the handshake message by concatenating the handshake header, zero bits and the owner peerId 
				// (retrieved from the commonConfig hashmap written in peerProcess).
				byte[] concatenateByteArrays = ByteArrayManipulation.mergeByteArrays(ByteArrayManipulation.mergeByteArrays(HANDSHAKE_HEADER, ZERO_BITS),
						String.valueOf(PeerManager.ownerId).getBytes());

				try {

					// write the owner's created handshake message in the BufferedOutputStream stream of the client socket
					output.write(concatenateByteArrays);
					output.flush();

					// if success, put the client peerId in the handshakeSuccess map of the owner peer.
					handshakeSucess.put(peerId, false);

				} catch (IOException e) {
					LOGGER.severe("Handshake sending failed." + e.getMessage());
				}

			}
	}

	public synchronized int rcvHandshakeMessage() {

		try {
			byte[] byteArr = new byte[32];

			
			// Read from the peer(should be client) socket's bufferedinputstream into the byteArr. 
			input.read(byteArr);

			// Obtain the peerId from the last four bytes of handshake message  
			byte[] reqByteArrRange = Arrays.copyOfRange(byteArr, 28, 32);
			Integer peerId = Integer.parseInt(new String(reqByteArrRange));

			// if a client peer
			if (clientValue) {

				// if the client peer exists in the handshakeSuccess map of owner peer and 
				// if this client's handshake has not been received yet
				if (handshakeSucess.containsKey(peerId) && handshakeSucess.get(peerId) == false) {

					// set the received handshake flag of client peer to true
					handshakeSucess.put(peerId, true);
					System.out.println("Valid peer id:" + peerId);
				} 

				else {
					System.out.println("Invalid peer id:"+peerId);
				}

			}

			// return the client's peerId which sent a handshake only in response to a handshake sent by owner peer.   
			return peerId;

		} catch (IOException e) {
			e.printStackTrace();
		}

		return -1;
	}

	// Sends a Message of type BitField
	public synchronized void sndBitFieldMessageToPeer() {

		try {

			byte[] ownerBitField = getOwnerBitField();

			// create the actual message by merging message length, type and payload
			byte[] actualMessage = getOriginalMessage(ownerBitField, MessageTypes.BITFIELD);

			// write the actual message in the peer socket's output stream 
			output.write(actualMessage);
			System.out.println("see port for send bit:"+this.socket.getPort());
			System.out.println("see2:"+Arrays.toString(actualMessage));
			output.flush();

		} catch (IOException e) {
			System.out.println("Sending of bit field message failed."+e.getMessage());
		}

	}

	public synchronized void readBitFieldMessageOfPeer() {

		// read the array of bytes from client peer socket bufferedinputstream into bitFieldMessageOfPeer byte array
		System.out.println("see port for read bit:"+this.socket.getPort());
		bitFieldMesssageOfPeer = readOriginalMessage(input , MessageTypes.BITFIELD);
		System.out.println("returning"+this.socket.getPort()+ " "+ this.getPeerId());
	}



	public synchronized boolean isInterested() {

		int i = 0;
		// Obtain the ownerBitField byte array
		byte[] bitField = getOwnerBitField();
		// byte array to store result of comparing owner peer and client peer bit field message values 
		byte[] bitor = new byte[bitField.length];
		
		// Perform a bitwise OR between ownerBitField and bitFieldMessageOfPeer and store result in output
		for (byte byt : bitField) {
			bitor[i] = (byte) (byt ^ bitFieldMesssageOfPeer[i]);
			i++;
		}
		i = 0;
		// Compute bitwise AND between output and complement of ownerBitField to see if any bits exist in output which are not already in owner
		// resulting in output holding bits in client which are not in owner.
		// if resulting output is zero, implies client has no bits which are not already in owner.
		for (byte b : bitField) {
			bitor[i] = (byte) (bitor[i] & ~b);
			if (bitor[i] != 0) {
				System.out.println("returning true from isInteresed" + PeerManager.ownerId + " "+ this.getPeerId());
				return true;
			}
		}
		System.out.println("returning false from isInterested "+ Arrays.toString(bitor) + " "+ Arrays.toString(ownerBitField));
		return false;

	}

	// Sends a Message of type Interested
	public synchronized void sendInterestedMessage() throws IOException {

		// Obtains the concatenated msgL, messageValue of INTERESTED message 
		byte[] actualMessage = getOriginalMessage(MessageTypes.INTERESTED);

		try {

			// write the INTERESTED message into the client socket bufferedoutputstream
			output.write(actualMessage);
			output.flush();

		} catch (IOException e) {
			System.out.println("Sending of interested message failed: " + e.getMessage());
		}
	}

	// Sends a Message of type NotInterested
	public synchronized void sendNotInterestedMessage() throws IOException {

		// Obtains the concatenated msgL, messageValue of INTERESTED message
		byte[] originalMessage = getOriginalMessage(MessageTypes.NOT_INTERESTED);

		try {

			// write the NOT_INTERESTED message into the client socket bufferedoutputstream
			output.write(originalMessage);
			output.flush();

		} catch (IOException e) {
			System.out.println("Sending of not interested message failed: " + e.getMessage());
		}
	}

	// Still working on it.

	// update the bitField Message of peer
	public synchronized  void updateBitFieldMessageOfPeer(int indexOfPiece) {
		bitFieldMesssageOfPeer[indexOfPiece / 8] |= (1 << (7 - (indexOfPiece % 8)));
	}

	// Send have message
	public synchronized void sendHaveMessage(int pieceIndex) throws IOException {
		byte[] actualMessage = getOriginalMessage(
				ByteArrayManipulation.intToByteArray(pieceIndex),
				MessageTypes.HAVE);
		try {
			output.write(actualMessage);
			output.flush();

		} catch (IOException e) {
			System.out.println("io exception in reading " + e.getMessage());
		}
	}

	// Send choke message
	public synchronized  void sendChokeMessage() throws IOException {
		byte[] actualMessage = getOriginalMessage(MessageTypes.CHOKE);
		try {
			output.write(actualMessage);
			output.flush();

		} catch (IOException e) {
			System.out.println("io exception in reading " + e.getMessage());
			e.printStackTrace();
		}
	}

	// Send unchoke message
	public synchronized void sendUnchokeMessage() throws IOException {
		byte[] actualMessage = getOriginalMessage(MessageTypes.UNCHOKE);
		try {
			output.write(actualMessage);
			output.flush();

		} catch (IOException e) {
			System.out.println("io exception in reading " + e.getMessage());
			e.printStackTrace();
		}
	}

	private boolean choked = true;

	public synchronized void setChoked(boolean n) {
		choked = n;
	}

	public synchronized boolean isChoked() {
		return choked;
	}

	// Send request message
	public synchronized void sendRequestMessage(int indexOfPiece) throws IOException {

		// if the requested piece index is >=0
		if (indexOfPiece >= 0) {

			// obtain the byte array of requested pieceIndex
			byte[] pieceIndexByteArray = ByteArrayManipulation.intToByteArray(indexOfPiece);

			// obtain the request message by concatenating message length, message value and payload of request message
			byte[] originalMessage = getOriginalMessage(
					pieceIndexByteArray, MessageTypes.REQUEST);
			try {

				// write the original request message created into the peer's socket output buffer
				output.write(originalMessage);
				output.flush();

				// Add the peer id and its time of request in the peerRequestTime hashmap
				PeerManager.peerRequestTime.put(peerId, System.nanoTime());

			} catch (IOException e) {

				System.out.println("Exception encountered while writing in sendRequestMessage:" + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	// Send piece message
	public synchronized void sendPieceMessage(int pieceIndex) throws IOException {

		int pI = pieceIndex;

		int pieceSizeFromFile = Integer.parseInt(CommonPeerConfig.retrieveCommonConfig().get(
				"PieceSize"));

		int startIndex = pieceSizeFromFile * pieceIndex;

		int endIndex = startIndex + pieceSizeFromFile - 1;

		if (endIndex >= sharedDataArr.length) {
			endIndex = sharedDataArr.length - 1;
		}
		//special case
		//if pieceSize is greater than the entire file left

		byte[] data = new byte[endIndex - startIndex + 1 + 4]; // 4 is for pieceIndex

		// populate the piece index
		byte[] pieceIndexByteArray = ByteArrayManipulation.intToByteArray(pieceIndex);

		for (int i = 0; i < 4; i++) {
			data[i] = pieceIndexByteArray[i];
		}

		// populate the data
		for (int i = startIndex; i <= endIndex ; i++) {
			data[i - startIndex + 4] = sharedDataArr[i];
		}

		// obtain the piece message by concatenating message length, message value and payload 
		byte[] originalMessage = getOriginalMessage(data,
				MessageTypes.PIECE);
		try {
			System.out.println("The actual message size is " + originalMessage.length);
			// write the created piece message into the peer's socket output stream
			output.write(originalMessage);
			output.flush();
		} catch (IOException e) {
			System.out.println("io exception in reading " + e.getMessage());
			e.printStackTrace();
		}
	}

	// TODO: Test this function
	public synchronized int getNextBitFieldIndexToRequest() {

		/// request a piece owner peer doesn't have and did not request from other peers, 
		// select next piece to request index randomly
		byte[] reqstdUntilnow = getRequestedBitField();
		byte[] ntHavendNtReqst = new byte[bitFieldMesssageOfPeer.length]; // to store bytes that I don't have
		byte[] bitFieldReqAndHave = new byte[bitFieldMesssageOfPeer.length];
		byte[] ownerbitfield = getOwnerBitField();
		System.out.println("Arrays.toString(reqstdUntilnow) = " + Arrays.toString(reqstdUntilnow));

		for (int i = 0; i < reqstdUntilnow.length; i++) {
			bitFieldReqAndHave[i] = (byte) (reqstdUntilnow[i] & ownerbitfield[i]);
		}

		// determine bits I dont have.
		for (int i = 0; i < bitFieldReqAndHave.length; i++) {
			ntHavendNtReqst[i] = (byte) ((bitFieldReqAndHave[i] ^ bitFieldMesssageOfPeer[i]) & ~bitFieldReqAndHave[i]);
		}

		System.out.println("Arrays.toString(peerBitFieldMsg) = " + Arrays.toString(bitFieldMesssageOfPeer));
		System.out.println("Arrays.toString(getMyBitField()) = " + Arrays.toString(getOwnerBitField()));
		System.out.println("Arrays.toString(bitFieldReqAndHave) = " + Arrays.toString(bitFieldReqAndHave));
		System.out.println("Arrays.toString(ntHavendNtReqst) = " + Arrays.toString(ntHavendNtReqst));

		int count = 0;
		int pos = 0;
		for (int i = 0; i < ntHavendNtReqst.length; i++) {
			count = 8 * i;
			byte temp = ntHavendNtReqst[i];
			Byte b = new Byte(temp);

			pos = 0;
			while (temp != 0 && pos < 8) {
				if ((temp & (1 << pos)) != 0) {
					setIndexOfPieceRequested(i, pos);
					pos = 7 - pos;
					int index = count + pos;
					setindexOfRequestedPiece(index);
					// set the ith bit as 1
					return index;
				}
				++pos;
			}
		}

		System.out.println("Arrays.toString(myBitField) = " + Arrays.toString(getOwnerBitField()));
		return -1;
	}

}