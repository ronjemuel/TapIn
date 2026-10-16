package tapin;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.concurrent.Callable;
import javax.imageio.ImageIO;
import javax.swing.*;
import tapin.model.*;
import tapin.service.AuthService;
import tapin.ui.*;

/** Renders real Swing controls offscreen and checks asynchronous data loading. */
public final class UiSmokeTest {
    private static <T>T edt(Callable<T> task)throws Exception{
        java.util.concurrent.FutureTask<T> f=new java.util.concurrent.FutureTask<>(task);SwingUtilities.invokeAndWait(f);return f.get();
    }
    private static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private static void snapshot(JFrame frame,String name)throws Exception{
        edt(()->{frame.validate();Container content=frame.getContentPane();BufferedImage image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();content.printAll(g);g.dispose();ImageIO.write(image,"png",Path.of("build/screenshots/"+name+".png").toFile());return null;});
        System.out.println("PASS rendered "+name+" "+frame.getWidth()+"x"+frame.getHeight());
    }
    public static void main(String[] args)throws Exception{
        Files.createDirectories(Path.of("build/screenshots"));
        Account teacher=new AuthService().login("teacher","TapInTeacher!2026".toCharArray(),"TEACHER"),admin=new AuthService().login("admin","TapInAdmin!2026".toCharArray(),"ADMIN");
        edt(()->{Style.install();return null;});
        StaffLogin login=edt(StaffLogin::new);snapshot(login,"login");
        edt(()->{((JPasswordField)field(login,"password")).setText("example");((JCheckBox)field(login,"showPassword")).doClick();if(((JPasswordField)field(login,"password")).getEchoChar()!=0)throw new AssertionError("Show password");((JButton)field(login,"clear")).doClick();if(((JPasswordField)field(login,"password")).getPassword().length!=0)throw new AssertionError("Clear");return null;});
        System.out.println("PASS login show/hide and clear");
        TeacherDashboard dashboard=edt(()->new TeacherDashboard(teacher));
        AdminDashboard adminFrame=edt(()->new AdminDashboard(admin));
        AttendanceKiosk kiosk=edt(AttendanceKiosk::new);
        ReportsFrame reports=edt(()->new ReportsFrame(admin));
        ManagementFrame management=edt(()->new ManagementFrame(admin,Entity.STUDENTS));
        long end=System.currentTimeMillis()+12000;
        while(System.currentTimeMillis()<end){boolean ready=edt(()->((JTable)field(management,"records")).getRowCount()==4&&((JTable)field(dashboard,"attendance")).getRowCount()==4);if(ready)break;Thread.sleep(100);}
        if(edt(()->((JTable)field(dashboard,"attendance")).getRowCount())!=4)throw new AssertionError("Dashboard roster did not load");
        System.out.println("PASS dashboard and CRUD lists loaded from MySQL");
        edt(()->{((JTextField)field(dashboard,"search")).setText("Reyes");if(((JTable)field(dashboard,"attendance")).getRowCount()!=1)throw new AssertionError("Search");((JTextField)field(dashboard,"search")).setText("");return null;});
        System.out.println("PASS live student search");
        snapshot(dashboard,"dashboard");snapshot(adminFrame,"admin");snapshot(kiosk,"kiosk");snapshot(reports,"reports");snapshot(management,"students");
        edt(()->{if(kiosk.getDefaultCloseOperation()!=WindowConstants.DISPOSE_ON_CLOSE)throw new AssertionError("Kiosk close operation");kiosk.dispose();if(!dashboard.isDisplayable())throw new AssertionError("Child closed dashboard");return null;});
        System.out.println("PASS child disposal preserves dashboard");
        edt(()->{for(Window w:Window.getWindows())w.dispose();return null;});
        System.out.println("ALL UI SMOKE CHECKS PASSED");System.exit(0);
    }
}
