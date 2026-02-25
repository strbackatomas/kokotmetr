import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Kokotmetr extends JFrame implements KeyListener {

    private static final int STATE_IDLE     = 0;
    private static final int STATE_MEASURE  = 1;
    private static final int STATE_DONE     = 2;

    static class ProgressBar extends Canvas {
        private int value = 0;
        private long currentMs = 0;

        public void setValue(int v, long ms) { value = v; currentMs = ms; repaint(); }
        public void setValue(int v) { setValue(v, 0); }

        public Dimension getPreferredSize() { return new Dimension(100, 40); }
        public Dimension getMinimumSize() { return new Dimension(10, 40); }
        public void update(Graphics g) { paint(g); }

        public void paint(Graphics g) {
            int w = getSize().width;
            int h = getSize().height;
            g.setColor(Color.darkGray);
            g.fillRect(0, 0, w, h);
            int barW = w * value / 100;
            if (value > 0) {
                int wobble = (int)(Math.random() * 7) - 3;
                barW = Math.max(0, Math.min(w, barW + wobble));
            }
            g.setColor(getBarColor(currentMs));
            g.fillRect(0, 0, barW, h);
            g.setColor(Color.black);
            g.drawRect(0, 0, w - 1, h - 1);
        }

        private Color getBarColor(long ms) {
            if (ms < 1000) {
                return new Color(0, 200, 0);
            } else if (ms < 2000) {
                float t = (ms - 1000) / 1000f;
                return new Color((int)(t * 255), 200, 0);
            } else if (ms < 4000) {
                float t = (ms - 2000) / 2000f;
                return new Color(255, (int)(200 - t * 150), 0);
            } else if (ms < 6000) {
                float t = (ms - 4000) / 2000f;
                return new Color(255, (int)(50 - t * 50), 0);
            } else if (ms < 8000) {
                float t = (ms - 6000) / 2000f;
                return new Color((int)(255 - t * 75), 0, (int)(t * 200));
            } else {
                return new Color(180, 0, 200);
            }
        }
    }

    private ProgressBar progressBar;
    private JLabel label;

    private int state = STATE_IDLE;
    private long startTime = 0;
    private Timer timer;

    public Kokotmetr() {
        setTitle("KOKOTMETR");
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screen.width, screen.height);
        // setDefaultCloseOperation - CrEme nepodporuje
        // setUndecorated - CrEme nepodporuje

        getContentPane().setLayout(new BorderLayout());

        label = new JLabel("Skenu...", SwingConstants.CENTER);
        label.setFont(new Font("Dialog", Font.BOLD, 24));

        progressBar = new ProgressBar();

        getContentPane().add(label, BorderLayout.CENTER);
        getContentPane().add(progressBar, BorderLayout.SOUTH);

        addKeyListener(this);
        // setFocusable - CrEme nepodporuje

        timer = new Timer(100, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (state == STATE_MEASURE && startTime != 0) {
                    long duration = System.currentTimeMillis() - startTime;
                    int value = (int)(duration * 100 / 10000); // 10s = plny bar
                    if (value > 100) value = 100;
                    progressBar.setValue(value, duration);
                }
            }
        });

        timer.start();
    }

    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_2
                || k == KeyEvent.VK_UP || k == KeyEvent.VK_DOWN) {
            if (state == STATE_IDLE || state == STATE_DONE) {
                state = STATE_MEASURE;
                startTime = System.currentTimeMillis();
                progressBar.setValue(0);
                label.setText("Skenujes...");
            } else if (state == STATE_MEASURE) {
                long duration = System.currentTimeMillis() - startTime;
                state = STATE_DONE;
                startTime = 0;
                label.setText(getRating(duration));
                progressBar.setValue(0);
            }
        }
    }

    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {}

    private String getRating(long ms) {
        if (ms < 1000)  return "Normalni borec";
        if (ms < 2000)  return "Mirny kokot";
        if (ms < 4000)  return "Kokot";
        if (ms < 5000)  return "Tezky kokot";
        if (ms < 6000)  return "Kurevsky kokot";
        if (ms < 8000)  return "Pracurak";
        return "MEGAPRACURAK";
    }

    public static void main(String[] args) {
        try {
            Kokotmetr app = new Kokotmetr();
            app.setVisible(true);
            app.requestFocus();
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(null,
                t.getClass().getName() + ": " + t.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            t.printStackTrace();
        }
    }
}
