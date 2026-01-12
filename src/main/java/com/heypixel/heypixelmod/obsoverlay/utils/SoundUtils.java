package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.Naven;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * @Author：jiuxian_baka
 * @Date：2025/11/30 03:18
 * @Filename：SoundUtils
 */
public class SoundUtils {

    public static void playSound(String path, float volume) {
        if (!Naven.getInstance().canPlaySound) return;
        Multithreading.runAsync((() -> {
            try {
                BufferedInputStream bufferedInputStream = new BufferedInputStream(SoundUtils.class.getResourceAsStream("/assets/heypixel/VcX6svVqmeT8/sounds/" + path));
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(bufferedInputStream);

                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);

                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float range = gainControl.getMaximum() - gainControl.getMinimum();
                float gain = (range * volume) + gainControl.getMinimum();
                gainControl.setValue(gain);

                clip.start();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
            }
        }));
    }
}
