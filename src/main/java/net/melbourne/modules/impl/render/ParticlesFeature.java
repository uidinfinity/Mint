package net.melbourne.modules.impl.render;

import net.melbourne.engine.particle.Particles;
import net.melbourne.engine.particle.Physics;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.*;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.*;
import net.minecraft.util.math.Vec3d;
import java.awt.Color;
import java.util.Random;

@FeatureInfo(name = "Particles", category = Category.Render)
public class ParticlesFeature extends Feature {
    public ModeSetting engineMode = new ModeSetting("Engine", "Particle engine to use", "Custom", new String[]{"Vanilla", "Custom"});
    public ModeSetting shape = new ModeSetting("Shape", "Particle shape", "Circle", new String[]{"Square", "Circle"}, () -> engineMode.getValue().equals("Custom"));
    public ModeSetting gravity = new ModeSetting("Gravity", "Physics mode", "Normal", new String[]{"Normal", "Drop"}, () -> engineMode.getValue().equals("Custom"));
    public BooleanSetting glow = new BooleanSetting("Glow", "Add glow effect", true, () -> engineMode.getValue().equals("Custom"));
    public NumberSetting scale = new NumberSetting("Scale", "Scale of particle effects", 1.0, 0.1, 5.0);
    public NumberSetting amount = new NumberSetting("Amount", "Particles per event", 15, 1, 100, () -> engineMode.getValue().equals("Custom"));
    public NumberSetting life = new NumberSetting("Life", "Ticks to live", 30, 5, 100, () -> engineMode.getValue().equals("Custom"));

    public final WhitelistSetting types = new WhitelistSetting("Types", "Particle types to customize", WhitelistSetting.Type.CUSTOM, new String[]{}, new String[]{"Totems", "Rockets", "Hitting", "Deaths"});

    public ColorSetting totemColor = new ColorSetting("TotemColor", "Color for totem pops", new Color(255, 255, 255), () -> types.getWhitelistIds().contains("Totems"));
    public ColorSetting rocketColor = new ColorSetting("RocketColor", "Color for rocket particles", new Color(219, 127, 255), () -> types.getWhitelistIds().contains("Rockets"));
    public ColorSetting hitColor = new ColorSetting("HitColor", "Color for hits", new Color(255, 0, 0), () -> types.getWhitelistIds().contains("Hitting"));
    public ColorSetting deathColor = new ColorSetting("DeathColor", "Color for deaths", new Color(255, 255, 255), () -> types.getWhitelistIds().contains("Deaths"));

    private final Random random = new Random();

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (engineMode.getValue().equals("Custom")) {
            Particles.INSTANCE.update();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (engineMode.getValue().equals("Custom")) {
            Particles.INSTANCE.render(event.getContext());
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (types.getWhitelistIds().contains("Hitting") && event.getPlayer() == mc.player) {
            if (engineMode.getValue().equals("Custom")) {
                spawn(event.getTarget().getPos().add(0, event.getTarget().getHeight() / 2.0, 0), hitColor.getColor());
            }
        }
    }

    @SubscribeEvent
    public void onPop(PlayerPopEvent event) {
        if (types.getWhitelistIds().contains("Totems")) {
            if (engineMode.getValue().equals("Custom")) {
                spawn(event.getPlayer().getPos().add(0, 1, 0), totemColor.getColor());
            }
        }
    }

    @SubscribeEvent
    public void onDeath(PlayerDeathEvent event) {
        if (types.getWhitelistIds().contains("Deaths")) {
            if (engineMode.getValue().equals("Custom")) {
                spawn(event.getPlayer().getPos().add(0, 1, 0), deathColor.getColor());
            }
        }
    }

    private void spawn(Vec3d pos, Color color) {
        for (int i = 0; i < amount.getValue().intValue(); i++) {
            Vec3d vel = new Vec3d((random.nextDouble() - 0.5) * 0.2, random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.2);
            Physics particle = new Physics(
                    pos, vel, life.getValue().intValue(), color,
                    scale.getValue().floatValue() * 0.15f, glow.getValue(),
                    shape.getValue(), gravity.getValue()
            );

            particle.setFadeDuration(300);
            Particles.INSTANCE.addParticle(particle);
        }
    }
}