import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;
import javax.swing.*;

public class Kokotmetr extends JFrame implements KeyListener {

    private static final int STATE_IDLE     = 0;
    private static final int STATE_MEASURE  = 1;
    private static final int STATE_DONE     = 2;

    static class ProgressBar extends Canvas {
        private int value = 0;
        private long currentMs = 0;

        static final int MODE_LINKA  = 0;
        static final int MODE_RADIUS = 1;
        private int mode = MODE_RADIUS;

        void setMode(int m) { mode = m; repaint(); }
        int getMode() { return mode; }

        private Image meterImage = null;
        private boolean imageLoadAttempted = false;
        private Image radiusImage = null;
        private boolean radiusImageLoadAttempted = false;
        private Image offscreen = null;

        // Cache barev pro getBarColor (staticke hodnoty)
        private static final Color COLOR_GREEN  = new Color(0, 200, 0);
        private static final Color COLOR_PURPLE = new Color(180, 0, 200);

        // Souradnice pasu stupnice v obrazku (zlomky rozmeru, 1024x1024px)
        private static final double BAR_X_FRAC = 0.1074;
        private static final double BAR_Y_FRAC = 281.0 / 1024.0; // pixel 281 v 1024px obrazku
        private static final double BAR_W_FRAC = 0.7930;
        private static final double BAR_H_FRAC = (309.0 - 281.0) / 1024.0; // pixel 281-309

        // Kalibracni konstanty jehly (kokotmetr_radius.jpg, 1024x1024px)
        // pivot=(512,706), val0=(141,316), val100=(887,316)
        private static final double PIVOT_X_FRAC      = 0.500;
        private static final double PIVOT_Y_FRAC      = 0.690;
        private static final double NEEDLE_LEN_FRAC   = 0.527;
        private static final double NEEDLE_START_DEG  = 226.4; // atan2(-390,-371)
        private static final double NEEDLE_END_DEG    = 313.9; // atan2(-390, 375)
        private static final double NEEDLE_WOBBLE_DEG = 8.0;
        // Jehla se nevykresli pod touto Y-souradnici v obrazku (rozmazana cast)
        private static final double NEEDLE_CLIP_Y_FRAC = 620.0 / 1024.0;

        public void setValue(int v, long ms) { value = v; currentMs = ms; repaint(); }
        public void setValue(int v) { setValue(v, 0); }

        public Dimension getPreferredSize() {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            return new Dimension(screen.width, screen.width);
        }
        public Dimension getMinimumSize() { return new Dimension(100, 100); }
        public void update(Graphics g) { paint(g); }

        private void ensureRadiusImageLoaded() {
            if (radiusImageLoadAttempted) return;
            radiusImageLoadAttempted = true;
            try {
                InputStream is = Kokotmetr.class.getResourceAsStream("/kokotmetr_radius.jpg");
                if (is == null) return;
                byte[] buf = new byte[4096];
                int n;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
                is.close();
                Image img = Toolkit.getDefaultToolkit().createImage(baos.toByteArray());
                MediaTracker mt = new MediaTracker(this);
                mt.addImage(img, 0);
                mt.waitForAll();
                if (!mt.isErrorAny()) radiusImage = img;
            } catch (Exception ex) { /* fallback na plain bar */ }
        }

        private void ensureImageLoaded() {
            if (imageLoadAttempted) return;
            imageLoadAttempted = true;
            try {
                InputStream is = Kokotmetr.class.getResourceAsStream("/kokotmetr_linka.jpg");
                if (is == null) return;
                byte[] buf = new byte[4096];
                int n;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
                is.close();
                Image img = Toolkit.getDefaultToolkit().createImage(baos.toByteArray());
                MediaTracker mt = new MediaTracker(this);
                mt.addImage(img, 0);
                mt.waitForAll();
                if (!mt.isErrorAny()) meterImage = img;
            } catch (Exception ex) { /* fallback na plain bar */ }
        }

