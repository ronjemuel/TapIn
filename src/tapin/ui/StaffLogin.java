package tapin.ui;

public class StaffLogin extends javax.swing.JFrame {
    private String role = "TEACHER";
    private boolean busy;
    public StaffLogin() {
        initComponents();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        Style.frame(this,root,logo,350);
        formPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(16,24,16,24));
        message.setFont(new java.awt.Font("Segoe UI",java.awt.Font.PLAIN,12));
        Style.primary(login); chooseRole("TEACHER");
        teacher.addActionListener(e->chooseRole("TEACHER"));
        admin.addActionListener(e->chooseRole("ADMIN"));
        char echo=password.getEchoChar();
        showPassword.addActionListener(e->password.setEchoChar(showPassword.isSelected()?(char)0:echo));
        clear.addActionListener(e->{username.setText("");password.setText("");showPassword.setSelected(false);password.setEchoChar(echo);message.setText(" ");username.requestFocusInWindow();});
        login.addActionListener(e->signIn());
        password.addActionListener(e->signIn());
        kiosk.addActionListener(e->new AttendanceKiosk().setVisible(true));
        getRootPane().setDefaultButton(login);
    }
    private void chooseRole(String value) {
        role=value;
        teacher.setBackground(value.equals("TEACHER")?Style.BLUE:Style.BG);
        admin.setBackground(value.equals("ADMIN")?Style.BLUE:Style.BG);
        teacher.setForeground(value.equals("TEACHER")?java.awt.Color.WHITE:Style.NAVY);
        admin.setForeground(value.equals("ADMIN")?java.awt.Color.WHITE:Style.NAVY);
    }
    private void signIn() {
        if(busy)return;
        String user=username.getText();char[] pass=password.getPassword();String chosen=role;
        busy=true;login.setEnabled(false);message.setText("Connecting...");
        new javax.swing.SwingWorker<tapin.model.Account,Void>() {
            protected tapin.model.Account doInBackground() throws Exception {return new tapin.service.AuthService().login(user,pass,chosen);}
            protected void done() {
                busy=false;login.setEnabled(true);password.setText("");
                try {
                    tapin.model.Account account=get();
                    if(account.admin())new AdminDashboard(account).setVisible(true);else new TeacherDashboard(account).setVisible(true);
                    dispose();
                } catch(Exception ex){Throwable t=ex.getCause()==null?ex:ex.getCause();message.setText(Style.html(Style.message(t),320));message.setToolTipText(Style.message(t));}
            }
        }.execute();
    }
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        root = new javax.swing.JPanel();
        root.setBackground(new java.awt.Color(239,245,250));
        root.setPreferredSize(new java.awt.Dimension(980,640));
        root.setLayout(new java.awt.BorderLayout(12,12));
        brand = new javax.swing.JPanel();
        brand.setBackground(new java.awt.Color(0,0,0));
        brand.setPreferredSize(new java.awt.Dimension(410,550));
        brand.setLayout(new java.awt.BorderLayout(12,12));
        logo = new javax.swing.JLabel();
        logo.setText("TapIn");
        logo.setFont(new java.awt.Font("Segoe UI", 0, 14));
        logo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        brand.add(logo, java.awt.BorderLayout.CENTER);
        root.add(brand, java.awt.BorderLayout.WEST);
        formPanel = new javax.swing.JPanel();
        formPanel.setBackground(new java.awt.Color(255,255,255));
        formPanel.setLayout(new java.awt.GridLayout(0,1,12,12));
        heading = new javax.swing.JLabel();
        heading.setText("Welcome back");
        heading.setFont(new java.awt.Font("Segoe UI", 0, 36));
        formPanel.add(heading);
        subtitle = new javax.swing.JLabel();
        subtitle.setText("Choose your role and enter your account credentials.");
        subtitle.setFont(new java.awt.Font("Segoe UI", 0, 14));
        formPanel.add(subtitle);
        roles = new javax.swing.JPanel();
        roles.setLayout(new java.awt.GridLayout(1,2,12,12));
        teacher = new javax.swing.JButton();
        teacher.setText("Teacher");
        roles.add(teacher);
        admin = new javax.swing.JButton();
        admin.setText("Administrator");
        roles.add(admin);
        formPanel.add(roles);
        usernameLabel = new javax.swing.JLabel();
        usernameLabel.setText("Username");
        usernameLabel.setFont(new java.awt.Font("Segoe UI", 0, 14));
        formPanel.add(usernameLabel);
        username = new javax.swing.JTextField();
        username.setColumns(22);
        formPanel.add(username);
        passwordLabel = new javax.swing.JLabel();
        passwordLabel.setText("Password");
        passwordLabel.setFont(new java.awt.Font("Segoe UI", 0, 14));
        formPanel.add(passwordLabel);
        password = new javax.swing.JPasswordField();
        password.setColumns(22);
        formPanel.add(password);
        showPassword = new javax.swing.JCheckBox();
        showPassword.setText("Show password");
        formPanel.add(showPassword);
        actions = new javax.swing.JPanel();
        actions.setLayout(new java.awt.GridLayout(1,2,12,12));
        login = new javax.swing.JButton();
        login.setText("Log In");
        actions.add(login);
        clear = new javax.swing.JButton();
        clear.setText("Clear");
        actions.add(clear);
        formPanel.add(actions);
        kiosk = new javax.swing.JButton();
        kiosk.setText("Open RFID Kiosk");
        kiosk.setFont(new java.awt.Font("Segoe UI", 0, 13));
        formPanel.add(kiosk);
        message = new javax.swing.JLabel();
        message.setText(" ");
        message.setFont(new java.awt.Font("Segoe UI", 0, 14));
        formPanel.add(message);
        root.add(formPanel, java.awt.BorderLayout.CENTER);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("TapIn - Staff Login");
        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(root, java.awt.BorderLayout.CENTER);
        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel root;
    private javax.swing.JPanel brand;
    private javax.swing.JLabel logo;
    private javax.swing.JPanel formPanel;
    private javax.swing.JLabel heading;
    private javax.swing.JLabel subtitle;
    private javax.swing.JPanel roles;
    private javax.swing.JButton teacher;
    private javax.swing.JButton admin;
    private javax.swing.JLabel usernameLabel;
    private javax.swing.JTextField username;
    private javax.swing.JLabel passwordLabel;
    private javax.swing.JPasswordField password;
    private javax.swing.JCheckBox showPassword;
    private javax.swing.JPanel actions;
    private javax.swing.JButton login;
    private javax.swing.JButton clear;
    private javax.swing.JButton kiosk;
    private javax.swing.JLabel message;
    // End of variables declaration//GEN-END:variables
}
