import javax.sound.sampled.*;
import java.util.Random;

public class GeigerSound {
    private static final int SAMPLE_RATE   = 22050;
    private static final int CLICK_SAMPLES = 176;    // ~8ms na jeden puls
    private static final float TAU         = 22.0f;  // decay konstanta (mensi = ostrejsi klik)

    private byte[] clickData = null;
    private AudioFormat fmt  = null;
    private boolean available = false;

    public GeigerSound() {
        try {
            fmt = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            clickData = buildClickWave();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            available = AudioSystem.isLineSupported(info);
        } catch (Throwable t) {
            available = false;  // CrEme bez Sound API → fallback na beep()
        }
    }

    public boolean isAvailable() { return available; }

    public void playClick() {
        if (!available) return;
        final byte[] data   = clickData;
        final AudioFormat f = fmt;
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, f);
                    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(f, data.length * 2);
                    line.start();
                    line.write(data, 0, data.length);
                    line.drain();
                    line.close();
                } catch (Throwable ex) { /* silent */ }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static byte[] buildClickWave() {
        // Dvojity klik "tek-tik" (~16ms celkem)
        int total = CLICK_SAMPLES * 2 + 8;
        byte[] buf = new byte[total];
        Random rng = new Random(42);
        for (int i = 0; i < CLICK_SAMPLES; i++) {         // prvni "tek" (hlasitejsi)
            double noise = rng.nextDouble() * 2.0 - 1.0;
            buf[i] = (byte)(noise * Math.exp(-i / TAU) * 110.0);
        }
        // 8 vzorku ticha (~0.4ms pauza)
        for (int i = 0; i < CLICK_SAMPLES; i++) {         // druhy "tik" (tiss, ostrejsi)
            double noise = rng.nextDouble() * 2.0 - 1.0;
            buf[CLICK_SAMPLES + 8 + i] = (byte)(noise * Math.exp(-i / (TAU * 0.7)) * 70.0);
        }
        return buf;
    }
}
