package javakinber;

import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.util.List;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.*;

public class KinberFrame extends JFrame {

    private JavaKinber jKinber;
    private KinberOptions kOptions;
    private TrayIcon trayIcon;
    private boolean trayIconOK;
    public ImageIcon iconEnvelopeSrc;
    public ImageIcon iconEnvelope;
    private ImageIcon[] iconsStatus;
    private ImageIcon iconExit;
    private ImageIcon iconOptions;
    private Image imageEnvelope;
    private JMenuItem[] miStatuses;
    private javax.swing.Timer blinkTimer;
    private boolean blinkTrayIcon;
    private java.util.List<KinberContact> blinkTabs;
    private JPopupMenu trayPopupMenu;
    private JPopupMenu contactPopupMenu;
    private KinberCopyPasteMenu copyPastePopupMenu;
    private boolean initialized;
    private ButtonGroup groupAddressesMenu;
    private ActionListener actionAddress;
    private KinberContact activeContact;

    public KinberFrame() {
        this.initialized = false;
        initComponents();
        KinberRes.init(this);
        this.menuFile.setMnemonic('F');
        this.menuTools.setMnemonic('T');
        this.menuHelp.setMnemonic('H');
        this.comboStatus.setEnabled(false);
        for (String text : JavaKinber.statusTexts) {
            this.comboStatus.addItem(text);
        }
        this.comboStatus.setEnabled(true);
        this.messageTextArea.setWrapStyleWord(true);
        setSize(500, 650);
        setLocationRelativeTo(null);
        this.trayIconOK = false;
        this.activeContact = null;

        this.groupAddressesMenu = new ButtonGroup();

        this.actionAddress = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                int i = 0;
                while (KinberFrame.this.menuTools.getItem(i) != evt.getSource()) {
                    i++;
                }
                if (KinberFrame.this.menuTools.getItem(i) != evt.getSource()) {
                    KinberUtil.log('W', "SETIP", "IP not found in menu!");
                    return;
                }
                KinberFrame.this.jKinber.setAddress(i);
            }
        };
        initIcons();

        this.kOptions = new KinberOptions();
        String nickname = this.kOptions.get("nickname");
        int bindPort = Integer.parseInt(this.kOptions.get("bind-port"));
        // FIXME: Better like this
        //int bindPort = this.kOptions.getInt("bind-port");
        try {
            this.jKinber = new JavaKinber(this, nickname, bindPort);
        } catch (Exception e) {
            KinberUtil.log('W', "ENGINE", "Error creating JavaKinber: " + e);

            JavaKinber.sendActivate();
            KinberUtil.log('-', "EXIT");
            System.exit(1);
        }

        createContactPopupMenu();
        copyPastePopupMenu = new KinberCopyPasteMenu(messageTextArea);

        this.kOptions.setKinber(this.jKinber);

        setTitle("JavaKinber");

        this.contactList.setModel(this.jKinber.getListModel());

        this.blinkTrayIcon = false;
        this.blinkTabs = new ArrayList();

        this.trayIconOK = addTrayIcon();

        this.blinkTimer = new Timer(500, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                KinberFrame.this.blinkTimerFired();
            }
        });
        this.blinkTimer.start();

        this.initialized = true;

        if (!this.trayIconOK) {
            setVisible(true);
        }

        boolean useLastStatus = KinberUtil.parseBoolean(this.kOptions.get("use-last-status"));
        int lastStatus = -1;
        if (useLastStatus) {
            String lastStatusStr = this.kOptions.get("last-status");
            lastStatus = JavaKinber.getStatusCode(lastStatusStr);
        }

        if (nickname != null) {
            if (lastStatus != -1) {
                setStatus(lastStatus);
            } else {
                setStatus(6);
            }
        } else {
            setStatus(6);
            this.kOptions.showOptions();
        }
    }

    public boolean showHideFrame() {
        if (isVisible()) {
            if ((getExtendedState() & 0x1) > 0) {
                showOnTop();
            } else {
                setVisible(false);
            }
        } else {
            setVisible(true);
        }
        return isVisible();
    }

    public void forceShowFrame() {
        if (isActive()) {
            return;
        }
        Point p = getLocation();
        int prevX = (int) p.getX();
        int prevY = (int) p.getY();

        if (isVisible()) {
            setVisible(false);
        }
        showOnTop();
        setLocation(p);
    }

    public void showOnTop() {
        if (!isVisible()) {
            setVisible(true);
        }

        toFront();
    }

    private void initIcons() {
        this.iconEnvelopeSrc = KinberRes.createImageIcon(this, "images/IconEnvelopeSrc.png", "");

        this.iconEnvelope = KinberRes.createImageIcon(this, "images/IconEnvelope.png", "");

        this.iconsStatus = new ImageIcon[JavaKinber.statusTexts.length];
        for (int i = 0; i < JavaKinber.statusTexts.length; i++) {
            this.iconsStatus[i] = KinberRes.createImageIcon(this, "images/JavaKinberBlue" + JavaKinber.statusTexts[i] + ".png", "");
        }

        this.iconExit = KinberRes.createImageIcon(this, "images/IconExit.png", "");
        this.iconOptions = KinberRes.createImageIcon(this, "images/IconOptions.png", "");

        this.imageEnvelope = this.iconEnvelope.getImage();

        this.mmiExit.setIcon(this.iconExit);
        this.mmiOptions.setIcon(this.iconOptions);
        this.mmiAbout.setIcon(KinberRes.iconBlueKinber);

        setIconImage(KinberRes.imageBlueKinber);
        this.tabbedPane.setIconAt(0, KinberRes.iconBlueKinber);
    }

    private synchronized void setStatus(int status) {
        if (!this.initialized) {
            return;
        }
        KinberUtil.log('-', "STATUS", JavaKinber.statusTexts[status] + " (" + status + ")");
        int oldStatus = this.jKinber.getStatus();
        this.jKinber.setStatus(status);
        setIconImage(this.iconsStatus[status].getImage());
        this.tabbedPane.setIconAt(0, this.iconsStatus[status]);
        this.trayIcon.setImage(this.iconsStatus[status].getImage());

        if (this.comboStatus.getSelectedIndex() != status) {
            this.comboStatus.setSelectedIndex(status);
        }
        if (oldStatus != -1) {
            this.miStatuses[oldStatus].setFont(this.miStatuses[status].getFont().deriveFont(0));
        }
        this.miStatuses[status].setFont(this.miStatuses[status].getFont().deriveFont(1));
        if (!this.miStatuses[status].isSelected()) {
            this.miStatuses[status].setSelected(true);
        }
    }

    public void setStatusBarText(String text) {
        this.statusBarLabel.setText("  " + text);
    }

    public boolean hasTrayIcon() {
        return this.trayIconOK;
    }

    private boolean addTrayIcon() {
        this.trayIcon = null;
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();

            MouseListener mouseListener = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent evt) {
                    if ((evt.getClickCount() == 2) && ((evt.getButton() & 0x1) > 0)) {
                        KinberFrame.this.forceShowFrame();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent evt) {
                    if ((evt.isPopupTrigger()) || ((evt.getButton() & 0x2) > 0)) {
                        int y = 0;
                        if (evt.getY() < KinberFrame.this.trayPopupMenu.getHeight()) {
                            y = evt.getY();
                        } else {
                            y = evt.getY() - KinberFrame.this.trayPopupMenu.getHeight();
                        }
                        KinberFrame.this.trayPopupMenu.setLocation(evt.getX() - 15, y);
                        KinberFrame.this.trayPopupMenu.setInvoker(KinberFrame.this.trayPopupMenu);
                        KinberFrame.this.trayPopupMenu.setVisible(true);
                    }
                }
            };
            this.trayPopupMenu = new JPopupMenu();

            JMenuItem miShow = new JMenuItem("Hide/Show");
            miShow.setFont(miShow.getFont().deriveFont(1));
            miShow.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent evt) {
                    KinberFrame.this.showHideFrame();
                }
            });
            JMenuItem miOptions = new JMenuItem("Options...", this.iconOptions);
            miOptions.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent evt) {
                    KinberFrame.this.showOptions();
                }
            });
            JCheckBoxMenuItem miAutoReply = new JCheckBoxMenuItem("Auto-Reply", true);

            ButtonGroup statusGroup = new ButtonGroup();

            this.miStatuses = new JRadioButtonMenuItem[JavaKinber.statusTexts.length];
            for (int i = 0; i < JavaKinber.statusTexts.length; i++) {
                final int sts = i;
                this.miStatuses[i] = new JRadioButtonMenuItem(JavaKinber.statusTexts[i], this.iconsStatus[i]);
                this.miStatuses[i].addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        KinberFrame.this.setStatus(sts);
                    }
                });
                statusGroup.add(this.miStatuses[i]);
            }

            JMenuItem miExit = new JMenuItem("Exit", this.iconExit);
            miExit.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent evt) {
                    KinberFrame.this.exit();
                }
            });
            this.trayPopupMenu.add(miShow);
            this.trayPopupMenu.add(miOptions);

            this.trayPopupMenu.addSeparator();
            this.trayPopupMenu.add(miAutoReply);
            this.trayPopupMenu.addSeparator();
            for (int i = 0; i < JavaKinber.statusTexts.length; i++) {
                this.trayPopupMenu.add(this.miStatuses[i]);
            }
            this.trayPopupMenu.addSeparator();
            this.trayPopupMenu.add(miExit);

            this.trayPopupMenu.addFocusListener(new FocusListener() {

                @Override
                public void focusGained(FocusEvent evt) {
                    System.out.println("trayPopup: Focus gained");
                }

                @Override
                public void focusLost(FocusEvent evt) {
                    System.out.println("trayPopup: Focus lost");
                }
            });
            this.trayPopupMenu.setLocation(3000, 3000);
            this.trayPopupMenu.setVisible(true);
            this.trayPopupMenu.setVisible(false);

            this.trayIcon = new TrayIcon(KinberRes.imageBlueKinber, "JavaKinber (" + this.jKinber.getIP() + ")");

            ActionListener actionListener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                }
            };
            this.trayIcon.setImageAutoSize(true);
            this.trayIcon.addActionListener(actionListener);
            this.trayIcon.addMouseListener(mouseListener);
            try {
                tray.add(this.trayIcon);
                setDefaultCloseOperation(1);
            } catch (AWTException e) {
                KinberUtil.log('W', "TRAY", "Couldn't add TrayIcon: " + e);
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public void setTrayToolTip() {
        if (hasTrayIcon()) {
            this.trayIcon.setToolTip("JavaKinber (" + this.jKinber.getIP() + ")");
        }
    }

    private void createContactPopupMenu() {
        this.contactPopupMenu = new JPopupMenu();

        JMenuItem miMessage = new JMenuItem("Send Message", this.iconEnvelope);
        miMessage.setFont(miMessage.getFont().deriveFont(1));
        miMessage.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                KinberConvPanel tab = getContactTab(activeContact, true);
            }
        });
        JMenuItem miHistory = new JMenuItem("History");
        miHistory.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                showHistoryFrame(activeContact.getNickName(), null);
            }
        });
        this.contactPopupMenu.add(miMessage);
        this.contactPopupMenu.add(miHistory);
    }

    public boolean isBlinkingTrayIcon() {
        return this.blinkTrayIcon;
    }

    public void blinkTrayIcon(boolean blink) {
        this.blinkTrayIcon = blink;
        if (!blink) {
            KinberUtil.log('D', "BLINKT", "Stopping blinking of tray icon");
            if (this.trayIcon.getImage() != KinberRes.imageBlueKinber) {
                this.trayIcon.setImage(KinberRes.imageBlueKinber);
            }
        }
    }

    public boolean isBlinkingTab(KinberContact contact) {
        return this.blinkTabs.contains(contact);
    }

    public void blinkTab(KinberContact contact, boolean blink) {
        if (blink) {
            if (!this.blinkTabs.contains(contact)) {
                this.blinkTabs.add(contact);
            }
        } else {
            this.blinkTabs.remove(contact);
            if (this.blinkTabs.isEmpty()) {
                blinkTrayIcon(false);
            }

            int i = getTabIndexByContact(contact);
            if ((i > -1) && (this.tabbedPane.getIconAt(i) != KinberRes.iconRedKinber)) {
                this.tabbedPane.setIconAt(i, KinberRes.iconRedKinber);
            }
        }
    }

    private void sendNewMessage() {
        boolean[] checkedContacts = this.contactList.getChecked();
        if (!aContactChecked()) {
            JOptionPane.showMessageDialog(null, "Please select at least one recipient.", "Send", 2);

            return;
        }

        String message = this.messageTextArea.getText();
        for (int i = 0; i < this.jKinber.contactList.size(); i++) {
            if (checkedContacts[i]) {
                KinberContact curContact = this.jKinber.contactList.get(i);
                this.jKinber.sendMessage(curContact, message);
                this.contactList.setChecked(i, false);
            }
        }
        this.messageTextArea.setText("");
    }

    public boolean[] getCheckedContacts() {
        return this.contactList.getChecked();
    }

    private KinberConvPanel getTabByAddress(InetAddress addr) {
        for (int i = 1; i < this.tabbedPane.getComponentCount(); i++) {
            KinberConvPanel tab = (KinberConvPanel) this.tabbedPane.getComponent(i);
            if ((tab != null) && (tab.getContact().getAddress().equals(addr))) {
                return tab;
            }
        }
        return null;
    }

    public KinberConvPanel getContactTab(KinberContact contact, boolean forceFocus) {
        int numTabsWas = this.tabbedPane.getTabCount();
        KinberConvPanel tab = getTabByAddress(contact.getAddress());
        if (tab == null) {
            tab = new KinberConvPanel(this.jKinber, this, contact);
            this.tabbedPane.addTab(contact.getNickName(), KinberRes.iconRedKinber, tab, contact.getNickName() + "(" + contact.getIP() + ")");
        }
        if ((forceFocus) || (!isActive()) || (numTabsWas == 1)) {
            this.tabbedPane.setSelectedComponent(tab);
            tab.focusTextArea();
        }
        return tab;
    }

    public int getTabIndexByContact(KinberContact contact) {
        for (int i = 1; i < this.tabbedPane.getComponentCount(); i++) {
            KinberConvPanel tab = (KinberConvPanel) this.tabbedPane.getComponent(i);
            if (tab.getContact() == contact) {
                return i;
            }
        }
        return -1;
    }

    public void displayMessage(KinberContact contact, String message) {
        forceShowFrame();
        int i = getTabIndexByContact(contact);
        KinberConvPanel tab = getContactTab(contact, !isActive());
        tab.displayMessage(contact, message);
        KinberUtil.AlertOnWindow(this);

        if (i != this.tabbedPane.getSelectedIndex()) {
            KinberUtil.log('D', "DISMPSG", "Going to blink tab " + contact.toStringV());
            blinkTab(contact, true);
            KinberUtil.log('D', "DISMPSG", "Going to blink tray icon");
            blinkTrayIcon(true);
        }
    }

    public void exit() {
        KinberUtil.log('-', "EXIT");
        this.kOptions.load();
        this.kOptions.set("last-status", JavaKinber.statusTexts[this.jKinber.getStatus()]);
        this.kOptions.save();
        setVisible(false);
        System.exit(0);
    }

    public void showOptions() {
        this.kOptions.showOptions();
    }

    public void showHelp() {
        JOptionPane.showMessageDialog(null, "JavaKinber version 0.9.9\n(c) 2009-2010 Petr Sedlacek, petr@sedlacek.biz\nReleased under GNU GPL v3\nProtocol compatible with Kinberlink 1.4", "About", 1);
    }

    public JFrame showHistoryFrame(String contactName, KinberConvPanel convPanel) {
        KinberHistoryFrame historyFrame = new KinberHistoryFrame(this.jKinber.getKinberHistory(), contactName, convPanel);
        if (getX() < getWidth()) {
            setBounds(getWidth(), getY(), getWidth(), getHeight());
        }
        historyFrame.setBounds(getX() - getWidth(), getY(), getWidth(), getHeight());
        historyFrame.setVisible(true);
        return historyFrame;
    }

    public void updateAddresses(List<InetAddress> addrs, InetAddress curAddr) {
        while (!this.menuTools.getItem(0).getText().equals("Refresh")) {
            this.groupAddressesMenu.remove(this.menuTools.getItem(0));
            this.menuTools.remove(0);
        }

        for (int i = 0; i < addrs.size(); i++) {
            InetAddress addr = (InetAddress) addrs.get(i);
            JRadioButtonMenuItem mi1 = new JRadioButtonMenuItem(KinberUtil.getIP(addr));
            mi1.addActionListener(this.actionAddress);

            this.menuTools.insert(mi1, i);
            this.groupAddressesMenu.add(mi1);

            if (addr.equals(curAddr)) {
                mi1.setSelected(true);
            }
        }
    }

    private synchronized void blinkTimerFired() {
        if (this.blinkTrayIcon) {
            if (this.trayIcon.getImage() == KinberRes.imageBlueKinber) {
                this.trayIcon.setImage(this.imageEnvelope);
            } else {
                this.trayIcon.setImage(KinberRes.imageBlueKinber);
            }
        }
        for (KinberContact contact : this.blinkTabs) {
            int i = getTabIndexByContact(contact);

            if (i > -1) {
                if (this.tabbedPane.getIconAt(i) == KinberRes.iconRedKinber) {
                    this.tabbedPane.setIconAt(i, this.iconEnvelope);
                } else {
                    this.tabbedPane.setIconAt(i, KinberRes.iconRedKinber);
                }
            }
        }
    }

    public boolean aContactChecked() {
        boolean isChecked = false;
        boolean[] checkedContacts = this.contactList.getChecked();
        for (int i = 0; i < checkedContacts.length; i++) {
            if (checkedContacts[i]) {
                isChecked = true;
                break;
            }
        }
        return isChecked;
    }

    private void buttonSendEnable() {
        boolean buttonEnable = (this.messageTextArea.getText().length() > 0) && (aContactChecked());
        if ((!this.buttonSend.isEnabled()) && (buttonEnable)) {
            this.buttonSend.setEnabled(true);
        } else if ((this.buttonSend.isEnabled()) && (!buttonEnable)) {
            this.buttonSend.setEnabled(false);
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    if (KinberUtil.getOperatingSystem() == 2) {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } else if (KinberUtil.getOperatingSystem() == 1) {
                        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                    }
                } catch (UnsupportedLookAndFeelException e) {
                } catch (ClassNotFoundException e) {
                } catch (InstantiationException e) {
                } catch (IllegalAccessException e) {
                }
                JFrame kFrame = new KinberFrame();
            }
        });
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        tabbedPane = new javax.swing.JTabbedPane();
        newMsgPanel = new javax.swing.JPanel();
        topPanel = new javax.swing.JPanel();
        contactListPane = new javax.swing.JScrollPane();
        contactList = new org.jdesktop.swingx.JXCheckList();
        messagePane = new javax.swing.JScrollPane();
        messageTextArea = new javax.swing.JTextArea();
        buttonPanel = new javax.swing.JPanel();
        leftButtonPanel = new javax.swing.JPanel();
        buttonAll = new javax.swing.JButton();
        buttonNone = new javax.swing.JButton();
        buttonRefresh = new javax.swing.JButton();
        rightButtonPanel = new javax.swing.JPanel();
        buttonSend = new javax.swing.JButton();
        statusBarPanel = new javax.swing.JPanel();
        statusBarLabel = new javax.swing.JLabel();
        comboStatus = new javax.swing.JComboBox();
        mainMenuBar = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        mmiExit = new javax.swing.JMenuItem();
        menuTools = new javax.swing.JMenu();
        mmiRefresh = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        mmiOptions = new javax.swing.JMenuItem();
        menuHelp = new javax.swing.JMenu();
        mmiAbout = new javax.swing.JMenuItem();

        setName("frame0"); // NOI18N
        getContentPane().setLayout(new java.awt.GridBagLayout());

        tabbedPane.setMinimumSize(new java.awt.Dimension(316, 84));
        tabbedPane.setPreferredSize(new java.awt.Dimension(450, 282));
        tabbedPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tabbedPaneMouseClicked(evt);
            }
        });
        tabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabbedPaneStateChanged(evt);
            }
        });

        newMsgPanel.setMinimumSize(new java.awt.Dimension(311, 57));
        newMsgPanel.setPreferredSize(new java.awt.Dimension(515, 160));
        newMsgPanel.setMaximumSize(new java.awt.Dimension(2147483647, 2147483647));
        newMsgPanel.setLayout(new java.awt.GridBagLayout());

        topPanel.setMinimumSize(new java.awt.Dimension(49, 22));
        topPanel.setPreferredSize(new java.awt.Dimension(378, 130));
        topPanel.setMaximumSize(new java.awt.Dimension(2147483647, 2147483647));
        topPanel.setLayout(new java.awt.GridBagLayout());

        contactListPane.setPreferredSize(new java.awt.Dimension(150, 130));

        contactList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                contactListMouseClicked(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                contactListMouseReleased(evt);
            }
        });
        contactList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                contactListKeyPressed(evt);
            }
        });
        contactListPane.setViewportView(contactList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        topPanel.add(contactListPane, gridBagConstraints);

        messagePane.setPreferredSize(new java.awt.Dimension(223, 78));

        messageTextArea.setColumns(20);
        messageTextArea.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        messageTextArea.setForeground(new java.awt.Color(51, 97, 147));
        messageTextArea.setLineWrap(true);
        messageTextArea.setRows(5);
        messageTextArea.setMinimumSize(new java.awt.Dimension(50, 15));
        messageTextArea.setPreferredSize(new java.awt.Dimension(287, 75));
        messageTextArea.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                messageTextAreaKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                messageTextAreaKeyReleased(evt);
            }
        });
        messagePane.setViewportView(messageTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        topPanel.add(messagePane, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        newMsgPanel.add(topPanel, gridBagConstraints);

        buttonPanel.setMinimumSize(new java.awt.Dimension(311, 35));
        buttonPanel.setPreferredSize(new java.awt.Dimension(515, 30));
        buttonPanel.setMaximumSize(new java.awt.Dimension(2147483647, 2147483647));
        buttonPanel.setLayout(new java.awt.GridBagLayout());

        leftButtonPanel.setMinimumSize(new java.awt.Dimension(231, 35));
        leftButtonPanel.setPreferredSize(new java.awt.Dimension(231, 35));
        leftButtonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        buttonAll.setText("All");
        buttonAll.setMaximumSize(new java.awt.Dimension(51, 25));
        buttonAll.setMinimumSize(new java.awt.Dimension(51, 25));
        buttonAll.setPreferredSize(new java.awt.Dimension(51, 25));
        buttonAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAllActionPerformed(evt);
            }
        });
        leftButtonPanel.add(buttonAll);

        buttonNone.setText("None");
        buttonNone.setMinimumSize(new java.awt.Dimension(71, 25));
        buttonNone.setPreferredSize(new java.awt.Dimension(71, 25));
        buttonNone.setMaximumSize(new java.awt.Dimension(71, 25));
        buttonNone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNoneActionPerformed(evt);
            }
        });
        leftButtonPanel.add(buttonNone);

        buttonRefresh.setText("Refresh");
        buttonRefresh.setMaximumSize(new java.awt.Dimension(89, 25));
        buttonRefresh.setMinimumSize(new java.awt.Dimension(89, 25));
        buttonRefresh.setPreferredSize(new java.awt.Dimension(89, 25));
        buttonRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRefreshActionPerformed(evt);
            }
        });
        leftButtonPanel.add(buttonRefresh);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 4.0;
        gridBagConstraints.weighty = 1.0;
        buttonPanel.add(leftButtonPanel, gridBagConstraints);

        rightButtonPanel.setMinimumSize(new java.awt.Dimension(80, 35));
        rightButtonPanel.setPreferredSize(new java.awt.Dimension(80, 35));
        rightButtonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        buttonSend.setText("Send");
        buttonSend.setEnabled(false);
        buttonSend.setMaximumSize(new java.awt.Dimension(70, 25));
        buttonSend.setMinimumSize(new java.awt.Dimension(70, 25));
        buttonSend.setPreferredSize(new java.awt.Dimension(70, 25));
        buttonSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSendActionPerformed(evt);
            }
        });
        rightButtonPanel.add(buttonSend);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 2.0;
        gridBagConstraints.weighty = 1.0;
        buttonPanel.add(rightButtonPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0;
        newMsgPanel.add(buttonPanel, gridBagConstraints);

        tabbedPane.addTab("Contacts", newMsgPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(tabbedPane, gridBagConstraints);

        statusBarPanel.setMinimumSize(new java.awt.Dimension(115, 28));
        statusBarPanel.setPreferredSize(new java.awt.Dimension(115, 28));
        statusBarPanel.setMaximumSize(new java.awt.Dimension(2147483647, 2147483647));
        statusBarPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        statusBarPanel.setLayout(new java.awt.GridBagLayout());

        statusBarLabel.setText(" Status Bar");
        statusBarLabel.setMinimumSize(new java.awt.Dimension(79, 15));
        statusBarLabel.setPreferredSize(new java.awt.Dimension(79, 15));
        statusBarLabel.setMaximumSize(new java.awt.Dimension(79, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        statusBarPanel.add(statusBarLabel, gridBagConstraints);

        comboStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboStatusActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        statusBarPanel.add(comboStatus, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(statusBarPanel, gridBagConstraints);

        mainMenuBar.setMinimumSize(new java.awt.Dimension(3, 3));
        mainMenuBar.setPreferredSize(new java.awt.Dimension(133, 21));
        mainMenuBar.setMaximumSize(new java.awt.Dimension(133, 32769));

        menuFile.setText("File");
        menuFile.setMinimumSize(new java.awt.Dimension(1, 1));
        menuFile.setPreferredSize(new java.awt.Dimension(37, 19));
        menuFile.setMaximumSize(new java.awt.Dimension(37, 32767));

        mmiExit.setText("Exit");
        mmiExit.setPreferredSize(new java.awt.Dimension(77, 19));
        mmiExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mmiExitActionPerformed(evt);
            }
        });
        menuFile.add(mmiExit);

        mainMenuBar.add(menuFile);

        menuTools.setText("Tools");
        menuTools.setMinimumSize(new java.awt.Dimension(1, 1));
        menuTools.setPreferredSize(new java.awt.Dimension(51, 19));
        menuTools.setMaximumSize(new java.awt.Dimension(51, 32767));

        mmiRefresh.setText("Refresh");
        mmiRefresh.setPreferredSize(new java.awt.Dimension(75, 19));
        mmiRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mmiRefreshActionPerformed(evt);
            }
        });
        menuTools.add(mmiRefresh);

        jSeparator1.setFont(new java.awt.Font("Dialog", 1, 12));
        menuTools.add(jSeparator1);

        mmiOptions.setText("Options...");
        mmiOptions.setPreferredSize(new java.awt.Dimension(125, 19));
        mmiOptions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mmiOptionsActionPerformed(evt);
            }
        });
        menuTools.add(mmiOptions);

        mainMenuBar.add(menuTools);

        menuHelp.setText("Help");
        menuHelp.setMinimumSize(new java.awt.Dimension(1, 1));
        menuHelp.setPreferredSize(new java.awt.Dimension(45, 19));
        menuHelp.setMaximumSize(new java.awt.Dimension(45, 32767));

        mmiAbout.setText("About");
        mmiAbout.setPreferredSize(new java.awt.Dimension(63, 19));
        mmiAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mmiAboutActionPerformed(evt);
            }
        });
        menuHelp.add(mmiAbout);

        mainMenuBar.add(menuHelp);

        setJMenuBar(mainMenuBar);
    }// </editor-fold>//GEN-END:initComponents

    private void mmiExitActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mmiExitActionPerformed
        exit();
    }//GEN-LAST:event_mmiExitActionPerformed

    private void mmiOptionsActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mmiOptionsActionPerformed
        showOptions();
    }//GEN-LAST:event_mmiOptionsActionPerformed

    private void comboStatusActionPerformed(ActionEvent evt) {//GEN-FIRST:event_comboStatusActionPerformed
        setStatus(this.comboStatus.getSelectedIndex());
    }//GEN-LAST:event_comboStatusActionPerformed

    private void mmiAboutActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mmiAboutActionPerformed
        showHelp();
    }//GEN-LAST:event_mmiAboutActionPerformed

    private void mmiRefreshActionPerformed(ActionEvent evt) {//GEN-FIRST:event_mmiRefreshActionPerformed
        this.jKinber.refreshAddresses();
    }//GEN-LAST:event_mmiRefreshActionPerformed

    private void tabbedPaneStateChanged(ChangeEvent evt) {//GEN-FIRST:event_tabbedPaneStateChanged
        if (this.tabbedPane.getSelectedIndex() > 0) {
            KinberConvPanel panel = (KinberConvPanel) this.tabbedPane.getSelectedComponent();
            panel.focusTextArea();
        }
    }//GEN-LAST:event_tabbedPaneStateChanged

    private void tabbedPaneMouseClicked(MouseEvent evt) {//GEN-FIRST:event_tabbedPaneMouseClicked
        if (evt.getClickCount() == 2) {
            for (int i = 0; i < this.tabbedPane.getComponentCount(); i++) {
                Rectangle b = this.tabbedPane.getBoundsAt(i);
                if ((b.contains(evt.getX(), evt.getY())) && (i > 0)) {
                    this.tabbedPane.remove(i);
                    this.contactList.requestFocusInWindow();
                }
            }
        }
    }//GEN-LAST:event_tabbedPaneMouseClicked

    private void messageTextAreaKeyPressed(KeyEvent evt) {//GEN-FIRST:event_messageTextAreaKeyPressed
        if ((evt.getKeyCode() == 10) && ((evt.getModifiers() & 0x2) > 0) && (this.messageTextArea.getText().length() > 0)) {
            sendNewMessage();
        }
    }//GEN-LAST:event_messageTextAreaKeyPressed

    private void contactListKeyPressed(KeyEvent evt) {//GEN-FIRST:event_contactListKeyPressed
        if ((evt.getKeyCode() == 10) && ((evt.getModifiers() & 0x2) == 0)) {
            int index = this.contactList.getSelectedIndex();
            if (index != -1) {
                KinberContact contact = (KinberContact) this.contactList.getElementAt(index);
                KinberConvPanel tab = getContactTab(contact, true);
                return;
            }
        }
    }//GEN-LAST:event_contactListKeyPressed

    private void contactListMouseClicked(MouseEvent evt) {//GEN-FIRST:event_contactListMouseClicked
        if ((evt.getButton() & 0x1) > 0) {
            if (evt.getClickCount() == 2) {
                int index = this.contactList.locationToIndex(evt.getPoint());
                if (index != -1) {
                    KinberContact contact = (KinberContact) this.contactList.getElementAt(index);
                    KinberConvPanel tab = getContactTab(contact, true);
                    return;
                }
            } else {
                buttonSendEnable();
            }
        }
    }//GEN-LAST:event_contactListMouseClicked

    private void buttonAllActionPerformed(ActionEvent evt) {//GEN-FIRST:event_buttonAllActionPerformed
        this.contactList.setChecked(0, this.contactList.getElementCount() - 1, true);
    }//GEN-LAST:event_buttonAllActionPerformed

    private void buttonNoneActionPerformed(ActionEvent evt) {//GEN-FIRST:event_buttonNoneActionPerformed
        this.contactList.setChecked(0, this.contactList.getElementCount() - 1, false);
    }//GEN-LAST:event_buttonNoneActionPerformed

    private void buttonRefreshActionPerformed(ActionEvent evt) {//GEN-FIRST:event_buttonRefreshActionPerformed
        this.jKinber.sendNetworkCall();
    }//GEN-LAST:event_buttonRefreshActionPerformed

    private void buttonSendActionPerformed(ActionEvent evt) {//GEN-FIRST:event_buttonSendActionPerformed
        sendNewMessage();
    }//GEN-LAST:event_buttonSendActionPerformed

    private void messageTextAreaKeyReleased(KeyEvent evt) {//GEN-FIRST:event_messageTextAreaKeyReleased
        buttonSendEnable();
    }//GEN-LAST:event_messageTextAreaKeyReleased

    private void contactListMouseReleased(MouseEvent evt) {//GEN-FIRST:event_contactListMouseReleased
        if ((evt.isPopupTrigger()) || ((evt.getButton() & 0x2) > 0)) {
            int index = this.contactList.locationToIndex(evt.getPoint());
            Rectangle r = this.contactList.getCellBounds(index, index);
            if ((index != -1) && (r != null) && (r.contains(evt.getPoint()))) {
                this.activeContact = ((KinberContact) this.contactList.getElementAt(index));
                this.contactPopupMenu.setLocation(evt.getXOnScreen(), evt.getYOnScreen());
                this.contactPopupMenu.setInvoker(this.contactPopupMenu);
                this.contactPopupMenu.setVisible(true);
            }
        }
    }//GEN-LAST:event_contactListMouseReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAll;
    private javax.swing.JButton buttonNone;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton buttonRefresh;
    private javax.swing.JButton buttonSend;
    private javax.swing.JComboBox comboStatus;
    private org.jdesktop.swingx.JXCheckList contactList;
    private javax.swing.JScrollPane contactListPane;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPanel leftButtonPanel;
    private javax.swing.JMenuBar mainMenuBar;
    private javax.swing.JMenu menuFile;
    private javax.swing.JMenu menuHelp;
    private javax.swing.JMenu menuTools;
    private javax.swing.JScrollPane messagePane;
    private javax.swing.JTextArea messageTextArea;
    private javax.swing.JMenuItem mmiAbout;
    private javax.swing.JMenuItem mmiExit;
    private javax.swing.JMenuItem mmiOptions;
    private javax.swing.JMenuItem mmiRefresh;
    private javax.swing.JPanel newMsgPanel;
    private javax.swing.JPanel rightButtonPanel;
    private javax.swing.JLabel statusBarLabel;
    private javax.swing.JPanel statusBarPanel;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables
}