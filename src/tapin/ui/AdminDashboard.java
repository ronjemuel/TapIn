package tapin.ui;

public class AdminDashboard extends javax.swing.JFrame {
    private tapin.model.Account actor;
    private final java.util.List<java.awt.Window> children=new java.util.ArrayList<>();
    public AdminDashboard(){initComponents();Style.frame(this,root,logo,200);}
    public AdminDashboard(tapin.model.Account account){
        this();tapin.service.AuthService.requireAdmin(account);actor=account;heading.setText("Administrator - "+account.fullName());
        students.addActionListener(e->open(tapin.model.Entity.STUDENTS));accounts.addActionListener(e->open(tapin.model.Entity.ACCOUNTS));
        sections.addActionListener(e->open(tapin.model.Entity.SECTIONS));subjects.addActionListener(e->open(tapin.model.Entity.SUBJECTS));schedules.addActionListener(e->open(tapin.model.Entity.SCHEDULES));
        reports.addActionListener(e->{var f=new ReportsFrame(actor);children.add(f);f.setVisible(true);});
        logout.addActionListener(e->{if(Style.confirm(this,"Log out of TapIn?")){new StaffLogin().setVisible(true);dispose();}});
        addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){for(var w:children)w.dispose();}});
    }
    private void open(tapin.model.Entity entity){var f=new ManagementFrame(actor,entity);children.add(f);f.setVisible(true);}

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        root = new javax.swing.JPanel();
        root.setBackground(new java.awt.Color(239,245,250));
        root.setPreferredSize(new java.awt.Dimension(900,620));
        root.setLayout(new java.awt.BorderLayout(12,12));
        brand = new javax.swing.JPanel();
        brand.setBackground(new java.awt.Color(17,39,58));
        brand.setPreferredSize(new java.awt.Dimension(230,560));
        brand.setLayout(new java.awt.BorderLayout(12,12));
        logo = new javax.swing.JLabel();
        logo.setText("TapIn");
        logo.setFont(new java.awt.Font("Segoe UI", 0, 14));
        logo.setPreferredSize(new java.awt.Dimension(220,140));
        logo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        brand.add(logo, java.awt.BorderLayout.NORTH);
        root.add(brand, java.awt.BorderLayout.WEST);
        main = new javax.swing.JPanel();
        main.setLayout(new java.awt.GridLayout(0,1,12,12));
        heading = new javax.swing.JLabel();
        heading.setText("Administrator");
        heading.setFont(new java.awt.Font("Segoe UI", 0, 30));
        main.add(heading);
        description = new javax.swing.JLabel();
        description.setText("Manage students, staff, and class schedules.");
        description.setFont(new java.awt.Font("Segoe UI", 0, 14));
        main.add(description);
        students = new javax.swing.JButton();
        students.setText("Manage Students");
        main.add(students);
        accounts = new javax.swing.JButton();
        accounts.setText("Manage Accounts");
        main.add(accounts);
        sections = new javax.swing.JButton();
        sections.setText("Manage Sections");
        main.add(sections);
        subjects = new javax.swing.JButton();
        subjects.setText("Manage Subjects");
        main.add(subjects);
        schedules = new javax.swing.JButton();
        schedules.setText("Manage Schedules");
        main.add(schedules);
        reports = new javax.swing.JButton();
        reports.setText("Attendance Reports");
        main.add(reports);
        logout = new javax.swing.JButton();
        logout.setText("Logout");
        main.add(logout);
        root.add(main, java.awt.BorderLayout.CENTER);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("TapIn - Administrator");
        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(root, java.awt.BorderLayout.CENTER);
        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel root;
    private javax.swing.JPanel brand;
    private javax.swing.JLabel logo;
    private javax.swing.JPanel main;
    private javax.swing.JLabel heading;
    private javax.swing.JLabel description;
    private javax.swing.JButton students;
    private javax.swing.JButton accounts;
    private javax.swing.JButton sections;
    private javax.swing.JButton subjects;
    private javax.swing.JButton schedules;
    private javax.swing.JButton reports;
    private javax.swing.JButton logout;
    // End of variables declaration//GEN-END:variables
}

