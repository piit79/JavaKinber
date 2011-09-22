package javakinber;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import static java.lang.System.out;

/**
 *
 * @author error
 */
public class JavaKinber {

    private int status;
    private KinberContact me;
    private InetAddress destNetwork;
    private DatagramSocket txSocket;
    public KinberContactList contactList;
    private Thread jkReceiver;
    private KinberFrame kFrame;
    private javax.swing.Timer timer;
    private List<InetAddress> addresses;
    private KinberHistory jkHistory;

    public static final String NAME = "JavaKinber";
    public static final String VERSION = "0.9.9";

    public static final String JAVAKINBER_FOLDER = ".JavaKinber";
    public static final String JAVAKINBER_HISTORY = "logs";

    public static final int STATUS_NULL = -1;
    public static final int STATUS_ONLINE = 0;
    public static final int STATUS_AWAY = 1;
    public static final int STATUS_NA = 2;
    public static final int STATUS_OCCUPIED = 3;
    public static final int STATUS_DND = 4;
    public static final int STATUS_INVISIBLE = 5;
    public static final int STATUS_OFFLINE = 6;

    public static final String[] statusTexts = { "Online", "Away", "NA", "Occupied", "DND", "Invisible", "Offline" };

    private static final int RECEIVE_PORT = 1001;

    private static final String HEADER_MESSAGE = "[KINBERLINK:MSG]";
    private static final String HEADER_CALL = "[KINBERLINK:CALL]";
    private static final String HEADER_ANSWER = "[KINBERLINK:ANSWER]";
    private static final String HEADER_ACTIVATE = "[KINBERLINK:ACTIVATE]";

    private static final int TIMER_PERIOD = 60 * 1000;
    private static final int MAX_IDLE_TIME = 120 * 1000;
    private static final int MAX_CALLED_TIME = 10 * 1000;
    private static final int MAX_REFRESH_TIME = 15 * 60 * 1000;

    private static boolean callInProgress = false;

