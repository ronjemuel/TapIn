package tapin.ui;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import tapin.dao.Sql;
import tapin.model.*;
import tapin.model.Choice;
import tapin.service.ManagementService;

/** A small shared editor avoids five copies of the same CRUD dialog. */
public final class ManagementEditor {
    private ManagementEditor() {}
    public static void show(JFrame owner,Account actor,Entity entity,Map<String,Object> row,Map<String,java.util.List<Choice>> options,Runnable saved) {
        JDialog dialog=new JDialog(owner,(row==null?"Create ":"Edit ")+entity.title,true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel fields=new JPanel(new GridLayout(0,2,12,14));fields.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        Map<String,JComponent> controls=new LinkedHashMap<>();
        for(int i=0;i<entity.fields.length;i++) {
            String key=entity.fields[i];Object value=row==null?null:row.get(key);JComponent input;
            if(options.containsKey(key)) {
                JComboBox<Choice> combo=new JComboBox<>(options.get(key).toArray(Choice[]::new));
                if(value!=null)for(int j=0;j<combo.getItemCount();j++)if(combo.getItemAt(j).id()==((Number)value).intValue())combo.setSelectedIndex(j);
                input=combo;
            } else if(key.equals("password")) input=new JPasswordField(24);
            else if(key.equals("active")) {JCheckBox check=new JCheckBox("Student is active");check.setSelected(value==null||Boolean.TRUE.equals(value)||"1".equals(value.toString()));input=check;}
            else if(key.equals("role")) {JComboBox<String> combo=new JComboBox<>(new String[]{"TEACHER","ADMIN"});if(value!=null)combo.setSelectedItem(value.toString());input=combo;}
            else if(key.equals("day_of_week")) {JComboBox<String> combo=new JComboBox<>(Arrays.stream(java.time.DayOfWeek.values()).map(Enum::name).toArray(String[]::new));combo.setSelectedItem(value==null?java.time.LocalDate.now().getDayOfWeek().name():value.toString());input=combo;}
            else {JTextField text=new JTextField(24);text.setText(value==null?(key.equals("grace_minutes")?"10":""):value.toString());input=text;}
            JLabel label=new JLabel(entity.labels[i]);label.setLabelFor(input);fields.add(label);fields.add(input);controls.put(key,input);
        }
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));JButton save=new JButton("Save"),cancel=new JButton("Cancel");Style.primary(save);actions.add(save);actions.add(cancel);
        cancel.addActionListener(e->dialog.dispose());
        save.addActionListener(e->{
            Map<String,Object> values=new LinkedHashMap<>();
            for(var entry:controls.entrySet()) {
                JComponent input=entry.getValue();Object value;
                if(input instanceof JPasswordField password)value=password.getPassword();
                else if(input instanceof JTextField text)value=text.getText();
                else if(input instanceof JCheckBox check)value=check.isSelected();
                else {Object choice=((JComboBox<?>)input).getSelectedItem();value=choice instanceof Choice c?c.id():choice;}
                values.put(entry.getKey(),value);
            }
            save.setEnabled(false);
            new SwingWorker<Integer,Void>(){
                protected Integer doInBackground() throws Exception{return new ManagementService().save(actor,entity,row==null?0:Sql.id(row,entity.id),values);}
                protected void done(){save.setEnabled(true);try{get();dialog.dispose();saved.run();}catch(Exception ex){Style.error(dialog,ex.getCause()==null?ex:ex.getCause());}}
            }.execute();
        });
        dialog.add(fields,BorderLayout.CENTER);dialog.add(actions,BorderLayout.SOUTH);dialog.getRootPane().setDefaultButton(save);dialog.pack();dialog.setLocationRelativeTo(owner);dialog.setVisible(true);
    }
}
