package javakinber;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import java.text.*;

/**
 *
 * @author error
 */
public class KinberUtil {

    public static final int OS_UNKNOWN = 0;
    public static final int OS_WINDOWS = 1;
    public static final int OS_LINUX = 2;

    public static InetAddress getInetAddress(String address) {
        StringTokenizer st = new StringTokenizer(address, ".");
        if (st.countTokens() == 4) {
            try {
                byte[] bytes = new byte[4];
                for (int i = 0; i < 4; i++) {
                    bytes[i] = (byte) Integer.parseInt(st.nextToken());
                }
                InetAddress addr;
                try {
                    addr = InetAddress.getByAddress(bytes);
                } catch (Exception e) {
                    addr = null;
                }
                return addr;
            } catch (NumberFormatException num) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static String getIP(InetAddress addr) {
        if (addr == null)
            return "null";
        String ipStr = addr.toString();
        return ipStr.substring(ipStr.indexOf("/")+1);
    }

/*
    public static Image createImage(Object o, String path) {
        java.net.URL imgURL = o.getClass().getResource(path);
        if (imgURL != null) {
            return Toolkit.getDefaultToolkit().getImage(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
*/
    
    public static void log(char type, String realm, Object message, boolean newLine) {
        Date date = new Date();
        Format formatter = new SimpleDateFormat("HH:mm:ss ");
        String timeStr = formatter.format(date);
        String nl = "";
        if (newLine)
            nl = "\n";
        System.out.printf(timeStr + type + type + type + " %-7s %s%s", realm, message, nl);
    }

    public static void log(char type, String realm, Object message) {
        log(type, realm, message, true);
    }

    public static void log(char type, String realm, boolean newLine) {
        log(type, realm, "", newLine);
    }

    public static void log(char type, String realm) {
        log(type, realm, "", true);
    }

    public static Color getRGBColor(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }


    /** Alerter object used for alerts on windows */
    static WindowAlerter alerter;

    /** Force a window to flash if not focused */
    public static void AlertOnWindow(JFrame frm)
    {
        try
        {
            if (alerter == null)
            {
                alerter = new WindowAlerter();
            }
            alerter.flash(frm);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static class WindowAlerter
    {
        java.awt.Dialog d;

        WindowAlerter() {
            super();
        }

        /** It flashes the window's taskbar icon if the window is not focused.
         * The flashing "stops" when the window becomes focused.
         **/
        public void flash(final Window w) {
            d = new java.awt.Dialog(w);
            d.setUndecorated(true);
            d.setSize(0, 0);
            d.setModal(false);

            d.addWindowFocusListener(new WindowAdapter() {

                @Override
                public void windowGainedFocus(WindowEvent e) {
                    w.requestFocus();
                    d.setVisible(false);
                    super.windowGainedFocus(e);
                    //w.removeWindowFocusListener(w.getWindowFocusListeners()[0]);
                    //d.removeWindowFocusListener(d.getWindowFocusListeners()[0]);
                }
            });
            w.addWindowFocusListener(new WindowAdapter() {

                @Override
                public void windowGainedFocus(WindowEvent e)
                {
                    d.setVisible(false);
                    super.windowGainedFocus(e);
                    //w.removeWindowFocusListener(w.getWindowFocusListeners()[0]);
                    //d.removeWindowFocusListener(d.getWindowFocusListeners()[0]);
                }
            });

            if (!w.isFocused())
            {
                //if (d.isVisible())
                {
                    d.setVisible(false);
                }
                d.setLocation(0, 0);
                d.setLocationRelativeTo(w);
                d.setVisible(true);
            }
        }
    }

    public static boolean parseBoolean(String string) {
        return string != null && (string.matches("true") || string.matches("yes") ||
                string.equals("1"));
    }

    public static int getOperatingSystem() {
        /*
        for (Map.Entry<Object, Object> e : System.getProperties().entrySet()) {
            System.out.println(e);
        }
        */
        /*
        String os = System.getenv("OS");
        if (os != null && Pattern.compile(".*windows.*", Pattern.CASE_INSENSITIVE).matcher(os).matches()) {
            //KinberUtil.log('D', "HOME", "We're most likely running on Windows :)");
            return OS_WINDOWS;
        }
        */
        String osname = System.getProperty("os.name");
        //KinberUtil.log('D', "HOME", "osname: " + osname);
        if (osname == null) {
            return OS_UNKNOWN;
        } else if (Pattern.compile(".*linux.*", Pattern.CASE_INSENSITIVE).matcher(osname).matches()) {
            //KinberUtil.log('D', "HOME", "We're most likely running on Linux :)");
            return OS_LINUX;
        } else if (Pattern.compile(".*windows.*", Pattern.CASE_INSENSITIVE).matcher(osname).matches()) {
            //KinberUtil.log('D', "HOME", "We're most likely running on Windows :)");
            return OS_WINDOWS;
        }
        return OS_UNKNOWN;
    }

    public static String getUserHome() {
        String home = "";
        int os = getOperatingSystem();
        if (os == OS_WINDOWS) {
            home = System.getenv("APPDATA");
        } else if (os == OS_LINUX) {
            home = System.getenv("HOME");
        } else {
            home = System.getProperty("user.home");
        }
        if (home == null)
            KinberUtil.log('W', "HOME", "Cannot determine user's home");
        return home;
    }

    public static String getUserName() {
        String username = "";
        int os = getOperatingSystem();
        if (os == OS_WINDOWS) {
            username = System.getenv("USERNAME");
        } else if (os == OS_LINUX) {
            username = System.getenv("USER");
        }
        if (username == null)
            KinberUtil.log('W', "USER", "Cannot determine user name");
        return username;
    }

    public static String getFolder() {
        String home = KinberUtil.getUserHome();
        String folder = home + "/" + JavaKinber.JAVAKINBER_FOLDER;
        File iniFolder = new File(folder);
        if (!iniFolder.isDirectory()) {
            if (!iniFolder.mkdir()) {
                KinberUtil.log('W', "CONFIG", "Cannot create directory " + folder + ", reverting to home");
                folder = home;
            }
        }
        return folder;
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) { System.out.println("KinberUtil.sleep: Error sleeping: " + e); }
    }

    public static String getClipboardText() {
        // get the system clipboard
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        // get the contents on the clipboard in a transferable object
        Transferable clipboardContents = systemClipboard.getContents(null);
        // check if clipboard is empty
        if (clipboardContents == null) {
            return ("");
        } else {
            try {
                // see if DataFlavor of DataFlavor.stringFlavor is supported
                if (clipboardContents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    // return text content
                    String returnText = (String)clipboardContents.getTransferData(DataFlavor.stringFlavor);
                    return returnText;
                }
            } catch (UnsupportedFlavorException ufe) {
                System.out.println("getClipboardText: UnsupportedFlavorException");
            } catch (IOException ioe) {
                System.out.println("getClipboardText: IOException");
            }
        }
        return null;
    }    
}
