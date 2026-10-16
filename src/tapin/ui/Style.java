package tapin.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public final class Style {
    public static final Color BLUE=new Color(0,126,245), NAVY=new Color(17,39,58), BG=new Color(239,245,250);
    private Style() {}
    public static void install() {
        UIManager.put("defaultFont",new Font("Segoe UI",Font.PLAIN,14));
        java.util.Enumeration<Object> keys=UIManager.getDefaults().keys();
        while(keys.hasMoreElements()){Object key=keys.nextElement();if(UIManager.get(key) instanceof javax.swing.plaf.FontUIResource) UIManager.put(key,new javax.swing.plaf.FontUIResource("Segoe UI",Font.PLAIN,14));}
        UIManager.put("Table.rowHeight",32);
    }
    public static void frame(JFrame frame,JPanel root,JLabel logo,int logoWidth) {
        polish(root);
        root.setBorder(new EmptyBorder(22,22,22,22));
        logo(logo,logoWidth);
        try {frame.setIconImage(ImageIO.read(Style.class.getResource("/resources/images/tapin_logo.png")));}catch(Exception ignored){}
        frame.pack();frame.setLocationRelativeTo(null);
    }
    private static void polish(Component component) {
        if(component instanceof JPanel panel && panel.getBackground().equals(UIManager.getColor("Panel.background")))panel.setBackground(Color.WHITE);
        if(component instanceof JCheckBox check)check.setBackground(Color.WHITE);
        if(component instanceof JButton button){
            button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            button.setFont(new Font("Segoe UI",Font.BOLD,14));button.setBackground(BG);button.setForeground(NAVY);
            button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(202,214,225)),new EmptyBorder(10,16,10,16)));
            button.setFocusPainted(false);
        }
        if(component instanceof JLabel label && label.getFont().getSize()>=22)label.setFont(label.getFont().deriveFont(Font.BOLD));
        if(component instanceof Container container)for(Component child:container.getComponents())polish(child);
    }
    public static void navigation(JButton... buttons){for(JButton button:buttons){button.setBackground(NAVY);button.setForeground(Color.WHITE);button.setHorizontalAlignment(SwingConstants.LEFT);button.setBorder(new EmptyBorder(10,18,10,18));}}
    public static void summary(JLabel label,Color background){label.setOpaque(true);label.setBackground(background);label.setBorder(new EmptyBorder(8,12,8,12));}
    public static void logo(JLabel label,int width) {
        try {
            BufferedImage original=ImageIO.read(Style.class.getResource("/resources/images/tapin_logo.png"));
            // Display the official logo without its large outer black margins; file is unchanged.
            BufferedImage crop=original.getSubimage(340,590,860,410);
            label.setIcon(new ImageIcon(crop.getScaledInstance(width,width*410/860,Image.SCALE_SMOOTH)));
            label.setText("");label.setOpaque(true);label.setBackground(Color.BLACK);
        } catch(Exception e){label.setText("TapIn logo unavailable");}
    }
    public static void primary(JButton button) {button.setBackground(BLUE);button.setForeground(Color.WHITE);button.setFocusPainted(false);}
    public static <T> void work(Component owner,Callable<T> task,Consumer<T> success) {
        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T,Void>() {
            protected T doInBackground() throws Exception {return task.call();}
            protected void done(){owner.setCursor(Cursor.getDefaultCursor());try{success.accept(get());}catch(Exception e){Throwable t=e.getCause()==null?e:e.getCause();error(owner,t);}}
        }.execute();
    }
    public static String message(Throwable t) {
        if(t instanceof SQLException s) {
            if(s.getErrorCode()==1062) {
                String detail=s.getMessage();
                if(detail.contains("rfid_uid")) return "This RFID UID is already assigned to a student.";
                if(detail.contains("student_no")) return "This student number already exists.";
                return "A record with this unique value already exists. Check the username, code, or name.";
            }
            if(s.getErrorCode()==1451) return "This record is used by other records. Preserve its history or remove its unused dependents first.";
            if(s.getErrorCode()==1452) return "The selected related record no longer exists. Refresh and choose again.";
            if(s.getSQLState()!=null && s.getSQLState().startsWith("08")) return "Cannot connect to MySQL. Start MySQL in XAMPP and check config/database.properties.";
            return "Database error: "+s.getMessage();
        }
        return t.getMessage()==null?t.toString():t.getMessage();
    }
    public static void error(Component owner,Throwable t) {JOptionPane.showMessageDialog(owner,message(t),"TapIn",JOptionPane.ERROR_MESSAGE);}
    public static String html(String text,int width){return "<html><div style='width:"+width+"px'>"+text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")+"</div></html>";}
    public static boolean confirm(Component owner,String text) {return JOptionPane.showConfirmDialog(owner,text,"TapIn",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}
    public static void export(Component owner,JTable table) {
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File("TapIn-attendance.csv"));
        if(chooser.showSaveDialog(owner)!=JFileChooser.APPROVE_OPTION)return;
        java.io.File file=chooser.getSelectedFile();
        if(file.exists()&&!confirm(owner,"Replace "+file.getName()+"?"))return;
        java.util.List<String> headers=new java.util.ArrayList<>();
        java.util.List<java.util.List<Object>> rows=new java.util.ArrayList<>();
        for(int col=0;col<table.getColumnCount();col++)headers.add(table.getColumnName(col));
        for(int row=0;row<table.getRowCount();row++){
            java.util.List<Object> values=new java.util.ArrayList<>();
            for(int col=0;col<table.getColumnCount();col++)values.add(table.getValueAt(row,col));rows.add(values);
        }
        work(owner,()->{tapin.util.Csv.write(file.toPath(),headers,rows);return file;},f->JOptionPane.showMessageDialog(owner,"Exported "+rows.size()+" rows to "+f));
    }
    public static javax.swing.table.DefaultTableModel table(JTable table,String... headers) {
        var model=new javax.swing.table.DefaultTableModel(headers,0){public boolean isCellEditable(int r,int c){return false;}};
        table.setModel(model);table.setRowHeight(34);table.setFillsViewportHeight(true);table.setAutoCreateRowSorter(true);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,13));return model;
    }
}
