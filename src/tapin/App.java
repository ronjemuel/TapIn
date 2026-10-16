package tapin;

import javax.swing.SwingUtilities;
import tapin.ui.*;

public final class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{Style.install();if(args.length>0&&args[0].equals("--kiosk"))new AttendanceKiosk().setVisible(true);else new StaffLogin().setVisible(true);});
    }
}
