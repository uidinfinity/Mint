package net.melbourne.modules.impl.render;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.ItemRenderEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

@FeatureInfo(name = "Viewmodel", category = Category.Render)
public class ViewmodelFeature extends Feature {

    public final NumberSetting transX = new NumberSetting("TranslateX", "", 0.0, -2.0, 2.0);
    public final NumberSetting transY = new NumberSetting("TranslateY", "", 0.0, -2.0, 2.0);
    public final NumberSetting transZ = new NumberSetting("TranslateZ", "", 0.0, -2.0, 2.0);
    public final NumberSetting scaleX = new NumberSetting("ScaleX", "", 1.0, 0.0, 3.0);
    public final NumberSetting scaleY = new NumberSetting("ScaleY", "", 1.0, 0.0, 3.0);
    public final NumberSetting scaleZ = new NumberSetting("ScaleZ", "", 1.0, 0.0, 3.0);
    public final NumberSetting rotX = new NumberSetting("RotateX", "", 0.0, -360.0, 360.0);
    public final NumberSetting rotY = new NumberSetting("RotateY", "", 0.0, -360.0, 360.0);
    public final NumberSetting rotZ = new NumberSetting("RotateZ", "", 0.0, -360.0, 360.0);
    public final BooleanSetting sway = new BooleanSetting("Sway", "", true);
    public final BooleanSetting eating = new BooleanSetting("Eating", "", true);

    @SubscribeEvent
    public void onItemRender(ItemRenderEvent event) {
        if (mc.player == null) return;

        if (!eating.getValue() && mc.player.isUsingItem() && mc.player.getActiveHand() == event.getHand()) {
            return;
        }

        MatrixStack matrices = event.getMatrixStack();

        Arm arm = event.getHand() == Hand.MAIN_HAND ?
                mc.player.getMainArm() :
                mc.player.getMainArm().getOpposite();
        int direction = arm == Arm.RIGHT ? 1 : -1;

        matrices.translate(transX.getValue().floatValue() * direction, transY.getValue().floatValue(), transZ.getValue().floatValue());
        matrices.scale(scaleX.getValue().floatValue(), scaleY.getValue().floatValue(), scaleZ.getValue().floatValue());

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX.getValue().floatValue()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY.getValue().floatValue() * direction));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ.getValue().floatValue() * direction));
    }
}