package net.melbourne.modules.impl.render;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.RenderHudEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ColorSetting;   
import net.melbourne.utils.miscellaneous.WaypointUtils;

import java.awt.*;

@FeatureInfo(name = "Waypoints", category = Category.Render)
public class WaypointsFeature extends Feature {
    public BooleanSetting Logouts = new BooleanSetting("Logouts", "Shows logout spots.", true);
    public BooleanSetting Pings = new BooleanSetting("Pings", "Shows ping spots.", true);
    public BooleanSetting pingMintNames = new BooleanSetting("Telemetry", "Shows mint username instead of minecraft username for pings.", false);
    public BooleanSetting showHealth = new BooleanSetting("Health", "Shows health on logout nametag.", true);
    public BooleanSetting showDistance = new BooleanSetting("Distance", "Shows distance to logout spot.", true);
    public BooleanSetting showTotemPops = new BooleanSetting("Pops", "Shows totem pops on logout nametag.", true);
    public ColorSetting Fill = new ColorSetting("Fill", "The color used for the box fill.", new Color(255, 0, 0, 50));
    public ColorSetting Outline = new ColorSetting("Outline", "The color used for the box and nametag outline.", new Color(255, 0, 0, 255));

    @Override
    public void onEnable() {
        WaypointUtils.clearSpots();
    }

    @Override
    public void onDisable() {
        WaypointUtils.clearSpots();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        WaypointUtils.onTick(event, Logouts.getValue(), Pings.getValue());
    }

    @SubscribeEvent
    public void onPacket(PacketReceiveEvent event) {
        WaypointUtils.onPacket(event, Logouts.getValue());
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        WaypointUtils.onRenderWorld(event, Logouts.getValue(), Pings.getValue(), Fill.getColor(), Outline.getColor());
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderHudEvent event) {
        WaypointUtils.onRenderOverlay(
                event,
                Logouts.getValue(),
                Pings.getValue(),
                showHealth.getValue(),
                showDistance.getValue(),
                showTotemPops.getValue(),
                pingMintNames.getValue(),
                Outline.getColor()
        );
    }
}
