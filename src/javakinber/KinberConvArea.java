package javakinber;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;

/**
 *
 * @author error
 */
public class KinberConvArea extends JScrollPane {

    private JTextPane convPane;
    private KinberContact myContact;
    private KinberCopyPasteMenu copyPasteMenu;
    private StyledDocument doc;
    private int paneWidth;
    private Date lastMsgDate;
    private int lastMsgDir;

    public static final int MAX_CONV_TIME = 15 * 60 * 1000;

    private static Format timeFormatter = new SimpleDateFormat("HH:mm");
    private static Format dateFormatter = new SimpleDateFormat("dd/MM/yyyy");

    private static final String strSent = KinberHistory.getDirectionStr(KinberHistory.MESSAGE_SENT);
    private static final String strRecd = KinberHistory.getDirectionStr(KinberHistory.MESSAGE_RECEIVED);

    public KinberConvArea() {
        super();

        convPane = new JTextPane();
        setViewportView(convPane);

        convPane.setEditable(false);
        convPane.setFont(new java.awt.Font("Tahoma", 0, 12));

        doc = (StyledDocument)convPane.getDocument();
        paneWidth = 0;
        lastMsgDate = null;
        lastMsgDir = -1;

        // Create the styles
        Style styleMsgOut = doc.addStyle("Msg-" + strSent, null);
        StyleConstants.setForeground(styleMsgOut, KinberUtil.getRGBColor(51, 97, 147));

        Style styleNickOut = doc.addStyle("Nick-" + strSent, styleMsgOut);
        StyleConstants.setBold(styleNickOut, true);

        Style styleMsgIn = doc.addStyle("Msg-" + strRecd, null);
        StyleConstants.setForeground(styleMsgIn, KinberUtil.getRGBColor(251, 125, 0));

        Style styleNickIn = doc.addStyle("Nick-" + strRecd, styleMsgIn);
        StyleConstants.setBold(styleNickIn, true);

        copyPasteMenu = new KinberCopyPasteMenu(convPane, KinberCopyPasteMenu.COPY);
        
        convPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evt) {
                if (convPane.getWidth() > 0 && paneWidth != convPane.getWidth()) {
                    //KinberUtil.log('-', "CONVP", "Resize");
                    paneWidth = convPane.getWidth();
                    updateTabs();
                    // FIXME: How to set text focus?
                    //setTextFocus();
                }
            }
        });
    }

    public KinberConvArea(KinberContact aMyContact) {
        this();
        myContact = aMyContact;
    }

    public void updateTabs() {
        TabStop[] tstops = { new TabStop(convPane.getWidth()-8, TabStop.ALIGN_RIGHT, TabStop.LEAD_NONE) };
        TabSet tabset = new TabSet(tstops);
        Style style = convPane.getLogicalStyle();
        StyleConstants.setTabSet(style, tabset);
        convPane.setLogicalStyle(style);
    }

    private void scrollToBottom() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
           public void run() {
               try {
                   int endPosition = convPane.getDocument().getLength();
                   Rectangle bottom = convPane.modelToView(endPosition);
                   convPane.scrollRectToVisible(bottom);
               }
               catch (BadLocationException e) {
                   KinberUtil.log('W', "CONV", "Could not scroll to bottom: " + e);
               }
           }
        });
    }

    public void displayMessage(KinberHistoryItem item) {
        //Determine whether the scrollbar is currently at the very bottom position.
        JScrollBar vbar = getVerticalScrollBar();
        boolean autoScroll = (vbar.getValue() == (vbar.getMaximum() - vbar.getVisibleAmount()));

        try {
            if (doc.getLength() > 0) {
                doc.insertString(doc.getLength(), "\n", null);
            }
        } catch (Exception e) { System.out.println("Error inserting text (1): " + e); }

        String dirStr = item.getDirectionCode();
        Date date = item.getDate();
        String timeStr = timeFormatter.format(date);
        String dateStr = "";
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date);
        cal1.set(Calendar.HOUR, 0);
        cal1.set(Calendar.MINUTE, 0);
        cal1.set(Calendar.SECOND, 0);
        cal1.set(Calendar.MILLISECOND, 0);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(Calendar.HOUR, 0);
        cal2.set(Calendar.MINUTE, 0);
        cal2.set(Calendar.SECOND, 0);
        cal2.set(Calendar.MILLISECOND, 0);
        if (cal1.equals(cal2)) {
            dateStr = "Today";
        } else {
            cal2.roll(Calendar.DAY_OF_MONTH, -1);
            if (cal1.equals(cal2)) {
                dateStr = "Yesterday";
            } else {
                dateStr = dateFormatter.format(date);
            }
        }

        // if it's a message in different direction or different conversation
        // print the nickname and date
        if ((item.getDirection() != lastMsgDir) || (lastMsgDate == null) || ((date.getTime()-lastMsgDate.getTime()) > MAX_CONV_TIME)) {
            try {
                doc.insertString(doc.getLength(), item.getContactName() + ":", doc.getStyle("Nick-" + dirStr));
            } catch (Exception e) { System.out.println("Error inserting text (2): " + e); }
            try {
                doc.insertString(doc.getLength(), "\t" + dateStr + "\n", doc.getStyle("Msg-" + dirStr));
            } catch (Exception e) { System.out.println("Error inserting text (3): " + e); }
        }

        String message = item.getMessage();
        while (Character.isSpaceChar(message.charAt(message.length()-1))) {
            message = message.substring(0, message.length()-1);
        }
        if (message.indexOf('\n') > -1) {
            message = message.replaceFirst("\n", "\t" + timeStr + "\n");
        } else {
            message = message + "\t" + timeStr;
        }

        try {
            doc.insertString(doc.getLength(), message, doc.getStyle("Msg-" + dirStr));
        } catch (Exception e) { System.out.println("Error inserting text (4): " + e); }

        lastMsgDate = date;
        lastMsgDir = item.getDirection();

        if (autoScroll) {
            scrollToBottom();
        }
    }

}
