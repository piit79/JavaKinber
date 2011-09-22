/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package javakinber;

import java.util.*;
import java.util.regex.*;
import java.text.*;

/**
 *
 * @author error
 */
public class KinberHistoryItem {

    private String contactName;
    private int direction;
    private Date date;
    private String message;

    // FORMAT: <message type="sent" date="2011-02-17 11:39:57" contact="Petr">message<br/>second line</message>
    private static Pattern logPattern = Pattern.compile("<message type=\"(recd|sent)\" date=\"([^\"]+)\" contact=\"([^\"]+)\">(.+)</message>$");

    private static DateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public KinberHistoryItem() {
        super();
    }

    public KinberHistoryItem(String aContactName, int aDirection, Date aDate, String aMessage) {
        this();
        contactName = aContactName;
        direction = aDirection;
        date = aDate;
        message = aMessage;
    }

    public KinberHistoryItem(String logLine) throws Exception {
        Matcher matcher = logPattern.matcher(logLine);
        if (matcher.find()) {
            String dirStr = matcher.group(1);
            String dateStr = matcher.group(2);

            date = null;
            try {
                date = timestampFormatter.parse(dateStr);
            } catch (ParseException e) {
                KinberUtil.log('W', "LOG", "Cannot parse date '" + dateStr + "': " + e);
            }
            contactName = matcher.group(3);
            direction = KinberHistory.getDirectionCode(dirStr);
            message = matcher.group(4);
            message = unEscape(message);
        } else {
            throw new Exception("Cannot parse log line: " + logLine);
        }
    }

    public String getContactName() {
        return contactName;
    }

    public int getDirection() {
        return direction;
    }

    public Date getDate() {
        return date;
    }

    public String getMessage() {
        return message;
    }

    public String getDirectionCode() {
        return KinberHistory.getDirectionStr(direction);
    }

    public static String escape(String message) {
        String msg = message.replaceAll("\n", "<br/>");
        return msg;
    }

    public static String unEscape(String message) {
        String msg = message.replaceAll("<br/>", "\n");
        return msg;
    }

    public String toLogString() {
        String messageStr = escape(message);
        String dateStr = timestampFormatter.format(date);
        String str = "<message type=\"" + getDirectionCode() + "\" date=\"" + dateStr + "\" contact=\"" + contactName + "\">" + messageStr + "</message>";
        return str;
    }

}
