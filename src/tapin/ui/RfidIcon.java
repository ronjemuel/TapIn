package tapin.ui;

import java.awt.*;
import javax.swing.Icon;

/** Small vector card symbol; branding always uses the supplied PNG. */
public final class RfidIcon implements Icon {
    public int getIconWidth(){return 76;}
    public int getIconHeight(){return 76;}
    public void paintIcon(Component c,Graphics graphics,int x,int y){
        Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(230,244,255));g.fillOval(0,0,76,76);
        g.setStroke(new BasicStroke(3.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.setColor(Style.NAVY);g.drawRoundRect(13,27,32,25,6,6);g.drawLine(32,34,38,34);
        g.setColor(Style.BLUE);g.drawArc(39,26,17,29,-55,110);g.drawArc(39,18,29,45,-55,110);g.dispose();
    }
}
