import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/*
 * Class for reading the Common and peerInfo configuration files.
 */
public class CommonPeerConfig {

	// Variable to store peerInfo configuration file name.
	private final static String peerInfoFile = "PeerInfo.cfg";

	// Variable to store Common configuration file name.
	private final static String commonConfigFile = "Common.cfg";

	// Map to maintain peerInfo configuration file details.
	private static HashMap<Integer, String> peerInfo = new HashMap<Integer, String>();;

	// Map to maintain Common configuration file details. 
	private static HashMap<String, String> commonConfig = new HashMap<String, String>();

	// Obtain the BufferedReader for the commonConfigFile.
	private static BufferedReader in;
	
	static {
		
		try {
			in = new BufferedReader(new FileReader(commonConfigFile));

			// Read the commonConfigFile lines using the BufferedReader into the commonConfig map.
			String st = null;
			try {
				while ((st = in.readLine()) != null) {
					String[] commonConfigTokens = st.split("\\s+");
					commonConfig.put(commonConfigTokens[0], commonConfigTokens[1]);
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		// Obtain the BufferedReader for the peerInfoFile.
		try {
			
			in = new BufferedReader(new FileReader(peerInfoFile));
			

			String str = null;
			// Read the peerInfoFile lines using the BufferedReader into the peerInfo map.
			try {
				while ((str = in.readLine()) != null) {
					String[] peerInfoTokens = str.split("\\s+");
					peerInfo.put(Integer.parseInt(peerInfoTokens[0]), str);
				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}

	
	public static HashMap<String, String> retrieveCommonConfig(){
		return commonConfig;
	}

	public static HashMap<Integer, String> retrievePeerInfo(){
		return peerInfo;
	}

}