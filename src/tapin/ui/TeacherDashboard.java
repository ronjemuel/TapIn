package tapin.ui;

public class TeacherDashboard extends javax.swing.JFrame {
    private tapin.model.Account actor;
    private final tapin.dao.AttendanceDao dao=new tapin.dao.AttendanceDao();
    private final tapin.service.AttendanceService service=new tapin.service.AttendanceService();
    private java.util.List<java.util.Map<String,Object>> schedules=java.util.List.of(), roster=java.util.List.of();
    private java.util.Map<String,Object> currentSession;
    private javax.swing.table.DefaultTableModel model;
    private javax.swing.Timer timer;
    private boolean loading, refreshing, changing;
    private int selectionVersion;
    private final java.util.List<java.awt.Window> children=new java.util.ArrayList<>();
    public TeacherDashboard(){initComponents();Style.frame(this,root,logo,170);}
    public TeacherDashboard(tapin.model.Account account){
        this(); actor=account;heading.setText("Attendance Dashboard - "+account.fullName());
        model=Style.table(attendance,"Student Name","RFID UID","Time In","Time Out","Status","Remarks");
        date.setModel(new javax.swing.SpinnerDateModel());date.setEditor(new javax.swing.JSpinner.DateEditor(date,"yyyy-MM-dd"));
        Style.primary(start);Style.primary(manual);
        Style.navigation(dashboardNav,attendanceNav,studentsNav,schedulesNav,reportsNav,logout);
        Style.primary(dashboardNav);
        Style.summary(total,new java.awt.Color(234,244,255));Style.summary(present,new java.awt.Color(233,248,241));
        Style.summary(late,new java.awt.Color(255,247,226));Style.summary(absent,new java.awt.Color(255,236,239));
        section.addActionListener(e->{if(!loading)loadSubjects();});
        subject.addActionListener(e->{if(!loading)loadMeetings();});
        schedule.addActionListener(e->{if(!loading)selectionChanged();});
        date.addChangeListener(e->selectionChanged());
        start.addActionListener(e->startSession());close.addActionListener(e->closeSession());
        refresh.addActionListener(e->refresh());export.addActionListener(e->Style.export(this,attendance));
        manual.addActionListener(e->manual());dashboardNav.addActionListener(e->refresh());
        attendanceNav.addActionListener(e->showChild(new AttendanceKiosk()));
        reportsNav.addActionListener(e->showChild(new ReportsFrame(actor)));
        studentsNav.addActionListener(e->showRoster());schedulesNav.addActionListener(e->showSchedules());
        logout.addActionListener(e->{if(Style.confirm(this,"Log out? Open sessions remain available until you close them.")){new StaffLogin().setVisible(true);dispose();}});
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){filter();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){filter();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){filter();}
        });
        addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){if(timer!=null)timer.stop();for(java.awt.Window w:children)w.dispose();}});
        loadSchedules();timer=new javax.swing.Timer(3000,e->refresh());timer.start();
    }
    private void showChild(javax.swing.JFrame f){children.add(f);f.setVisible(true);}
    private java.time.LocalDate selectedDate(){return ((java.util.Date)date.getValue()).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();}
    private int selectedId(javax.swing.JComboBox<Object> combo){return combo.getSelectedItem() instanceof tapin.model.Choice c?c.id():0;}
    private void loadSchedules(){
        Style.work(this,()->dao.schedules(actor),rows->{schedules=rows;loading=true;section.removeAllItems();
            java.util.Set<Integer> seen=new java.util.HashSet<>();
            for(var r:rows){int id=tapin.dao.Sql.id(r,"section_id");if(seen.add(id))section.addItem(new tapin.model.Choice(id,tapin.dao.Sql.str(r,"section_name")));}
            loading=false;loadSubjects();
        });
    }
    private void loadSubjects(){
        loading=true;subject.removeAllItems();java.util.Set<Integer> seen=new java.util.HashSet<>();
        for(var r:schedules)if(tapin.dao.Sql.id(r,"section_id")==selectedId(section)){int id=tapin.dao.Sql.id(r,"subject_id");if(seen.add(id))subject.addItem(new tapin.model.Choice(id,tapin.dao.Sql.str(r,"subject_code")));}
        loading=false;loadMeetings();
    }
    private void loadMeetings(){
        loading=true;schedule.removeAllItems();
        for(var r:schedules)if(tapin.dao.Sql.id(r,"section_id")==selectedId(section)&&tapin.dao.Sql.id(r,"subject_id")==selectedId(subject))
            schedule.addItem(new tapin.model.Choice(tapin.dao.Sql.id(r,"schedule_id"),r.get("day_of_week")+" "+r.get("start_time")+" - "+r.get("end_time")));
        for(int i=0;i<schedule.getItemCount();i++)if(schedule.getItemAt(i).toString().startsWith(selectedDate().getDayOfWeek().name())){schedule.setSelectedIndex(i);break;}
        loading=false;selectionChanged();
    }
    private void selectionChanged(){selectionVersion++;currentSession=null;start.setEnabled(false);close.setEnabled(false);manual.setEnabled(false);refresh();}
    private java.util.Map<String,Object> selectedSchedule(){for(var r:schedules)if(tapin.dao.Sql.id(r,"schedule_id")==selectedId(schedule))return r;return null;}
    private record Snapshot(java.util.Map<String,Object> session,java.util.List<java.util.Map<String,Object>> rows){}
    private void refresh(){
        if(actor==null||loading||refreshing||changing)return;
        int id=selectedId(schedule),version=selectionVersion;
        if(id==0){model.setRowCount(0);currentSession=null;sessionStatus.setText("No assigned schedule");return;}
        java.time.LocalDate day=selectedDate();refreshing=true;
        new javax.swing.SwingWorker<Snapshot,Void>(){
            protected Snapshot doInBackground() throws Exception{
                var s=dao.session(actor,id,day);return new Snapshot(s,dao.roster(actor,id,s==null?0:tapin.dao.Sql.id(s,"session_id")));
            }
            protected void done(){
                refreshing=false;if(!isDisplayable())return;
                if(version!=selectionVersion){refresh();return;}
                try{
                    Snapshot data=get();currentSession=data.session();
                    int selected=attendance.getSelectedRow();int selectedStudent=selected<0?-1:tapin.dao.Sql.id(rosterSafe(selected),"student_id");
                    roster=data.rows();
                    model.setRowCount(0);int p=0,l=0,a=0;
                    for(var r:roster){String status=tapin.dao.Sql.str(r,"status");if(status.equals("ON_TIME"))p++;if(status.equals("LATE"))l++;if(status.equals("ABSENT"))a++;
                        model.addRow(new Object[]{r.get("student_name"),r.get("rfid_uid"),r.get("time_in"),r.get("time_out"),status,r.get("remarks")});}
                    total.setText("Total: "+roster.size());present.setText("Present: "+p);late.setText("Late: "+l);absent.setText("Absent: "+a);
                    var sc=selectedSchedule();sessionStatus.setText("Session: "+(currentSession==null?"NOT STARTED":currentSession.get("status"))+"   |   Grace period: "+sc.get("grace_minutes")+" minutes");
                    start.setEnabled(currentSession==null);close.setEnabled(currentSession!=null&&"OPEN".equals(currentSession.get("status")));manual.setEnabled(currentSession!=null);
                    for(int i=0;i<roster.size();i++)if(tapin.dao.Sql.id(roster.get(i),"student_id")==selectedStudent){int v=attendance.convertRowIndexToView(i);if(v>=0)attendance.setRowSelectionInterval(v,v);}
                }catch(Exception ex){sessionStatus.setText(Style.message(ex.getCause()==null?ex:ex.getCause()));}
            }
        }.execute();
    }
    private java.util.Map<String,Object> rosterSafe(int view){
        int i=attendance.convertRowIndexToModel(view);if(i<roster.size())return roster.get(i);return java.util.Map.of("student_id",-1);
    }
    @SuppressWarnings("unchecked") private void filter(){((javax.swing.table.TableRowSorter<javax.swing.table.DefaultTableModel>)attendance.getRowSorter()).setRowFilter(javax.swing.RowFilter.regexFilter("(?i)"+java.util.regex.Pattern.quote(search.getText()),0,1));}
    private void startSession(){
        if(changing)return;int id=selectedId(schedule);java.time.LocalDate day=selectedDate();changing=true;
        runChange(()->{service.start(actor,id,day);return null;});
    }
    private void closeSession(){
        if(changing||currentSession==null)return;
        if(!Style.confirm(this,"Close session and mark students with no Time In as ABSENT?"))return;
        int id=tapin.dao.Sql.id(currentSession,"session_id");changing=true;runChange(()->{service.close(actor,id);return null;});
    }
    private void runChange(java.util.concurrent.Callable<Void> task){
        start.setEnabled(false);close.setEnabled(false);
        new javax.swing.SwingWorker<Void,Void>(){
            protected Void doInBackground() throws Exception{return task.call();}
            protected void done(){changing=false;try{get();}catch(Exception e){Style.error(TeacherDashboard.this,e.getCause()==null?e:e.getCause());}refresh();}
        }.execute();
    }
    private void manual(){
        if(currentSession==null)return;
        int row=attendance.getSelectedRow();
        if(row<0){javax.swing.JOptionPane.showMessageDialog(this,"Select a student row first.");return;}
        var r=roster.get(attendance.convertRowIndexToModel(row));
        ManualAttendanceDialog.show(this,actor,tapin.dao.Sql.id(currentSession,"session_id"),selectedDate(),r,()->refresh());
    }
    private void showRoster(){
        javax.swing.JTable t=new javax.swing.JTable();var m=Style.table(t,"Student Name","RFID UID");
        for(var r:roster)m.addRow(new Object[]{r.get("student_name"),r.get("rfid_uid")});
        javax.swing.JFrame f=new javax.swing.JFrame("TapIn - Class Students");f.setDefaultCloseOperation(DISPOSE_ON_CLOSE);f.add(new javax.swing.JScrollPane(t));f.setSize(650,450);f.setLocationRelativeTo(this);showChild(f);
    }
    private void showSchedules(){
        javax.swing.JTable t=new javax.swing.JTable();var m=Style.table(t,"Section","Subject","Day","Start","End","Grace");
        for(var r:schedules)m.addRow(new Object[]{r.get("section_name"),r.get("subject_code"),r.get("day_of_week"),r.get("start_time"),r.get("end_time"),r.get("grace_minutes")});
        javax.swing.JFrame f=new javax.swing.JFrame("TapIn - My Schedules");f.setDefaultCloseOperation(DISPOSE_ON_CLOSE);f.add(new javax.swing.JScrollPane(t));f.setSize(850,450);f.setLocationRelativeTo(this);showChild(f);
    }
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        root = new javax.swing.JPanel();
        root.setBackground(new java.awt.Color(239,245,250));
        root.setPreferredSize(new java.awt.Dimension(1230,750));
        root.setLayout(new java.awt.BorderLayout(12,12));
        sidebar = new javax.swing.JPanel();
        sidebar.setBackground(new java.awt.Color(17,39,58));
        sidebar.setPreferredSize(new java.awt.Dimension(190,700));
        sidebar.setLayout(new java.awt.BorderLayout(12,12));
        logo = new javax.swing.JLabel();
        logo.setText("TapIn");
        logo.setFont(new java.awt.Font("Segoe UI", 0, 14));
        logo.setPreferredSize(new java.awt.Dimension(180,120));
        logo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        sidebar.add(logo, java.awt.BorderLayout.NORTH);
        nav = new javax.swing.JPanel();
        nav.setLayout(new java.awt.GridLayout(0,1,12,12));
        dashboardNav = new javax.swing.JButton();
        dashboardNav.setText("Dashboard");
        nav.add(dashboardNav);
        attendanceNav = new javax.swing.JButton();
        attendanceNav.setText("Attendance / Kiosk");
        nav.add(attendanceNav);
        studentsNav = new javax.swing.JButton();
        studentsNav.setText("Students");
        nav.add(studentsNav);
        schedulesNav = new javax.swing.JButton();
        schedulesNav.setText("Schedules");
        nav.add(schedulesNav);
        reportsNav = new javax.swing.JButton();
        reportsNav.setText("Reports");
        nav.add(reportsNav);
        logout = new javax.swing.JButton();
        logout.setText("Logout");
        nav.add(logout);
        sidebar.add(nav, java.awt.BorderLayout.CENTER);
        root.add(sidebar, java.awt.BorderLayout.WEST);
        main = new javax.swing.JPanel();
        main.setBackground(new java.awt.Color(255,255,255));
        main.setLayout(new java.awt.BorderLayout(12,12));
        top = new javax.swing.JPanel();
        top.setLayout(new java.awt.GridLayout(0,1,12,12));
        heading = new javax.swing.JLabel();
        heading.setText("Attendance Dashboard");
        heading.setFont(new java.awt.Font("Segoe UI", 0, 28));
        top.add(heading);
        description = new javax.swing.JLabel();
        description.setText("View attendance for your selected section and schedule.");
        description.setFont(new java.awt.Font("Segoe UI", 0, 14));
        top.add(description);
        selectors = new javax.swing.JPanel();
        selectors.setLayout(new java.awt.GridLayout(1,6,12,12));
        sectionLabel = new javax.swing.JLabel();
        sectionLabel.setText("Section");
        sectionLabel.setFont(new java.awt.Font("Segoe UI", 0, 14));
        selectors.add(sectionLabel);
        section = new javax.swing.JComboBox<>();
        selectors.add(section);
        subjectLabel = new javax.swing.JLabel();
        subjectLabel.setText("Subject");
        subjectLabel.setFont(new java.awt.Font("Segoe UI", 0, 14));
        selectors.add(subjectLabel);
        subject = new javax.swing.JComboBox<>();
        selectors.add(subject);
        dateLabel = new javax.swing.JLabel();
        dateLabel.setText("Date");
        dateLabel.setFont(new java.awt.Font("Segoe UI", 0, 14));
        selectors.add(dateLabel);
        date = new javax.swing.JSpinner();
        selectors.add(date);
        top.add(selectors);
        sessionActions = new javax.swing.JPanel();
        sessionActions.setLayout(new java.awt.GridLayout(1,3,12,12));
        schedule = new javax.swing.JComboBox<>();
        sessionActions.add(schedule);
        start = new javax.swing.JButton();
        start.setText("Start Session");
        sessionActions.add(start);
        close = new javax.swing.JButton();
        close.setText("Close Session");
        sessionActions.add(close);
        top.add(sessionActions);
        summary = new javax.swing.JPanel();
        summary.setLayout(new java.awt.GridLayout(1,4,12,12));
        total = new javax.swing.JLabel();
        total.setText("Total: 0");
        total.setFont(new java.awt.Font("Segoe UI", 0, 22));
        total.setBackground(new java.awt.Color(231,242,255));
        summary.add(total);
        present = new javax.swing.JLabel();
        present.setText("Present: 0");
        present.setFont(new java.awt.Font("Segoe UI", 0, 22));
        present.setForeground(new java.awt.Color(0,128,82));
        summary.add(present);
        late = new javax.swing.JLabel();
        late.setText("Late: 0");
        late.setFont(new java.awt.Font("Segoe UI", 0, 22));
        late.setForeground(new java.awt.Color(185,116,0));
        summary.add(late);
        absent = new javax.swing.JLabel();
        absent.setText("Absent: 0");
        absent.setFont(new java.awt.Font("Segoe UI", 0, 22));
        absent.setForeground(new java.awt.Color(195,45,55));
        summary.add(absent);
        top.add(summary);
        sessionStatus = new javax.swing.JLabel();
        sessionStatus.setText("Session: NOT STARTED   |   Grace period");
        sessionStatus.setFont(new java.awt.Font("Segoe UI", 0, 16));
        top.add(sessionStatus);
        searchPanel = new javax.swing.JPanel();
        searchPanel.setLayout(new java.awt.BorderLayout(12,12));
        searchLabel = new javax.swing.JLabel();
        searchLabel.setText("Search student");
        searchLabel.setFont(new java.awt.Font("Segoe UI", 0, 14));
        searchPanel.add(searchLabel, java.awt.BorderLayout.WEST);
        search = new javax.swing.JTextField();
        search.setColumns(24);
        searchPanel.add(search, java.awt.BorderLayout.CENTER);
        top.add(searchPanel);
        main.add(top, java.awt.BorderLayout.NORTH);
        scroll = new javax.swing.JScrollPane();
        attendance = new javax.swing.JTable();
        scroll.setViewportView(attendance);
        main.add(scroll, java.awt.BorderLayout.CENTER);
        actions = new javax.swing.JPanel();
        actions.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT,10,8));
        manual = new javax.swing.JButton();
        manual.setText("Manual Attendance");
        actions.add(manual);
        refresh = new javax.swing.JButton();
        refresh.setText("Refresh");
        actions.add(refresh);
        export = new javax.swing.JButton();
        export.setText("Export CSV");
        actions.add(export);
        legend = new javax.swing.JLabel();
        legend.setText("Present = on time; Late shown separately. Pending until close.");
        legend.setFont(new java.awt.Font("Segoe UI", 0, 12));
        actions.add(legend);
        main.add(actions, java.awt.BorderLayout.SOUTH);
        root.add(main, java.awt.BorderLayout.CENTER);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("TapIn - Teacher Dashboard");
        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(root, java.awt.BorderLayout.CENTER);
        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel root;
    private javax.swing.JPanel sidebar;
    private javax.swing.JLabel logo;
    private javax.swing.JPanel nav;
    private javax.swing.JButton dashboardNav;
    private javax.swing.JButton attendanceNav;
    private javax.swing.JButton studentsNav;
    private javax.swing.JButton schedulesNav;
    private javax.swing.JButton reportsNav;
    private javax.swing.JButton logout;
    private javax.swing.JPanel main;
    private javax.swing.JPanel top;
    private javax.swing.JLabel heading;
    private javax.swing.JLabel description;
    private javax.swing.JPanel selectors;
    private javax.swing.JLabel sectionLabel;
    private javax.swing.JComboBox<Object> section;
    private javax.swing.JLabel subjectLabel;
    private javax.swing.JComboBox<Object> subject;
    private javax.swing.JLabel dateLabel;
    private javax.swing.JSpinner date;
    private javax.swing.JPanel sessionActions;
    private javax.swing.JComboBox<Object> schedule;
    private javax.swing.JButton start;
    private javax.swing.JButton close;
    private javax.swing.JPanel summary;
    private javax.swing.JLabel total;
    private javax.swing.JLabel present;
    private javax.swing.JLabel late;
    private javax.swing.JLabel absent;
    private javax.swing.JLabel sessionStatus;
    private javax.swing.JPanel searchPanel;
    private javax.swing.JLabel searchLabel;
    private javax.swing.JTextField search;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable attendance;
    private javax.swing.JPanel actions;
    private javax.swing.JButton manual;
    private javax.swing.JButton refresh;
    private javax.swing.JButton export;
    private javax.swing.JLabel legend;
    // End of variables declaration//GEN-END:variables
}
