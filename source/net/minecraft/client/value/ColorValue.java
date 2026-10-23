package net.minecraft.client.value;

import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public class ColorValue extends Value<Integer> {
    private boolean rainbow;
    private float rainbowSpeed;
    private long rainbowOffset;

    public ColorValue(String name, String description, int defaultColor) {
        super(name, description, defaultColor);
        this.rainbow = false;
        this.rainbowSpeed = 1.0f;
        this.rainbowOffset = System.currentTimeMillis();
    }

    public ColorValue(String name, String description, int defaultColor, boolean rainbow, float rainbowSpeed) {
        super(name, description, defaultColor);
        this.rainbow = rainbow;
        this.rainbowSpeed = rainbowSpeed;
        this.rainbowOffset = System.currentTimeMillis();
    }

    public boolean isRainbow() {
        return rainbow;
    }

    public void setRainbow(boolean rainbow) {
        this.rainbow = rainbow;
    }

    public float getRainbowSpeed() {
        return rainbowSpeed;
    }

    public void setRainbowSpeed(float speed) {
        this.rainbowSpeed = speed;
    }

    public int getColor() {
        if (rainbow) {
            long time = System.currentTimeMillis() - rainbowOffset;
            float hue = ((time % 10000) / 10000.0f) * rainbowSpeed;
            return ARGB.colorFromHSV(hue, 1.0f, 1.0f);
        }
        return getValue();
    }

    public int getAlpha() {
        return ARGB.alpha(getValue());
    }

    public void setAlpha(int alpha) {
        int rgb = getValue() & 0x00FFFFFF;
        setValue(ARGB.color(alpha, rgb));
    }

    public float[] getHSV() {
        int color = getValue();
        float r = ARGB.red(color) / 255.0f;
        float g = ARGB.green(color) / 255.0f;
        float b = ARGB.blue(color) / 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h = 0;
        if (delta != 0) {
            if (max == r) h = (g - b) / delta % 6;
            else if (max == g) h = (b - r) / delta + 2;
            else h = (r - g) / delta + 4;
            h /= 6;
            if (h < 0) h += 1;
        }

        float s = max == 0 ? 0 : delta / max;
        float v = max;

        return new float[]{h, s, v};
    }

    public void setHSV(float h, float s, float v) {
        int r, g, b;
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);

        switch (i % 6) {
            case 0: r = (int) (v * 255); g = (int) (t * 255); b = (int) (p * 255); break;
            case 1: r = (int) (q * 255); g = (int) (v * 255); b = (int) (p * 255); break;
            case 2: r = (int) (p * 255); g = (int) (v * 255); b = (int) (t * 255); break;
            case 3: r = (int) (p * 255); g = (int) (q * 255); b = (int) (v * 255); break;
            case 4: r = (int) (t * 255); g = (int) (p * 255); b = (int) (v * 255); break;
            case 5: r = (int) (v * 255); g = (int) (p * 255); b = (int) (q * 255); break;
            default: throw new RuntimeException("Something went wrong when converting HSV to RGB");
        }

        int alpha = getAlpha();
        setValue(ARGB.color(alpha, r, g, b));
    }

    @Override
    public Component getDisplayValue() {
        return Component.literal(rainbow ? "Rainbow" : String.format("#%06X", getValue() & 0xFFFFFF));
    }

    @Override
    public void cycleForward() {
        float[] hsv = getHSV();
        setHSV((hsv[0] + 0.1f) % 1.0f, hsv[1], hsv[2]);
    }

    @Override
    public void cycleBackward() {
        float[] hsv = getHSV();
        setHSV((hsv[0] - 0.1f + 1.0f) % 1.0f, hsv[1], hsv[2]);
    }

    public static ColorValue createDefault(String name, String description) {
        return new ColorValue(name, description, 0xFFFFFFFF);
    }
}