package tapin.ui;

public class ManagementFrame extends javax.swing.JFrame {
    private tapin.model.Account actor;
    private tapin.model.Entity entity;
    private final tapin.service.ManagementService service=new tapin.service.ManagementService();
    private java.util.List<java.util.Map<String,Object>> rows=java.util.List.of();
    private boolean loading;
    private record ListData(java.util.List<java.util.Map<String,Object>> rows,java.util.Map<String,java.util.List<tapin.model.Choice>> choices){}
    public ManagementFrame(){initComponents();Style.frame(this,root,logo,140);}
    public ManagementFrame(tapin.model.Account account,tapin.model.Entity kind){
        this();tapin.service.AuthService.requireAdmin(account);actor=account;entity=kind;heading.setText("Manage "+kind.title);
        remove.setText(kind==tapin.model.Entity.STUDENTS?"Deactivate Selected":"Delete Selected");Style.primary(create);
        refresh.addActionListener(e->load());create.addActionListener(e->editRecord(null));edit.addActionListener(e->{var r=selected();if(r!=null)editRecord(r);});
        remove.addActionListener(e->removeRecord());load();
    }
    private java.util.Map<String,Object> selected(){
        int v=records.getSelectedRow();if(v<0){javax.swing.JOptionPane.showMessageDialog(this,"Select a row first.");return null;}
        return rows.get(records.convertRowIndexToModel(v));
    }
    private void load(){
        if(loading)return;loading=true;
        new javax.swing.SwingWorker<ListData,Void>(){
            protected ListData doInBackground() throws Exception{
                var choices=new java.util.HashMap<String,java.util.List<tapin.model.Choice>>();
                for(String f:entity.fields)if(f.equals("section_id")||f.equals("subject_id")||f.equals("teacher_id"))choices.put(f,service.choices(f));
                return new ListData(service.list(actor,entity),choices);
            }
            protected void done(){loading=false;try{
                var data=get();rows=data.rows();java.util.List<String> cols=new java.util.ArrayList<>(),labels=new java.util.ArrayList<>();
                for(int i=0;i<entity.fields.length;i++)if(!entity.fields[i].equals("password")){cols.add(entity.fields[i]);labels.add(entity.labels[i]);}
                var m=Style.table(records,labels.toArray(String[]::new));
                for(var r:rows)m.addRow(cols.stream().map(key->{Object value=r.get(key);if(data.choices().containsKey(key))for(var choice:data.choices().get(key))if(choice.id()==((Number)value).intValue())return (Object)choice.label();return value;}).toArray());
                hint.setText(rows.size()+" record(s). "+(entity==tapin.model.Entity.STUDENTS?"Deactivation preserves attendance history.":""));
            }catch(Exception e){Style.error(ManagementFrame.this,e.getCause()==null?e:e.getCause());}}
        }.execute();
    }
    private void editRecord(java.util.Map<String,Object> row){
        Style.work(this,()->{
            java.util.Map<String,java.util.List<tapin.model.Choice>> options=new java.util.HashMap<>();
            for(String f:entity.fields)if(f.equals("section_id")||f.equals("subject_id")||f.equals("teacher_id"))options.put(f,service.choices(f));
            return options;
        },options->ManagementEditor.show(this,actor,entity,row,options,()->load()));
    }
    private void removeRecord(){
        var row=selected();if(row==null)return;
        if(!Style.confirm(this,(entity==tapin.model.Entity.STUDENTS?"Deactivate":"Permanently delete")+" selected "+entity.title+" record?"))return;
        Style.work(this,()->{service.delete(actor,entity,tapin.dao.Sql.id(row,entity.id));return true;},ok->load());
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        root = new javax.swing.JPanel();
        root.setBackground(new java.awt.Color(239,245,250));
        root.setPreferredSize(new java.awt.Dimension(1050,640));
        root.setLayout(new java.awt.BorderLayout(12,12));
        top = new javax.swing.JPanel();
        top.setLayout(new java.awt.BorderLayout(12,12));
        logo = new javax.swing.JLabel();
        logo.setText("TapIn");
        logo.setFont(new java.awt.Font("Segoe UI", 0, 14));
        logo.setPreferredSize(new java.awt.Dimension(150,80));
        top.add(logo, java.awt.BorderLayout.WEST);
        heading = new javax.swing.JLabel();
        heading.setText("Manage records");
        heading.setFont(new java.awt.Font("Segoe UI", 0, 28));
        top.add(heading, java.awt.BorderLayout.CENTER);
        root.add(top, java.awt.BorderLayout.NORTH);
        scroll = new javax.swing.JScrollPane();
        records = new javax.swing.JTable();
        scroll.setViewportView(records);
        root.add(scroll, java.awt.BorderLayout.CENTER);
        actions = new javax.swing.JPanel();
        actions.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT,10,8));
        create = new javax.swing.JButton();
        create.setText("Create");
        actions.add(create);
        edit = new javax.swing.JButton();
        edit.setText("Edit Selected");
        actions.add(edit);
        remove = new javax.swing.JButton();
        remove.setText("Delete / Deactivate");
        actions.add(remove);
        refresh = new javax.swing.JButton();
        refresh.setText("Refresh");
        actions.add(refresh);
        hint = new javax.swing.JLabel();
        hint.setText("Select a row to edit or remove it.");
        hint.setFont(new java.awt.Font("Segoe UI", 0, 14));
        actions.add(hint);
        root.add(actions, java.awt.BorderLayout.SOUTH);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("TapIn - Management");
        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(root, java.awt.BorderLayout.CENTER);
        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel root;
    private javax.swing.JPanel top;
    private javax.swing.JLabel logo;
    private javax.swing.JLabel heading;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable records;
    private javax.swing.JPanel actions;
    private javax.swing.JButton create;
    private javax.swing.JButton edit;
    private javax.swing.JButton remove;
    private javax.swing.JButton refresh;
    private javax.swing.JLabel hint;
    // End of variables declaration//GEN-END:variables
}