        public void paint(Graphics g) {
            int w = getSize().width;
            int h = getSize().height;

            if (offscreen == null || offscreen.getWidth(null) != w || offscreen.getHeight(null) != h) {
                if (offscreen != null) offscreen.flush();
                offscreen = createImage(w, h);
            }

            Graphics og = (offscreen != null) ? offscreen.getGraphics() : g;

            og.setColor(Color.black);
            og.fillRect(0, 0, w, h);

            if (mode == MODE_RADIUS) {
                ensureRadiusImageLoaded();
                paintRadius(og, w, h);
            } else {
                ensureImageLoaded();
                paintLinka(og, w, h);
            }

            Font vf = new Font("Dialog", Font.PLAIN, 10);
            og.setFont(vf);
            FontMetrics fm = og.getFontMetrics(vf);
            String vs = "v" + AppConfig.VERSION;
            og.setColor(new Color(100, 100, 100));
            og.drawString(vs, w - fm.stringWidth(vs) - 4, h - 4);

            if (offscreen != null) {
                g.drawImage(offscreen, 0, 0, this);
                og.dispose();
            }
        }

        private int[] drawScaledImage(Graphics og, Image img, int w, int h) {
            int imgX = 0, imgY = 0, imgW = w, imgH = h;
            if (img != null) {
                int natW = img.getWidth(null);
                int natH = img.getHeight(null);
                if (natW > 0 && natH > 0) {
                    if (w * natH <= h * natW) {
                        imgW = w; imgH = w * natH / natW;
                    } else {
                        imgH = h; imgW = h * natW / natH;
                    }
                    imgX = (w - imgW) / 2;
                    imgY = (h - imgH) / 2;
                }
                og.drawImage(img, imgX, imgY, imgW, imgH, this);
            } else {
                og.setColor(Color.darkGray);
                og.fillRect(0, 0, w, h);
            }
            return new int[]{imgX, imgY, imgW, imgH};
        }

        private void paintLinka(Graphics og, int w, int h) {
            int[] r = drawScaledImage(og, meterImage, w, h);
            int imgX = r[0], imgY = r[1], imgW = r[2], imgH = r[3];

            int bx = imgX + (int)(imgW * BAR_X_FRAC);
            int by = imgY + (int)(imgH * BAR_Y_FRAC);
            int bw = (int)(imgW * BAR_W_FRAC);
            int bh = Math.max(1, (int)(imgH * BAR_H_FRAC));

            int fillW = bw * value / 100;
            if (value > 0) {
                int wobble = (int)((Math.random() * 2 - 1) * bw / 6);
                fillW = Math.max(0, Math.min(bw, fillW + wobble));
            }
            if (fillW > 0) {
                og.setColor(paleBarColor(currentMs));
                og.fillRect(bx, by, fillW, bh);
            }
        }

        private void paintRadius(Graphics og, int w, int h) {
            int[] r = drawScaledImage(og, radiusImage, w, h);
            int imgX = r[0], imgY = r[1], imgW = r[2], imgH = r[3];

            double angle = NEEDLE_START_DEG + (NEEDLE_END_DEG - NEEDLE_START_DEG) * value / 100.0;
            if (value > 0) {
                angle += (Math.random() * 2 - 1) * NEEDLE_WOBBLE_DEG;
            }
            double rad = Math.toRadians(angle);
            int px = imgX + (int)(imgW * PIVOT_X_FRAC);
            int py = imgY + (int)(imgH * PIVOT_Y_FRAC);
            int nl = (int)(imgH * NEEDLE_LEN_FRAC);
            int nx = px + (int)(nl * Math.cos(rad));
            int ny = py + (int)(nl * Math.sin(rad));

            int clipH = (int)(imgH * NEEDLE_CLIP_Y_FRAC);
            Shape oldClip = og.getClip();
            og.setClip(imgX, imgY, imgW, clipH);
            og.setColor(getBarColor(currentMs));
            og.drawLine(px-1, py,   nx-1, ny);
            og.drawLine(px,   py,   nx,   ny);
            og.drawLine(px+1, py,   nx+1, ny);
            og.drawLine(px,   py-1, nx,   ny-1);
            og.drawLine(px,   py+1, nx,   ny+1);
            og.drawLine(px+1, py+1, nx+1, ny+1);
            og.setClip(oldClip);
        }

