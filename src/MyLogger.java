import java.util.Date;
import java.util.logging.*;
import java.io.*;
import java.text.SimpleDateFormat;

class LogFormatter extends Formatter {
	@Override
	public String format(LogRecord record) {
		StringBuffer strBuffer = new StringBuffer(1000);
		SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
		Date resultdate = new Date(record.getMillis());
		strBuffer.append(date_format.format(resultdate));
		strBuffer.append(" ");
		strBuffer.append(record.getMessage());
		strBuffer.append("\n");
		String str = strBuffer.toString();
		return str;
	}
}

public class MyLogger {
	private static  FileHandler logFileHandler;
	private static  LogFormatter logTextHandler;
	private static  Logger messageLog;

	static public void setup() throws IOException {

		messageLog = Logger.getLogger(MyLogger.class.getName());
		messageLog.setUseParentHandlers(false); // prevent log from being sent to its parent
		Handler[] messageHandle = messageLog.getHandlers();
		if (messageHandle.length > 0 && messageHandle != null && messageHandle[0] instanceof ConsoleHandler) {
			messageLog.removeHandler(messageHandle[0]);
		}

		messageLog.setLevel(Level.INFO);
		String logDirName = "peer_" + PeerManager.ownerId;
        /*File newDir = new File(logDirName);
        if(!newDir.isDirectory()){
            newDir.mkdir(); //create directory if doesn't exist
        }*/
        logTextHandler = new LogFormatter();
        //String logFileName =  newDir.getPath() + File.separator + "log_peer_"+ PeerManager.ownerId + ".log";
        String logFileName =  "log_peer_"+ PeerManager.ownerId + ".log";
        logFileHandler = new FileHandler(logFileName); //create new log file
		logFileHandler.setFormatter(logTextHandler);
		messageLog.addHandler(logFileHandler);
	}

	public static Logger loggerInstance() {
		return messageLog;
	}
}