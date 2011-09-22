package javakinber;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 *
 * @author error
 */
public class KinberCopyPasteMenu extends JPopupMenu {
    private JTextComponent textComponent;
    private int menuItems = CUT | COPY | PASTE;
    private JMenuItem miCut;
    private JMenuItem miCopy;
    private JMenuItem miPaste;

    public static final int CUT = 0x01;
    public static final int COPY = 0x02;
    public static final int PASTE = 0x04;
    
    public KinberCopyPasteMenu(JTextComponent aTextComponent) {
        this(aTextComponent, CUT | COPY | PASTE);
    }

    public KinberCopyPasteMenu(JTextComponent aTextComponent, int theMenuItems) {
        super();
        this.textComponent = aTextComponent;
        this.menuItems = theMenuItems;

        final JPopupMenu thePopupMenu = this;

        if ((this.menuItems & CUT) != 0) {
            miCut = new JMenuItem("Cut");
            miCut.setFont(miCut.getFont().deriveFont(1));
            miCut.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    textComponent.cut();
                }
            });
            this.add(miCut);
        }
        if ((this.menuItems & COPY) != 0) {
            miCopy = new JMenuItem("Copy");
            miCopy.setFont(miCopy.getFont().deriveFont(1));
            miCopy.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    textComponent.copy();
                }
            });
            this.add(miCopy);
        }
        if ((this.menuItems & PASTE) != 0) {
            miPaste = new JMenuItem("Paste");
            miPaste.setFont(miPaste.getFont().deriveFont(1));
            miPaste.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    textComponent.paste();
                }
            });
            this.add(miPaste);
        }
        
        this.textComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent evt) {
                if ((evt.isPopupTrigger()) || ((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0)) {
                    if (textComponent.getSelectionEnd() > textComponent.getSelectionStart()) {
                        enableItems(true);
                    } else {
                        enableItems(false);
                    }
                    thePopupMenu.setLocation(evt.getXOnScreen(), evt.getYOnScreen());
                    thePopupMenu.setInvoker(thePopupMenu);
                    thePopupMenu.setVisible(true);
                }
            }
        });        
    }
    
    void enableItems(boolean status) {
        if (miCut != null) miCut.setEnabled(status);
        if (miCopy != null) miCopy.setEnabled(status);
        if (miPaste != null) {
            String clp = KinberUtil.getClipboardText();
            if (clp == null || clp.isEmpty()) miPaste.setEnabled(false);
            else miPaste.setEnabled(true);
        }
    }
    
}