        static Color paleBarColor(long ms) {
            Color c = getBarColor(ms);
            return new Color(
                Math.min(255, c.getRed()   + 100),
                Math.min(255, c.getGreen() + 100),
                Math.min(255, c.getBlue()  + 100)
            );
        }

        static Color getBarColor(long ms) {
            if (ms < 1000) {
                return COLOR_GREEN;
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
                return COLOR_PURPLE;
            }
        }
    }

    private ProgressBar progressBar;
    private JLabel label;

    private int state = STATE_IDLE;
    private long startTime = 0;
    private Timer timer;
    private Random geigerRandom = new Random();
    private boolean soundEnabled = true;

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

        final JButton modeBtn = new JButton("LINKA");
        modeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (progressBar.getMode() == 0) {
                    progressBar.setMode(1);
                    modeBtn.setText("LINKA");
                } else {
                    progressBar.setMode(0);
                    modeBtn.setText("RADIUS");
                }
                progressBar.requestFocus();
            }
        });

        final JButton soundBtn = new JButton("MUTE");
        soundBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                soundEnabled = !soundEnabled;
                soundBtn.setText(soundEnabled ? "MUTE" : "UNMUTE");
                progressBar.requestFocus();
            }
        });

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(label, BorderLayout.CENTER);
        northPanel.add(modeBtn, BorderLayout.EAST);
        northPanel.add(soundBtn, BorderLayout.WEST);

        getContentPane().add(northPanel, BorderLayout.NORTH);
        getContentPane().add(progressBar, BorderLayout.CENTER);

        addKeyListener(this);
        progressBar.addKeyListener(this);
        modeBtn.addKeyListener(this);
        soundBtn.addKeyListener(this);
        progressBar.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Kokotmetr.this.requestFocus();
            }
        });
        // setFocusable - CrEme nepodporuje

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                timer.stop();
                System.exit(0);
            }
        });

        timer = new Timer(100, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (state == STATE_MEASURE && startTime != 0) {
                    long duration = System.currentTimeMillis() - startTime;
                    int value = (int)(duration * 100 / 10000); // 10s = plny bar
                    if (value > 100) value = 100;
                    progressBar.setValue(value, duration);
                    double prob = 0.05 + (value / 100.0) * 0.85;
                    if (soundEnabled && geigerRandom.nextDouble() < prob) {
                        Toolkit.getDefaultToolkit().beep();
                    }
                }
            }
        });

        timer.start();
    }

    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_1) {
            if (progressBar.getMode() == ProgressBar.MODE_LINKA) {
                progressBar.setMode(ProgressBar.MODE_RADIUS);
            } else {
                progressBar.setMode(ProgressBar.MODE_LINKA);
            }
            return;
        }
        if (k == KeyEvent.VK_3) {
            soundEnabled = !soundEnabled;
            return;
        }
        if (k == KeyEvent.VK_2 || k == KeyEvent.VK_UP || k == KeyEvent.VK_DOWN) {
            if (state == STATE_IDLE || state == STATE_DONE) {
                state = STATE_MEASURE;
                startTime = System.currentTimeMillis();
                progressBar.setValue(0);
                label.setText("Skenujes...");
                label.setForeground(Color.black);
            } else if (state == STATE_MEASURE) {
                long duration = System.currentTimeMillis() - startTime;
                state = STATE_DONE;
                startTime = 0;
                label.setText(getRating(duration));
                label.setForeground(ProgressBar.getBarColor(duration));
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
