package plugins;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ConflictWindow {
    public ConflictWindow(JFrame frame) {
        JOptionPane.showMessageDialog(frame,
                        "There's a conflicting change.", 
                        "Conflict detected", JOptionPane.WARNING_MESSAGE);
    }
}
