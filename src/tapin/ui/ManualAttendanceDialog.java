package tapin.ui;

import java.awt.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.Map;
import javax.swing.*;
import tapin.dao.Sql;
import tapin.model.Account;
import tapin.service.AttendanceService;

public final class ManualAttendanceDialog {
    private ManualAttendanceDialog() {}
    public static void show(JFrame owner,Account actor,int session,LocalDate date,Map<String,Object> student,Runnable saved) {
        JDialog dialog=new JDialog(owner,"Manual Attendance - "+student.get("student_name"),true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel panel=new JPanel(new GridLayout(0,2,12,12));panel.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        JTextField in=new JTextField(time(student.get("time_in")),20),out=new JTextField(time(student.get("time_out")),20),remarks=new JTextField(Sql.str(student,"remarks"),25);
        panel.add(new JLabel("Session date"));panel.add(new JLabel(date.toString()));
        panel.add(new JLabel("Time In (HH:mm[:ss]; blank = absent)"));panel.add(in);
        panel.add(new JLabel("Time Out (optional HH:mm[:ss])"));panel.add(out);
        panel.add(new JLabel("Reason / remarks (required)"));panel.add(remarks);
        panel.add(new JLabel("Status is calculated from Time In."));panel.add(new JLabel("Source will be MANUAL."));
        JButton save=new JButton("Save Correction"),cancel=new JButton("Cancel");Style.primary(save);panel.add(save);panel.add(cancel);
        cancel.addActionListener(e->dialog.dispose());
        save.addActionListener(e->{
            LocalDateTime ti,to;
            try{ti=in.getText().isBlank()?null:date.atTime(LocalTime.parse(in.getText().trim()));to=out.getText().isBlank()?null:date.atTime(LocalTime.parse(out.getText().trim()));}
            catch(Exception ex){Style.error(dialog,new IllegalArgumentException("Enter valid times such as 08:05 or 08:05:30."));return;}
            String note=remarks.getText();save.setEnabled(false);
            new SwingWorker<Void,Void>(){
                protected Void doInBackground() throws Exception{new AttendanceService().manual(actor,session,Sql.id(student,"student_id"),ti,to,note);return null;}
                protected void done(){save.setEnabled(true);try{get();dialog.dispose();saved.run();}catch(Exception ex){Style.error(dialog,ex.getCause()==null?ex:ex.getCause());}}
            }.execute();
        });
        dialog.add(panel);dialog.pack();dialog.setLocationRelativeTo(owner);dialog.setVisible(true);
    }
    private static String time(Object value){return value instanceof Timestamp t?t.toLocalDateTime().toLocalTime().toString():"";}
}
