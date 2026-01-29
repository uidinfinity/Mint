package net.melbourne.modules.impl.render;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.settings.types.WhitelistSetting;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@FeatureInfo(name = "Search", category = Category.Render)
public class SearchFeature extends Feature {

    public WhitelistSetting searchBlocks = new WhitelistSetting("Blocks", "Blocks to search for.", WhitelistSetting.Type.BLOCKS);
    public ModeSetting renderMode = new ModeSetting("Render Mode", "Render style.", "Box", new String[]{"Tracer", "Box", "Both"});
    public ModeSetting colorMode = new ModeSetting("Color Mode", "Color source.", "Normal", new String[]{"Normal", "Custom"});
    public NumberSetting range = new NumberSetting("Range", "Maximum distance.", 50, 5, 300);

    public ColorSetting fill = new ColorSetting("Fill", "Custom fill color.", new Color(255, 255, 0, 50),
            () -> colorMode.getValue().equalsIgnoreCase("Custom"));

    public ColorSetting line = new ColorSetting("Line", "Custom line color.", new Color(255, 255, 255, 255),
            () -> colorMode.getValue().equalsIgnoreCase("Custom"));

    private final List<BlockPos> foundBlocks = new ArrayList<>();
    private final Object lock = new Object();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean searching = false;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull() || searching || mc.player.age % 10 != 0) return;

        searching = true;
        executor.execute(() -> {
            try {
                double rSq = Math.pow(range.getValue().doubleValue(), 2);
                List<BlockPos> found = new ArrayList<>();
                BlockPos playerPos = mc.player.getBlockPos();
                int r = range.getValue().intValue();

                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            BlockPos pos = playerPos.add(x, y, z);
                            if (mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= rSq) {
                                BlockState state = mc.world.getBlockState(pos);
                                if (searchBlocks.isWhitelistContains(state.getBlock())) {
                                    found.add(pos.toImmutable());
                                }
                            }
                        }
                    }
                }

                synchronized (lock) {
                    foundBlocks.clear();
                    foundBlocks.addAll(found);
                }
            } finally {
                searching = false;
            }
        });
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (getNull()) return;

        List<BlockPos> toRender;
        synchronized (lock) {
            if (foundBlocks.isEmpty()) return;
            toRender = new ArrayList<>(foundBlocks);
        }

        String mode = renderMode.getValue();
        boolean isCustom = colorMode.getValue().equalsIgnoreCase("Custom");
        boolean drawBox = mode.equalsIgnoreCase("Box") || mode.equalsIgnoreCase("Both");
        boolean drawTracer = mode.equalsIgnoreCase("Tracer") || mode.equalsIgnoreCase("Both");
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        for (BlockPos pos : toRender) {
            Color boxColor;
            Color lineColor;

            if (isCustom) {
                boxColor = fill.getColor();
                lineColor = line.getColor();
            } else {
                BlockState state = mc.world.getBlockState(pos);
                int colorInt = mc.getBlockColors().getColor(state, mc.world, pos, 0);

                if (colorInt <= 0) {
                    colorInt = state.getMapColor(mc.world, pos).color;
                }

                int r = (colorInt >> 16) & 0xFF;
                int g = (colorInt >> 8) & 0xFF;
                int b = colorInt & 0xFF;
                boxColor = new Color(r, g, b, 80);
                lineColor = new Color(r, g, b, 255);
            }

            double x = pos.getX();
            double y = pos.getY();
            double z = pos.getZ();
            Box box = new Box(x, y, z, x + 1, y + 1, z + 1);

            if (drawBox) {
                Renderer3D.renderBox(event.getContext(), box, boxColor, boxColor);
                Renderer3D.renderBoxOutline(event.getContext(), box, lineColor, lineColor);
            }

            if (drawTracer) {
                Renderer3D.renderLine(event.getContext(), cameraPos, Vec3d.ofCenter(pos), lineColor, lineColor);
            }
        }
    }

    @Override
    public void onDisable() {
        synchronized (lock) {
            foundBlocks.clear();
        }
    }
}