package net.melbourne.engine.particle;

import lombok.Setter;
import net.melbourne.utils.Globals;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.graphics.api.WorldContext;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.awt.Color;
import java.util.Random;

public class Physics implements Globals {
    public Vec3d pos;
    public Vec3d velocity;
    public int age, maxAge;
    public Color color;
    public boolean dead, glow;
    public float size;
    public String shape, gravityMode;
    private final Random random = new Random();

    private long deathTime = -1;
    @Setter
    private float fadeDuration = 300f;
    private float renderAlpha = 1f;

    public Physics(Vec3d pos, Vec3d velocity, int maxAge, Color color, float size, boolean glow, String shape, String gravityMode) {
        this.pos = pos;
        this.velocity = velocity;
        this.maxAge = maxAge;
        this.color = color;
        this.size = size;
        this.glow = glow;
        this.shape = shape;
        this.gravityMode = gravityMode;
    }

    public void update() {
        if (age < maxAge) {
            age++;

            double gMult = gravityMode.equals("Drop") ? 0.008 : 0.003;
            velocity = velocity.subtract(0, gMult, 0);

            Vec3d next = pos.add(velocity);
            Box box = new Box(next.x - 0.01, next.y - 0.01, next.z - 0.01, next.x + 0.01, next.y + 0.01, next.z + 0.01);

            if (mc.world.getBlockCollisions(null, box).iterator().hasNext()) {
                if (gravityMode.equals("Drop")) {
                    if (random.nextFloat() < 0.45f) {
                        velocity = new Vec3d(velocity.x * 0.5, Math.abs(velocity.y) * 0.3, velocity.z * 0.5);
                    } else {
                        velocity = new Vec3d(0, 0, 0);
                    }
                } else {
                    velocity = new Vec3d(velocity.x * -0.6, velocity.y * -0.6, velocity.z * -0.6);
                }
            } else {
                pos = next;
            }

            if (!velocity.equals(Vec3d.ZERO)) {
                velocity = velocity.multiply(0.96);
            }
        } else {
            if (deathTime == -1) deathTime = System.currentTimeMillis();

            long elapsed = System.currentTimeMillis() - deathTime;
            float progress = Math.min(1f, elapsed / fadeDuration);

            renderAlpha = 1f - Easing.ease(progress, Easing.Method.LINEAR);

            if (progress >= 1f) {
                dead = true;
            }
        }
    }

    public void render(WorldContext context) {
        float lifeAlpha = 1.0f - ((float) age / maxAge);
        float finalAlpha = Math.max(0, lifeAlpha * renderAlpha);

        Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (finalAlpha * 255));

        if (shape.equals("Square")) {
            Renderer3D.renderParticleQuad(context, pos, size, c, glow);
        } else {
            Renderer3D.renderParticleCircle(context, pos, size, c, glow);
        }
    }
}