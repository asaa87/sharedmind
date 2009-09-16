package plugins.sharedmind.view;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class TransportErrorWindow {
    public TransportErrorWindow(JFrame frame, String error_message) {
        JOptionPane.showMessageDialog(frame, error_message, 
                        "Transport Error", JOptionPane.WARNING_MESSAGE);
    }
}
