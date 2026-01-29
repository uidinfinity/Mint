package net.melbourne.modules.impl.player;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;

@FeatureInfo(name = "Swing", category = Category.Player)
public class SwingFeature extends Feature {

    public ModeSetting hand = new ModeSetting("Hand", "The hand that will be used for swinging.", "Default", new String[]{"Default", "None", "Packet", "Mainhand", "Offhand", "Both"});
    public BooleanSetting noPacket = new BooleanSetting("NoPacket", "Only swings clientside.", false,
            () -> !hand.getValue().equalsIgnoreCase("None") && !hand.getValue().equalsIgnoreCase("Packet"));

    public BooleanSetting modifySpeed = new BooleanSetting("ModifySpeed", "Modify the swing animation speed.", false);
    public NumberSetting speed = new NumberSetting("Speed", "Swing speed value.", 15, 1, 20,
            () -> modifySpeed.getValue());

    public BooleanSetting translations = new BooleanSetting("Translations", "Enable custom swing translations.", false);
    public NumberSetting translateX = new NumberSetting("TranslateX", "X offset.", -0.4f, -2.0f, 2.0f, () -> translations.getValue());
    public NumberSetting translateY = new NumberSetting("TranslateY", "Y offset.", 0.2f, -2.0f, 2.0f, () -> translations.getValue());
    public NumberSetting translateZ = new NumberSetting("TranslateZ", "Z offset.", -0.2f, -2.0f, 2.0f, () -> translations.getValue());

    public BooleanSetting rotations = new BooleanSetting("Rotations", "Enable custom swing rotations.", false);
    public NumberSetting rotationX = new NumberSetting("RotationX", "X rotation angle.", -80.0f, -180.0f, 180.0f, () -> rotations.getValue());
    public NumberSetting rotationY = new NumberSetting("RotationY", "Y rotation angle.", -45.0f, -180.0f, 180.0f, () -> rotations.getValue());
    public NumberSetting rotationZ = new NumberSetting("RotationZ", "Z rotation angle.", -20.0f, -180.0f, 180.0f, () -> rotations.getValue());

}