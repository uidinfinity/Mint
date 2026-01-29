package net.melbourne.modules.impl.client;

import net.melbourne.Melbourne;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import org.lwjgl.glfw.GLFW;

@FeatureInfo(name = "ClickGui", category = Category.Client, bind = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickGuiFeature extends Feature {
    public BooleanSetting sounds= new BooleanSetting("Sounds", "Custom sounds when enabling or disabling modules.", true);

    @Override
    public void onEnable() {
        if (mc.player == null) {
            setEnabled(false);
            return;
        }
        Melbourne.CLICK_GUI.setClose(false);

        mc.setScreen(Melbourne.CLICK_GUI);

//      if (!BotManager.INSTANCE.isAuthed())
//          System.exit(0);
    }

    @Override
    public void onDisable() {
        if (getNull())
            return;

        mc.setScreen(null);
    }

}
