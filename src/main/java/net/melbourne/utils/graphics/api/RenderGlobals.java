package net.melbourne.utils.graphics.api;

import net.melbourne.Melbourne;
import net.minecraft.client.render.RenderPhase;
import org.lwjgl.opengl.GL11;

public class RenderGlobals {

    public static final RenderPhase.Layering LINE_SMOOTH_LAYERING = new RenderPhase.Layering(Melbourne.MOD_ID + "_line_smooth", () -> {
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
    }, () -> {
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    });
}
