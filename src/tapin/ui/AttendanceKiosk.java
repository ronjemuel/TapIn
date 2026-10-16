package tapin.ui;

public class AttendanceKiosk extends javax.swing.JFrame {
    private final tapin.service.AttendanceService service=new tapin.service.AttendanceService();
    private javax.swing.Timer resetTimer, statusTimer;
    private boolean busy, checking;
    public AttendanceKiosk() {
        initComponents(); Style.frame(this,root,logo,210);
        cardArea.setText("");cardArea.setIcon(new RfidIcon());
        uid.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        uid.setFont(new java.awt.Font("Segoe UI",java.awt.Font.PLAIN,24));
        uid.addActionListener(e->scan());
        resetTimer=new javax.swing.Timer(5000,e->{result.setText("Ready for your card");readerStatus.setText("Reader status: waiting for keyboard / HID input");});
        resetTimer.setRepeats(false);
        statusTimer=new javax.swing.Timer(4000,e->checkStatus());statusTimer.start();checkStatus();
        addWindowFocusListener(new java.awt.event.WindowAdapter(){public void windowGainedFocus(java.awt.event.WindowEvent e){uid.requestFocusInWindow();}});
        addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){statusTimer.stop();resetTimer.stop();}});
    }
    private void checkStatus(){
        if(checking)return;checking=true;
        new javax.swing.SwingWorker<Integer,Void>(){
            protected Integer doInBackground() throws Exception{return new tapin.dao.AttendanceDao().openCount();}
            protected void done(){checking=false;if(!isDisplayable())return;try{int n=get();sessionStatus.setText(n>0?"Kiosk ready | "+n+" open session(s) today":"No active session today");}catch(Exception e){sessionStatus.setText("Database unavailable - ask a staff member");}}
        }.execute();
    }
    private void scan(){
        String card=uid.getText();uid.setText("");
        if(busy||card.isBlank())return;
        busy=true;resetTimer.stop();readerStatus.setText("Reader status: processing card...");
        new javax.swing.SwingWorker<tapin.service.AttendanceService.TapResult,Void>(){
            protected tapin.service.AttendanceService.TapResult doInBackground() throws Exception{return service.tap(card);}
            protected void done(){busy=false;try{result.setText(Style.html(get().toString(),500));result.setForeground(new java.awt.Color(0,125,78));}
                catch(Exception e){result.setText(Style.html(Style.message(e.getCause()==null?e:e.getCause()),500));result.setForeground(new java.awt.Color(170,35,35));}
                result.setToolTipText(result.getText());readerStatus.setText("Reader status: waiting for next card");resetTimer.restart();uid.requestFocusInWindow();
            }
        }.execute();
    }
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        root = new javax.swing.JPanel();
        root.setBackground(new java.awt.Color(239,245,250));
        root.setPreferredSize(new java.awt.Dimension(1050,700));
        root.setLayout(new java.awt.BorderLayout(12,12));
        brand = new javax.swing.JPanel();
        brand.setBackground(new java.awt.Color(17,39,58));
        brand.setPreferredSize(new java.awt.Dimension(240,650));
        brand.setLayout(new java.awt.BorderLayout(12,12));
        logo = new javax.swing.JLabel();
        logo.setText("TapIn");
        logo.setFont(new java.awt.Font("Segoe UI", 0, 14));
        logo.setPreferredSize(new java.awt.Dimension(230,170));
        logo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        brand.add(logo, java.awt.BorderLayout.NORTH);
        root.add(brand, java.awt.BorderLayout.WEST);
        main = new javax.swing.JPanel();
        main.setBackground(new java.awt.Color(255,255,255));
        main.setLayout(new java.awt.GridLayout(0,1,12,12));
        sessionStatus = new javax.swing.JLabel();
        sessionStatus.setText("Checking sessions...");
        sessionStatus.setFont(new java.awt.Font("Segoe UI", 0, 18));
        sessionStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        main.add(sessionStatus);
        cardArea = new javax.swing.JLabel();
        cardArea.setText("RFID  )))");
        cardArea.setFont(new java.awt.Font("Segoe UI", 0, 58));
        cardArea.setForeground(new java.awt.Color(0,126,245));
        cardArea.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        main.add(cardArea);
        heading = new javax.swing.JLabel();
        heading.setText("Tap your RFID card to continue");
        heading.setFont(new java.awt.Font("Segoe UI", 0, 30));
        heading.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        main.add(heading);
        readerStatus = new javax.swing.JLabel();
        readerStatus.setText("Reader status: waiting for keyboard / HID input");
        readerStatus.setFont(new java.awt.Font("Segoe UI", 0, 16));
        readerStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        main.add(readerStatus);
        uid = new javax.swing.JTextField();
        uid.setColumns(22);
        main.add(uid);
        result = new javax.swing.JLabel();
        result.setText("Ready for your card");
        result.setFont(new java.awt.Font("Segoe UI", 0, 19));
        result.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        main.add(result);
        tip = new javax.swing.JLabel();
        tip.setText("Tip: your teacher must start a session first.");
        tip.setFont(new java.awt.Font("Segoe UI", 0, 15));
        tip.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        main.add(tip);
        inputTip = new javax.swing.JLabel();
        inputTip.setText("Keep this window focused. The reader sends UID + Enter.");
        inputTip.setFont(new java.awt.Font("Segoe UI", 0, 13));
        inputTip.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        main.add(inputTip);
        root.add(main, java.awt.BorderLayout.CENTER);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("TapIn - RFID Kiosk");
        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(root, java.awt.BorderLayout.CENTER);
        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel root;
    private javax.swing.JPanel brand;
    private javax.swing.JLabel logo;
    private javax.swing.JPanel main;
    private javax.swing.JLabel sessionStatus;
    private javax.swing.JLabel cardArea;
    private javax.swing.JLabel heading;
    private javax.swing.JLabel readerStatus;
    private javax.swing.JTextField uid;
    private javax.swing.JLabel result;
    private javax.swing.JLabel tip;
    private javax.swing.JLabel inputTip;
    // End of variables declaration//GEN-END:variables
}
