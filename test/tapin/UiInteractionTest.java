package tapin;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import javax.swing.*;
import tapin.ui.*;

/** Runs real window navigation and HID Enter events against isolated demo data. */
public final class UiInteractionTest {
    private static <T>T edt(Callable<T> task)throws Exception{FutureTask<T> f=new FutureTask<>(task);SwingUtilities.invokeAndWait(f);return f.get();}
    private static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private static void click(Object o,String name)throws Exception{edt(()->{((JButton)field(o,name)).doClick();return null;});}
    private static void waitFor(Callable<Boolean> condition,String message)throws Exception{
        long until=System.currentTimeMillis()+15000;
        while(System.currentTimeMillis()<until){if(edt(condition)){System.out.println("PASS "+message);return;}Thread.sleep(100);}
        throw new AssertionError(message);
    }
    private static <T extends Window>T window(Class<T> type){for(Window w:Window.getWindows())if(type.isInstance(w)&&w.isDisplayable()&&w.isVisible())return type.cast(w);return null;}
    private static void confirmNext()throws Exception{edt(()->{javax.swing.Timer timer=new javax.swing.Timer(100,e->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isVisible())for(Component c:d.getContentPane().getComponents())if(c instanceof JOptionPane pane){pane.setValue(JOptionPane.YES_OPTION);((javax.swing.Timer)e.getSource()).stop();}});timer.start();return null;});}
    private static StaffLogin login(String username,String password,boolean admin)throws Exception{
        StaffLogin f=edt(()->{StaffLogin l=window(StaffLogin.class);if(l==null){l=new StaffLogin();l.setVisible(true);}return l;});
        edt(()->{((JTextField)field(f,"username")).setText(username);((JPasswordField)field(f,"password")).setText(password);return null;});
        if(admin)click(f,"admin");click(f,"login");return f;
    }
    public static void main(String[] args)throws Exception{
        try {
            edt(()->{Style.install();return null;});
            StaffLogin invalid=login("teacher","bad-password",false);
            waitFor(()->((JLabel)field(invalid,"message")).getText().contains("Incorrect"),"invalid login feedback through Log In button");
            login("teacher","TapInTeacher!2026",false);
            waitFor(()->window(TeacherDashboard.class)!=null,"teacher login opens dashboard");
            TeacherDashboard teacher=edt(()->window(TeacherDashboard.class));
            waitFor(()->((JButton)field(teacher,"start")).isEnabled()&&((JTable)field(teacher,"attendance")).getRowCount()==4,"dashboard loaded with today's schedule");
            click(teacher,"start");waitFor(()->((JLabel)field(teacher,"sessionStatus")).getText().contains("OPEN"),"Start Session button opens session");
            click(teacher,"attendanceNav");AttendanceKiosk kiosk=edt(()->window(AttendanceKiosk.class));
            edt(()->{JTextField uid=(JTextField)field(kiosk,"uid");uid.setText("0001234567");uid.postActionEvent();return null;});
            waitFor(()->((JLabel)field(kiosk,"result")).getText().contains("Time In recorded"),"HID UID + Enter records Time In");
            Thread.sleep(3100);
            edt(()->{JTextField uid=(JTextField)field(kiosk,"uid");uid.setText("0001234567");uid.postActionEvent();return null;});
            waitFor(()->((JLabel)field(kiosk,"result")).getText().contains("Time Out recorded"),"HID second scan records Time Out");
            edt(()->{kiosk.dispose();return null;});
            waitFor(teacher::isDisplayable,"closing kiosk preserves dashboard");
            confirmNext();click(teacher,"close");waitFor(()->((JLabel)field(teacher,"sessionStatus")).getText().contains("CLOSED"),"Close Session button commits absences");
            waitFor(()->((JLabel)field(teacher,"absent")).getText().equals("Absent: 3"),"live dashboard shows three absences");
            confirmNext();click(teacher,"logout");waitFor(()->!teacher.isDisplayable()&&window(StaffLogin.class)!=null,"teacher logout disposes dashboard and returns to login");
            login("admin","TapInAdmin!2026",true);waitFor(()->window(AdminDashboard.class)!=null,"administrator role selector and login");
            AdminDashboard admin=edt(()->window(AdminDashboard.class));click(admin,"students");waitFor(()->window(ManagementFrame.class)!=null,"Manage Students opens CRUD window");
            ManagementFrame child=edt(()->window(ManagementFrame.class));confirmNext();click(admin,"logout");
            waitFor(()->!admin.isDisplayable()&&!child.isDisplayable()&&window(StaffLogin.class)!=null,"admin logout also closes management children");
            System.out.println("ALL UI INTERACTION CHECKS PASSED");
        } finally {edt(()->{for(Window w:Window.getWindows())w.dispose();return null;});}
        System.exit(0);
    }
}
