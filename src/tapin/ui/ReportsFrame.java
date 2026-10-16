package tapin.ui;

public class ReportsFrame extends javax.swing.JFrame {
    private tapin.model.Account actor;
    private boolean busy;
    public ReportsFrame(){initComponents();Style.frame(this,root,logo,140);}
    public ReportsFrame(tapin.model.Account account){
        this();actor=account;Style.primary(apply);date.setText(java.time.LocalDate.now().toString());
        Style.table(records,"Date","Section","Subject","Student No","Student Name","RFID UID","Time In","Time Out","Status","Source","Remarks");
        apply.addActionListener(e->load());student.addActionListener(e->load());date.addActionListener(e->load());
        export.addActionListener(e->Style.export(this,records));
        print.addActionListener(e->{try{records.print();}catch(java.awt.print.PrinterException ex){Style.error(this,ex);}});
        Style.work(this,()->new tapin.dao.AttendanceDao().schedules(actor),rows->{
            section.addItem(new tapin.model.Choice(0,"All sections"));subject.addItem(new tapin.model.Choice(0,"All subjects"));
            java.util.Set<Integer> sec=new java.util.HashSet<>(),sub=new java.util.HashSet<>();
            for(var r:rows){int a=tapin.dao.Sql.id(r,"section_id"),b=tapin.dao.Sql.id(r,"subject_id");if(sec.add(a))section.addItem(new tapin.model.Choice(a,tapin.dao.Sql.str(r,"section_name")));if(sub.add(b))subject.addItem(new tapin.model.Choice(b,tapin.dao.Sql.str(r,"subject_code")));}
            load();
        });
    }
    private int id(javax.swing.JComboBox<Object> box){return box.getSelectedItem() instanceof tapin.model.Choice c?c.id():0;}
    private void load(){
        if(busy)return;
        java.time.LocalDate day;
        try{day=date.getText().isBlank()?null:java.time.LocalDate.parse(date.getText().trim());}catch(Exception e){Style.error(this,new IllegalArgumentException("Use date format yyyy-MM-dd, or leave blank for all dates."));return;}
        int sec=id(section),sub=id(subject);String term=student.getText();busy=true;apply.setEnabled(false);
        new javax.swing.SwingWorker<java.util.List<java.util.Map<String,Object>>,Void>(){
            protected java.util.List<java.util.Map<String,Object>> doInBackground() throws Exception{return new tapin.dao.AttendanceDao().reports(actor,day,sec,sub,term);}
            protected void done(){busy=false;apply.setEnabled(true);try{
                var rows=get();var m=(javax.swing.table.DefaultTableModel)records.getModel();m.setRowCount(0);
                for(var r:rows)m.addRow(r.values().toArray());
                long on=rows.stream().filter(r->"ON_TIME".equals(r.get("status"))).count(),late=rows.stream().filter(r->"LATE".equals(r.get("status"))).count(),abs=rows.stream().filter(r->"ABSENT".equals(r.get("status"))).count();
                summary.setText(rows.size()+" records | On time: "+on+" | Late: "+late+" | Absent: "+abs);
            }catch(Exception e){Style.error(ReportsFrame.this,e.getCause()==null?e:e.getCause());}}
        }.execute();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        root = new javax.swing.JPanel();
        root.setBackground(new java.awt.Color(239,245,250));
        root.setPreferredSize(new java.awt.Dimension(1180,660));
        root.setLayout(new java.awt.BorderLayout(12,12));
        top = new javax.swing.JPanel();
        top.setLayout(new java.awt.GridLayout(0,1,12,12));
        brandHeader = new javax.swing.JPanel();
        brandHeader.setLayout(new java.awt.BorderLayout(12,12));
        logo = new javax.swing.JLabel();
        logo.setText("TapIn");
        logo.setFont(new java.awt.Font("Segoe UI", 0, 14));
        logo.setPreferredSize(new java.awt.Dimension(150,80));
        brandHeader.add(logo, java.awt.BorderLayout.WEST);
        heading = new javax.swing.JLabel();
        heading.setText("Attendance Reports");
        heading.setFont(new java.awt.Font("Segoe UI", 0, 28));
        brandHeader.add(heading, java.awt.BorderLayout.CENTER);
        top.add(brandHeader);
        filters = new javax.swing.JPanel();
        filters.setLayout(new java.awt.GridLayout(2,4,12,12));
        dateLabel = new javax.swing.JLabel();
        dateLabel.setText("Date (yyyy-MM-dd; blank = all)");
        dateLabel.setFont(new java.awt.Font("Segoe UI", 0, 14));
        filters.add(dateLabel);
        sectionLabel = new javax.swing.JLabel();
        sectionLabel.setText("Section");
        sectionLabel.setFont(new java.awt.Font("Segoe UI", 0, 14));
        filters.add(sectionLabel);
        subjectLabel = new javax.swing.JLabel();
        subjectLabel.setText("Subject");
        subjectLabel.setFont(new java.awt.Font("Segoe UI", 0, 14));
        filters.add(subjectLabel);
        studentLabel = new javax.swing.JLabel();
        studentLabel.setText("Student name or number");
        studentLabel.setFont(new java.awt.Font("Segoe UI", 0, 14));
        filters.add(studentLabel);
        date = new javax.swing.JTextField();
        filters.add(date);
        section = new javax.swing.JComboBox<>();
        filters.add(section);
        subject = new javax.swing.JComboBox<>();
        filters.add(subject);
        student = new javax.swing.JTextField();
        filters.add(student);
        top.add(filters);
        root.add(top, java.awt.BorderLayout.NORTH);
        scroll = new javax.swing.JScrollPane();
        records = new javax.swing.JTable();
        scroll.setViewportView(records);
        root.add(scroll, java.awt.BorderLayout.CENTER);
        actions = new javax.swing.JPanel();
        actions.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT,10,8));
        apply = new javax.swing.JButton();
        apply.setText("Apply Filters");
        actions.add(apply);
        export = new javax.swing.JButton();
        export.setText("Export CSV");
        actions.add(export);
        print = new javax.swing.JButton();
        print.setText("Print");
        actions.add(print);
        summary = new javax.swing.JLabel();
        summary.setText("No records loaded.");
        summary.setFont(new java.awt.Font("Segoe UI", 0, 14));
        actions.add(summary);
        root.add(actions, java.awt.BorderLayout.SOUTH);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("TapIn - Attendance Reports");
        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(root, java.awt.BorderLayout.CENTER);
        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel root;
    private javax.swing.JPanel top;
    private javax.swing.JPanel brandHeader;
    private javax.swing.JLabel logo;
    private javax.swing.JLabel heading;
    private javax.swing.JPanel filters;
    private javax.swing.JLabel dateLabel;
    private javax.swing.JLabel sectionLabel;
    private javax.swing.JLabel subjectLabel;
    private javax.swing.JLabel studentLabel;
    private javax.swing.JTextField date;
    private javax.swing.JComboBox<Object> section;
    private javax.swing.JComboBox<Object> subject;
    private javax.swing.JTextField student;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable records;
    private javax.swing.JPanel actions;
    private javax.swing.JButton apply;
    private javax.swing.JButton export;
    private javax.swing.JButton print;
    private javax.swing.JLabel summary;
    // End of variables declaration//GEN-END:variables
}

