package net.melbourne.utils.sounds;

import net.melbourne.Melbourne;
import org.apache.commons.io.IOUtils;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.InputStream;

public class SoundUtils {

    public static void playSound(String filename, int volume) {
        InputStream inputStream = null;

        try {
            inputStream = SoundUtils.class.getResourceAsStream("/assets/melbourne/sounds/" + filename);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
            Clip audioClip = AudioSystem.getClip();

            audioClip.open(audioInputStream);

            FloatControl gainControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);

            gainControl.setValue((((float) volume) * 40f / 100f) - 35f);
            audioClip.start();
        }
        catch (Exception e) {
            Melbourne.getLogger().atError();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

}