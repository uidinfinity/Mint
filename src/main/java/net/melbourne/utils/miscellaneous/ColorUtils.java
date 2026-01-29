package net.melbourne.utils.miscellaneous;

import net.melbourne.Managers;
import net.melbourne.modules.impl.client.ColorFeature;
import net.melbourne.settings.types.ColorSetting;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ColorUtils {

    private static final Map<String, Color> COLOR_NAMES = new HashMap<>();

    static {
        COLOR_NAMES.put("black", Color.BLACK);
        COLOR_NAMES.put("blue", Color.BLUE);
        COLOR_NAMES.put("cyan", Color.CYAN);
        COLOR_NAMES.put("darkgray", Color.DARK_GRAY);
        COLOR_NAMES.put("gray", Color.GRAY);
        COLOR_NAMES.put("green", Color.GREEN);
        COLOR_NAMES.put("lightgray", Color.LIGHT_GRAY);
        COLOR_NAMES.put("magenta", Color.MAGENTA);
        COLOR_NAMES.put("orange", Color.ORANGE);
        COLOR_NAMES.put("pink", Color.PINK);
        COLOR_NAMES.put("red", Color.RED);
        COLOR_NAMES.put("white", Color.WHITE);
        COLOR_NAMES.put("yellow", Color.YELLOW);
    }

    public static Color getGlobalColor() {
        ColorFeature cf = Managers.FEATURE.getFeatureFromClass(ColorFeature.class);

        if (cf.mode.equalsValue("Rainbow")) {
            return getRainbowColor(cf.rainbowSpeed.getValue().longValue(), cf.rainbowLength.getValue().longValue(), cf.rainbowSaturation.getValue().floatValue(), 0);
        }

        return cf.color.getColor();
    }

    public static Color getGlobalColor(int alpha) {
        Color c = getGlobalColor();
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public static Color getColor(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color getColorByName(String name) {
        return COLOR_NAMES.get(name.toLowerCase());
    }

    public static Color getOffsetWave(Color color, long index) {
        return getWave(color, 8L, 255, index);
    }

    public static Color getWave(Color color, long speed, int alpha, long index) {
        speed = Math.max(1, Math.min(speed, 20));
        float[] hsb = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
        float cycle = ((System.currentTimeMillis() + index) % (10500 - (500 * speed))) / (10500.0f - (500.0f * speed));
        float adjustedBrightness = Math.abs((cycle * 2.0f) % 2.0f - 1.0f);
        hsb[2] = 0.5F + 0.5F * adjustedBrightness;
        Color result = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
        return new Color(result.getRed(), result.getGreen(), result.getBlue(), alpha);
    }

    public static Color getTransitionColor(Color start, Color end, long index) {
        return getTransitionColor(start, end, 13L, 255, index);
    }

    public static Color getTransitionColor(Color start, Color end, long speed, int alpha, long index) {
        speed = Math.max(1, Math.min(speed, 20));
        float cycle = ((System.currentTimeMillis() + index) % (10500 - (500 * speed))) / (10500.0f - (500.0f * speed));
        float scale = Math.abs((cycle * 2.0f) % 2.0f - 1.0f);
        return transitionColor(start, end, scale, alpha);
    }

    public static Color transitionColor(Color start, Color end, float scale, int alpha) {
        int r = start.getRed() + (int) ((end.getRed() - start.getRed()) * scale);
        int g = start.getGreen() + (int) ((end.getGreen() - start.getGreen()) * scale);
        int b = start.getBlue() + (int) ((end.getBlue() - start.getBlue()) * scale);
        return new Color(r, g, b, alpha);
    }

    public static Color getRainbowColor(long speed, long length, float saturation, long index) {
        speed = Math.max(1, Math.min(speed, 20));
        length = Math.max(1, Math.min(length, 20));
        float cycle = ((System.currentTimeMillis() + index) % (10500 - (500 * speed))) / (10500f - (500f * speed));
        float hue = (cycle + (index / (float) (length * 50))) % 1.0f;
        return Color.getHSBColor(hue, saturation, 1f);
    }

    public static Color getRainbow(long speed, float saturation, float brightness, long index) {
        return getRainbow(speed, saturation, brightness, 255, index);
    }

    public static Color getRainbow(long speed, float saturation, float brightness, int alpha, long index) {
        speed = Math.max(1, Math.min(speed, 20));
        float cycle = ((System.currentTimeMillis() + index) % (10500 - (500 * speed))) / (10500f - (500f * speed));
        float hue = cycle % 1.0f;
        Color result = Color.getHSBColor(hue, saturation, brightness);
        return new Color(result.getRed(), result.getGreen(), result.getBlue(), alpha);
    }

    public static Color getRainbow(long speed, long length, float saturation, float brightness, long index) {
        return getRainbow(speed, length, saturation, brightness, 255, index);
    }

    public static Color getRainbow(long speed, long length, float saturation, float brightness, int alpha, long index) {
        speed = Math.max(1, Math.min(speed, 20));
        length = Math.max(1, Math.min(length, 20));
        float cycle = ((System.currentTimeMillis() + index) % (10500 - (500 * speed))) / (10500f - (500f * speed));
        float hue = (cycle + (index / (float) (length * 50))) % 1.0f;
        Color result = Color.getHSBColor(hue, saturation, brightness);
        return new Color(result.getRed(), result.getGreen(), result.getBlue(), alpha);
    }


    public static Formatting getHealthColor(double health) {
        if (health > 18.0) return Formatting.GREEN;
        else if (health > 16.0) return Formatting.DARK_GREEN;
        else if (health > 12.0) return Formatting.YELLOW;
        else if (health > 8.0) return Formatting.GOLD;
        else if (health > 5.0) return Formatting.RED;

        return Formatting.DARK_RED;
    }

    public static Formatting getTotemColor(int pops) {
        if (pops == 1) return Formatting.GREEN;
        else if (pops == 2) return Formatting.DARK_GREEN;
        else if (pops == 3) return Formatting.YELLOW;
        else if (pops == 4) return Formatting.GOLD;
        else if (pops == 5) return Formatting.RED;

        return Formatting.DARK_RED;
    }
}