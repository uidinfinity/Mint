package net.melbourne.modules.impl.render;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.utils.entity.EntityUtils;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.minecraft.block.AirBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.awt.*;

@FeatureInfo(name = "BlockHighlight", category = Category.Render)
public class BlockHighlightFeature extends Feature {
    public BooleanSetting entities = new BooleanSetting("Entities", "Uses the team color, instead of global colors.",true);
    public ColorSetting fillColor = new ColorSetting("Fill", "The color used for the box fill.", new Color(255, 255, 255, 74));
    public ColorSetting outlineColor = new ColorSetting("Outline", "The color used for outlines.", new Color(255, 255, 255));

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.crosshairTarget instanceof BlockHitResult result) {
            if (mc.world.getBlockState(result.getBlockPos()).getBlock() instanceof AirBlock)
                return;

            VoxelShape shape = mc.world.getBlockState(result.getBlockPos()).getOutlineShape(mc.world, result.getBlockPos());

            Renderer3D.renderBox(event.getContext(), shape.getBoundingBox().offset(result.getBlockPos()), fillColor.getColor());
            Renderer3D.renderBoxOutline(event.getContext(), shape.getBoundingBox().offset(result.getBlockPos()), outlineColor.getColor());
        } else if (entities.getValue() && mc.crosshairTarget instanceof EntityHitResult result) {
            Entity entity = result.getEntity();
            Vec3d pos = EntityUtils.getRenderPos(entity, event.getTickDelta());

            Box box = new Box(pos.x - entity.getBoundingBox().getLengthX() / 2,
                    pos.y,
                    pos.z - entity.getBoundingBox().getLengthZ() / 2,
                    pos.x + entity.getBoundingBox().getLengthX() / 2,
                    pos.y + entity.getBoundingBox().getLengthY(),
                    pos.z + entity.getBoundingBox().getLengthZ() / 2);

            Renderer3D.renderBox(event.getContext(), box, fillColor.getColor());
            Renderer3D.renderBoxOutline(event.getContext(), box, outlineColor.getColor());
        }
    }
}
