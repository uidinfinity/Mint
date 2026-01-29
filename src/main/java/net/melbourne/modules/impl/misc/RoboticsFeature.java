package net.melbourne.modules.impl.misc;

import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PlayerUpdateEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.settings.types.TextSetting;

@FeatureInfo(name = "Robotics", category = Category.Misc)
public class RoboticsFeature extends Feature {
    public ModeSetting role = new ModeSetting("Role", "Role", "Host", new String[]{"Host", "Server"});
    public TextSetting hostIp = new TextSetting("HostIP", "Host IP", "127.0.0.1", () -> role.getValue().equals("Server"));
    public NumberSetting port = new NumberSetting("Port", "Port", 4444, 1024, 65535);

    @Override
    public String getInfo() {
        return role.getValue().equals("Host") ? "Host (local only)" : "Server (disabled)";
    }

    @Override
    public void onEnable() {

        Melbourne.getLogger().info("Robotics: Network functionality disabled for security");
    }

    @Override
    public void onDisable() {

    }

    public void syncToggle(Feature feature, boolean state) {

    }

    @SubscribeEvent
    public void onUpdate(PlayerUpdateEvent event) {

    }

    @SubscribeEvent
    public void onTravel(net.melbourne.events.impl.PlayerTravelEvent event) {

    }
}