    public JavaKinber(KinberFrame aFrame, String myNickName, int bindPort) throws IOException {
        super();
        status = STATUS_NULL;
        me = new KinberContact(myNickName, null);
        destNetwork = getDestNetworkAddress();
        txSocket = new DatagramSocket();
        contactList = new KinberContactList();
        kFrame = aFrame;
        addresses = new ArrayList<InetAddress>();
        //refreshAddresses();
        jkHistory = new KinberHistory(KinberUtil.getFolder() + "/" + JAVAKINBER_HISTORY);
        jkReceiver = new KinberReceiver(this, bindPort);
        jkReceiver.start();
        timer = new javax.swing.Timer(TIMER_PERIOD, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                timerFired();
            }
        });
        timer.start();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int newStatus) {
        int oldStatus = getStatus();
        status = newStatus;
        if (newStatus == STATUS_OFFLINE) {
            kFrame.setStatusBarText(getNickName() + " (" + JavaKinber.statusTexts[status] + ")");
        } else if (oldStatus == STATUS_OFFLINE) {
            kFrame.setStatusBarText(getNickName() + " [" + getIP() + "]");
        }
        if (newStatus == STATUS_OFFLINE && oldStatus != STATUS_OFFLINE) {
            timer.stop();
            contactList.clear();
        }
        if (newStatus != STATUS_OFFLINE && (oldStatus == STATUS_OFFLINE || oldStatus == STATUS_NULL)) {
            refreshAddresses(true);
            if (!timer.isRunning())
                timer.start();
        }
    }

    public static int getStatusCode(String statusStr) {
        for (int i = 0; i < statusTexts.length; i++) {
            if (statusTexts[i].equals(statusStr))
                return i;
        }
        return STATUS_NULL;
    }

    public String getNickName() {
        return me.getNickName();
    }

    public void setNickName(String newNickname) {
        if (!newNickname.equals(me.getNickName())) {
            me.setNickName(newNickname);
            kFrame.setStatusBarText(getNickName() + " [" + getIP() + "]");
            sendNetworkCall();
        }
    }

    public InetAddress getAddress() {
        return me.getAddress();
    }

    public void setAddress(InetAddress newAddr) {
        KinberUtil.log('-', "IPADDR", "Setting address " + KinberUtil.getIP(newAddr));
        me.setAddress(newAddr);
        kFrame.setStatusBarText(getNickName() + " [" + getIP() + "]");
        kFrame.setTrayToolTip();
        sendNetworkCall();
    }

    public void setAddress(int index) {
        if (index > addresses.size()) {
            KinberUtil.log('W', "SETIP", "setAddress: index out of range! (" +
                    index + " > " + addresses.size() + ")");
            return;
        }
        setAddress(addresses.get(index));
    }

    public String getIP() {
        return me.getIP();
    }

    public KinberContact getMyContact() {
        return me;
    }

    public KinberHistory getKinberHistory() {
        return jkHistory;
    }

    public synchronized void sendCall(InetAddress addr) {
        // no use sending messages to self
        if (addr.equals(me.getAddress())) {
            return;
        }
        String messageStr = HEADER_CALL + me.getNickName() + "|" + me.getIP() + "|0";
        byte[] buf = messageStr.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, addr, RECEIVE_PORT);
        //KinberUtil.log('>', "CALL", KinberUtil.getIP(addr));
        try {
            txSocket.send(packet);
        } catch (Exception e) { System.out.println("!!! sendCall: Error sending packet: " + e); }
        KinberContact c = contactList.get(addr);
        if (c != null) {
            c.setCalled();
        }
    }

    public synchronized void sendCall(KinberContact contact) {
        sendCall(contact.getAddress());
    }

    public synchronized void sendCall(String ip) {
        InetAddress addr = KinberUtil.getInetAddress(ip);
        sendCall(addr);
    }

    public synchronized void sendNetworkCall() {
        if (getStatus() == STATUS_NULL || getStatus() == STATUS_OFFLINE)
            return;
        callInProgress = true;
        KinberUtil.log('-', "REFRSH");
        byte[] rawAddress = destNetwork.getAddress();
        InetAddress addr;
        for (int i = 1; i < 255; i++)
        {
            rawAddress[3] = (byte)i;
            addr = null;
            try {
                addr = InetAddress.getByAddress(rawAddress);
            } catch (Exception e) { System.out.println("!!! sendNetworkCall: Error creating address: " + e); }
            KinberContact c;
            if (addr != null) {
                sendCall(addr);
            }
        }
        KinberUtil.sleep(100);
        contactList.setRefreshed();
        contactList.updateList();
        callInProgress = false;
    }

    public boolean getCallInProgres() {
        return callInProgress;
    }

    public synchronized void refresh(boolean wholeNet) {
        if (wholeNet) {
            sendNetworkCall();
        } else {
            for (KinberContact c : contactList) {
                sendCall(c);
            }
        }
    }

    public synchronized void sendAnswer(KinberContact contact) {
        String messageStr = HEADER_ANSWER + me.getNickName() + "|" + me.getIP() + "|0";
        byte[] buf = messageStr.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, contact.getAddress(), RECEIVE_PORT);
        KinberUtil.log('>', "ANSWER", me.toStringV() + " -> " + contact.toStringV());
        try {
            txSocket.send(packet);
        } catch (Exception e) {
            System.out.println("!!! sendAnswer: Error sending packet: " + e);
        }
    }

    public boolean sendMessage(KinberContact contact, String message) {
        String messageStr = HEADER_MESSAGE + me.getNickName() + "|" + message.replaceAll("\n", "\r\n");
        byte[] buf = messageStr.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, contact.getAddress(), RECEIVE_PORT);
        jkHistory.log(contact.toString(), new KinberHistoryItem(me.getNickName(), KinberHistory.MESSAGE_SENT, new Date(), message));
        KinberUtil.log('>', "MESSAGE", contact.toStringV());
        try {
            txSocket.send(packet);
        } catch (Exception e) {
            System.out.println("sendMessage: Error sending packet: " + e);
            return false;
        }
        return true;
    }

    public static boolean sendActivate() {
        String messageStr = HEADER_ACTIVATE;
        byte[] buf = messageStr.getBytes();
        try {
            DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByAddress(new byte[] { (byte)127, (byte)0, (byte)0, (byte)1 }), RECEIVE_PORT);
            KinberUtil.log('>', "ACTIVATE");
            DatagramSocket txSocket = new DatagramSocket();
            txSocket.send(packet);
        } catch (Exception e) {
            System.out.println("!!! sendActivate: Error sending packet: " + e);
            return false;
        }
        return true;
    }

    public synchronized void handlePacket(DatagramPacket packet) {
        if (status == STATUS_OFFLINE)
            return;

        String msg = new String(Arrays.copyOfRange(packet.getData(),
                packet.getOffset(), packet.getLength()));
        InetAddress addr = packet.getAddress();
        //KinberUtil.log('<', "PACKET", "'" + msg + "' from " + KinberUtil.getIP(addr));

        if (msg.startsWith(HEADER_CALL)) {
            msg = msg.substring(HEADER_CALL.length());
            String[] parts = msg.split("\\|");
            contactList.add(new KinberContact(parts[0], addr), !getCallInProgres());
            KinberContact contact = contactList.get(addr);
            if (contact == null) {
                System.out.println("!!! handlePacket(CALL): contact doesn't exist! " +
                        parts[0] + " (" + parts[1] + ")");
                return;
            }
            KinberUtil.log('<', "CALL", contact.toStringV());
            sendAnswer(contact);
        } else if (msg.startsWith(HEADER_ANSWER)) {
            msg = msg.substring(HEADER_ANSWER.length());
            String[] parts = msg.split("\\|");
            KinberContact contact = new KinberContact(parts[0], addr);
            KinberUtil.log('<', "ANSWER", contact.toStringV());
            contactList.add(contact, !getCallInProgres());
        } else if (msg.startsWith(HEADER_MESSAGE)) {
            msg = msg.substring(HEADER_MESSAGE.length());
            int msgIndex = msg.indexOf('|');
            String fromNick = msg.substring(0, msgIndex);
            KinberContact contact = new KinberContact(fromNick, addr);
            contactList.add(contact);
            contact = contactList.get(addr);
            if (contact == null) {
                System.out.println("!!! handlePacket(MESSAGE): contact doesn't exist!");
                return;
            }
            String message = msg.substring(msgIndex+1);
            message = message.replaceAll("\r", "");
            jkHistory.log(contact.toString(), new KinberHistoryItem(contact.toString(), KinberHistory.MESSAGE_RECEIVED, new Date(), message));
            KinberUtil.log('<', "MESSAGE", contact.toStringV() + ": '"
                    + message + "'");
            kFrame.displayMessage(contact, message);
        } else if (msg.startsWith(HEADER_ACTIVATE)) {
            KinberUtil.log('<', "ACTVATE");
            kFrame.showOnTop();
        }
    }

    public void refreshAddresses(boolean forceUpdate) {
        KinberUtil.log('-', "NETWRK", "Refreshing IP addresses");
        addresses = getMyInetAddresses();
        // if there's no suitable address
        if (addresses.isEmpty()) {
            // go offline
            setStatus(STATUS_OFFLINE);
            return;
        }
        // if our current address is no longer available
        InetAddress curAddr = me.getAddress();
        if (curAddr == null || !addresses.contains(curAddr)) {
            // set a new one
            InetAddress newAddr = addresses.get(0);
            setAddress(newAddr);
        } else if (forceUpdate) {
            refresh(true);
        }
        // update the Tools menu
        kFrame.updateAddresses(addresses, me.getAddress());
    }

    public void refreshAddresses() {
        refreshAddresses(false);
    }

    private synchronized void timerFired() {
        //KinberUtil.log('-', "TIMER");
        for (int i = 0; i < contactList.size(); i++) {
            KinberContact contact = contactList.get(i);
            //System.out.println("    " + contact.toStringV() + ": " + contact.getUpdatedTime());
            if (contact.wasCalled()) {
                if (contact.getCalledTime() > MAX_CALLED_TIME) {
                    // no longer present, remove contact
                    KinberUtil.log('-', "GONE", contact.toStringV());
                    contactList.remove(contact);
                }
            } else if (contact.getUpdatedTime() > MAX_IDLE_TIME) {
                // contact was not updated recently - call
                KinberUtil.log('-', "IDLE", contact.toStringV());
                sendCall(contact);
                contact.setCalled();
            }
        }

        // FIXME: is the full refresh really necessary?
        if (contactList.getRefreshTime() > MAX_REFRESH_TIME) {
            sendNetworkCall();
        }

    }

    public ListModel getListModel() {
        return contactList.getListModel();
    }

    private List<InetAddress> getMyInetAddresses() {
        List<InetAddress> addrs = new ArrayList<InetAddress>();
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            boolean namePrinted;
            for (NetworkInterface netint : Collections.list(nets)) {
                namePrinted = false;
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress addr : Collections.list(inetAddresses)) {
                    if (addr.getAddress().length == 4 && !addr.isLoopbackAddress()) {
                        if (!namePrinted) {
                            KinberUtil.log('-', "IFACE", netint.getName() + ": " + netint.getDisplayName());
                            KinberUtil.log('-', "IFACE", netint.getName() + ":", false);
                            namePrinted = true;
                        }
                        out.print(" " + KinberUtil.getIP(addr));
                        if (!netint.getName().startsWith("vbox"))
                            addrs.add(addr);
                    }
                }
                if (namePrinted) out.print("\n");
            }
        } catch (Exception e) { System.out.println("!!! getMyInetAddresses: Error getting addresses: " + e); }
        return addrs;
    }

    private InetAddress getDestNetworkAddress() {
        InetAddress addr = null;
        try {
            addr = InetAddress.getByAddress(new byte[] {(byte)192, (byte)168, (byte)0, (byte)0});
        } catch (Exception e) { System.out.println("Error creating network address: " + e); }
        return addr;
    }

    private class KinberReceiver extends Thread {

        private int bindPort;
        private JavaKinber jKinber;
        private DatagramSocket rxSocket;

        public KinberReceiver(JavaKinber aJKinber, int aBindPort) throws IOException {
            super();
            bindPort = aBindPort;
            jKinber = aJKinber;
            rxSocket = new DatagramSocket(bindPort);
        }

        @Override
        public void run() {
            byte[] buf = new byte[1500];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    rxSocket.receive(packet);
                    jKinber.handlePacket(packet);
                } catch (IOException e) { System.out.println("Error receiving packet: " + e); }
            }
        }

    }

}
