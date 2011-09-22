package javakinber;

import java.io.*;
import java.util.*;

/**
 *
 * @author error
 */
public class KinberHistory {

    private String logDirectory;
    private Map<String, PrintWriter> streams;

    public static final int MESSAGE_RECEIVED = 0;
    public static final int MESSAGE_SENT = 1;

    private static final String[] messageDirCodes = { "recd", "sent" };

    public KinberHistory(String logDir) {
        super();
        File f = new File(logDir);
        // create the directory if it doesn't exist
        if (!f.isDirectory()) {
            if (!f.mkdirs()) {
                KinberUtil.log('W', "LOG", "Unable to create log directory " + logDir);
            }
        }
        logDirectory = logDir;
        streams = new HashMap<String, PrintWriter>();
    }

    private String getLogFilePath(String contactName) {
        String path = logDirectory + "/" + contactName + ".log";
        return path;
    }

    private PrintWriter openFile(String contactName) {
        PrintWriter pw = streams.get(contactName);
        if (pw == null) {
            String path = getLogFilePath(contactName);
            try {
                pw = new PrintWriter(new FileWriter(path, true), true);
            } catch (IOException e) {
                KinberUtil.log('W', "LOG", "Cannot open log file " + path + ": " + e);
            }
            if (pw != null) {
                streams.put(contactName, pw);
            }
        }
        return pw;
    }

    public void log(String contactName, final KinberHistoryItem item) {
        String str = item.toLogString();
        PrintWriter pw = openFile(contactName);
        pw.println(str);
        pw.flush();
    }

    public List<KinberHistoryItem> getHistory(String contactName) {
        List<KinberHistoryItem> history = new ArrayList<KinberHistoryItem>();
        String path = getLogFilePath(contactName);
        KinberHistoryItem item = null;
        try {
            BufferedReader log = new BufferedReader(new FileReader(path));
            String line;
            while ((line = log.readLine()) != null) {
//                KinberUtil.log('D', "LOG", "Parsing line: " + line);
                try {
                    item = new KinberHistoryItem(line);
                } catch (Exception e) {
                    KinberUtil.log('W', "LOG", "Error creating history item: " + e);
                }
                if (item == null)
                    continue;
                if (!history.add(item)) {
                    KinberUtil.log('W', "LOG", "Error adding history item to list!");
                }
            }
            log.close();
        } catch (IOException e) {
            KinberUtil.log('W', "LOG", "Cannot read contact log " + path + ": " + e);
        }
        return history;
    }

    public static String getDirectionStr(int aDirection) {
        return messageDirCodes[aDirection];
    }

    public static int getDirectionCode(String aDirectionStr) {
        for (int i = 0; i < messageDirCodes.length; i++) {
            if (messageDirCodes[i].equals(aDirectionStr))
                return i;
        }
        return -1;
    }

}
