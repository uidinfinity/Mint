package net.melbourne.engine.particle;

import net.melbourne.utils.graphics.api.WorldContext;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class Particles {
    public static final Particles INSTANCE = new Particles();
    private final List<Physics> particles = new CopyOnWriteArrayList<>();

    public void addParticle(Physics particle) {
        particles.add(particle);
    }

    public void update() {
        particles.removeIf(p -> p.dead);
        for (Physics p : particles) {
            p.update();
        }
    }

    public void render(WorldContext context) {
        for (Physics p : particles) {
            p.render(context);
        }
    }
}