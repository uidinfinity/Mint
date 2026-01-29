package net.melbourne.modules.impl.render;

import net.melbourne.Managers;
import net.melbourne.modules.impl.client.RendersFeature;
import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderHudEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.WhitelistSetting;
import net.melbourne.utils.entity.EntityUtils;
import net.melbourne.utils.graphics.impl.Renderer2D;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.graphics.impl.font.FontUtils;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.melbourne.utils.miscellaneous.irc.BotManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@FeatureInfo(name = "Nametags", category = Category.Render)
public class NametagsFeature extends Feature {
    public BooleanSetting self = new BooleanSetting("Self", "Allows a nametag to be present on you.", false);
    public BooleanSetting invisible = new BooleanSetting("Invisibles", "Shows a nametag on invisible players.", true);
    public BooleanSetting scaling = new BooleanSetting("Scaling", "Scales the nametags based on distance to simulate depth.", true);

    public final WhitelistSetting display = new WhitelistSetting("Display", "Data to show on the nametag", WhitelistSetting.Type.CUSTOM, new String[]{}, new String[]{"GameMode", "Ping", "EntityID", "Health", "TotemPops", "Armor"});

//    public ModeSetting telemetry = new ModeSetting("Telemetry", "Signicates Mint via some cool methods.", "Symbol", new String[]{"None", "Symbol", "Name", "Both"});

    public BooleanSetting durability = new BooleanSetting("Durability", "Displays armor durability above each piece.", false,
            () -> display.getWhitelistIds().contains("Armor"));

    public BooleanSetting rectangle = new BooleanSetting("Rectangle", "Adds a contrasting background.", false);
    public ColorSetting fillColor = new ColorSetting("FillColor", "The color that will be used for the fill.", new Color(0, 0, 0, 100),
            () -> rectangle.getValue());
    public ColorSetting outlineColor = new ColorSetting("OutlineColor", "The color that will be used for the outline.", new Color(19, 19, 19, 140),
            () -> rectangle.getValue());

    public final WhitelistSetting world = new WhitelistSetting("World", "Entities to show nametags for", WhitelistSetting.Type.CUSTOM, new String[]{}, new String[]{"Items", "Pearls"});
    public ColorSetting miscColor = new ColorSetting("MiscColor", "The color used for dropped items and thrown pearls.", new Color(255, 255, 255, 255),
            () -> !world.getWhitelistIds().isEmpty());

    @SubscribeEvent
    public void onRenderOverlay(RenderHudEvent event) {
        if (getNull()) return;

//      if (!BotManager.INSTANCE.isAuthed())
//          System.exit(0);

        Matrix3x2fStack matrices = event.getContext().getMatrices();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player && !self.getValue()) continue;

            Vec3d vec3d = EntityUtils.getRenderPos(player, mc.getRenderTickCounter().getTickProgress(true));
            Vec3d projected = Renderer3D.project(vec3d.add(0, (player.isSneaking() ? 1.9f : 2.1f), 0));

            if (!invisible.getValue() && player.isInvisible()) continue;
            if (!player.isAlive()) return;
            if (!Renderer3D.projectionVisible(projected)) continue;

            String ign = player.getName().getString();
            UUID uuid = player.getUuid();

//            boolean isMint = false;
//            if (BotManager.INSTANCE != null && player != mc.player) {
//                isMint = BotManager.INSTANCE.ismelbourneUser(uuid, ign);
//            }

//            String mode = telemetry.getValue();
            String shownName = ign;

//            if (isMint && (mode.equalsIgnoreCase("Name") || mode.equalsIgnoreCase("Both"))) {
//                String mapped = BotManager.INSTANCE != null ? BotManager.INSTANCE.getMintNameFor(uuid) : null;
//                if (mapped != null && !mapped.trim().isEmpty()) shownName = mapped.trim();
//            }

            StringBuilder nameBuilder = new StringBuilder();
            nameBuilder.append(shownName);

//            if (isMint && (mode.equalsIgnoreCase("Symbol") || mode.equalsIgnoreCase("Both"))) {
//                nameBuilder.append(" [M]");
//            }

            String text = nameBuilder.toString();

            if (display.getWhitelistIds().contains("GameMode"))
                text += " [" + EntityUtils.getGameModeName(EntityUtils.getGameMode(player)) + "]";

            if (display.getWhitelistIds().contains("Ping"))
                text += " " + EntityUtils.getLatency(player) + "ms";

            if (display.getWhitelistIds().contains("EntityID"))
                text += " " + player.getId();

            if (display.getWhitelistIds().contains("Health"))
                text += " " + ColorUtils.getHealthColor(player.getHealth() + player.getAbsorptionAmount()) + new DecimalFormat("0.0").format(player.getHealth() + player.getAbsorptionAmount()) + Formatting.RESET;

            int pops = Services.WORLD.getPoppedTotems().getOrDefault(uuid, 0);
            if (display.getWhitelistIds().contains("TotemPops") && pops > 0)
                text += " " + ColorUtils.getTotemColor(pops) + "-" + pops;

            float width = FontUtils.getWidth(text);

            float scale = 1.0f;
            if (scaling.getValue()) {
                float dist = mc.cameraEntity.distanceTo(player);
                scale = Math.max(0.5f, Math.min(1.0f, 20.0f / dist));
            }

            matrices.pushMatrix();
            matrices.translate((float) projected.x, (float) projected.y);
            matrices.scale(scale, scale);

            if (rectangle.getValue()) {
                Renderer2D.renderQuad(event.getContext(), -width / 2f - 1, -FontUtils.getHeight() - 2, width / 2 + 2, 0, fillColor.getColor());
                Renderer2D.renderOutline(event.getContext(), -width / 2f - 1, -FontUtils.getHeight() - 2, width / 2 + 2, 0, outlineColor.getColor());
            }


//          if (!BotManager.INSTANCE.isAuthed())
//              System.exit(0);

            FontUtils.drawTextWithShadow(event.getContext(), text, -FontUtils.getWidth(text) / 2.f, -FontUtils.getHeight(), getNameColor(player));

            if (display.getWhitelistIds().contains("Armor")) {
                List<ItemStack> stacks = new ArrayList<>();
                ItemStack[] all = new ItemStack[]{
                        player.getEquippedStack(EquipmentSlot.MAINHAND),
                        player.getEquippedStack(EquipmentSlot.FEET),
                        player.getEquippedStack(EquipmentSlot.LEGS),
                        player.getEquippedStack(EquipmentSlot.CHEST),
                        player.getEquippedStack(EquipmentSlot.HEAD),
                        player.getEquippedStack(EquipmentSlot.OFFHAND)
                };

                for (ItemStack stack : all) {
                    if (!stack.isEmpty()) stacks.add(stack);
                }

                if (!stacks.isEmpty()) {
                    int totalWidth = (stacks.size() * 16) + ((stacks.size() - 1) * 2);
                    int x = -totalWidth / 2;
                    int y = -30;

                    for (int i = stacks.size() - 1; i >= 0; i--) {
                        ItemStack stack = stacks.get(i);
                        event.getContext().drawItem(stack, x, y);
                        event.getContext().drawStackOverlay(mc.textRenderer, stack, x, y);

                        if (durability.getValue()) {
                            int damage = stack.getDamage();
                            int maxDamage = stack.getMaxDamage();

                            if (maxDamage > 0) {
                                event.getContext().getMatrices().pushMatrix();
                                event.getContext().getMatrices().translate(x + 8 - (FontUtils.getWidth((((maxDamage - damage) * 100) / maxDamage) + "%") * 0.75f) / 2.0F, y - (6 * 0.75f));
                                event.getContext().getMatrices().pushMatrix();
                                event.getContext().getMatrices().scale(0.75f, 0.75f);
                                FontUtils.drawTextWithShadow(event.getContext(),
                                        (((maxDamage - damage) * 100) / maxDamage) + "%",
                                        0, 0,
                                        new Color(1.0f - ((maxDamage - damage) / (float) maxDamage),
                                                (maxDamage - damage) / (float) maxDamage, 0));
                                event.getContext().getMatrices().popMatrix();
                                event.getContext().getMatrices().popMatrix();
                            }
                        }

                        x += 16 + 2;
                    }
                }
            }

            matrices.popMatrix();
        }

        if (world.getWhitelistIds().contains("Items")) {
            for (ItemEntity item : mc.world.getEntitiesByClass(ItemEntity.class, mc.getCameraEntity().getBoundingBox().expand(64), e -> true)) {
                Vec3d pos = EntityUtils.getRenderPos(item, mc.getRenderTickCounter().getTickProgress(true));
                Vec3d proj = Renderer3D.project(pos);
                if (!Renderer3D.projectionVisible(proj)) continue;

                ItemStack stack = item.getStack();
                if (stack.isEmpty()) continue;

                String name = stack.getName().getString();
                int count = stack.getCount();
                String display = name + (count > 1 ? " x" + count : "");

                float scale = 1.0f;
                if (scaling.getValue()) {
                    float dist = mc.cameraEntity.distanceTo(item);
                    scale = Math.max(0.5f, Math.min(1.0f, 20.0f / dist));
                }

                matrices.pushMatrix();
                matrices.translate((float) proj.x, (float) proj.y);
                matrices.scale(scale, scale);
                FontUtils.drawTextWithShadow(event.getContext(), display, -FontUtils.getWidth(display) / 2f, -FontUtils.getHeight(), miscColor.getColor());
                matrices.popMatrix();
            }
        }

        if (world.getWhitelistIds().contains("Pearls")) {
            for (EnderPearlEntity pearl : mc.world.getEntitiesByClass(EnderPearlEntity.class, mc.getCameraEntity().getBoundingBox().expand(64), e -> true)) {
                Vec3d pos = EntityUtils.getRenderPos(pearl, mc.getRenderTickCounter().getTickProgress(true));
                Vec3d proj = Renderer3D.project(pos.add(0, 0.25, 0));

                if (!Renderer3D.projectionVisible(proj)) continue;

                if (pearl.getOwner() instanceof PlayerEntity thrower) {
                    String display = thrower.getName().getString();

                    float scale = 1.0f;
                    if (scaling.getValue()) {
                        float dist = mc.cameraEntity.distanceTo(pearl);
                        scale = Math.max(0.5f, Math.min(1.0f, 20.0f / dist));
                    }

                    matrices.pushMatrix();
                    matrices.translate((float) proj.x, (float) proj.y);
                    matrices.scale(scale, scale);
                    FontUtils.drawTextWithShadow(event.getContext(), display, -FontUtils.getWidth(display) / 2f, -FontUtils.getHeight(), miscColor.getColor());
                    matrices.popMatrix();
                }
            }
        }
    }

    private Color getNameColor(PlayerEntity player) {
        if (player.isSneaking()) return new Color(255, 170, 0);
        if (player.getId() == -696969) return new Color(225, 0, 70);


//      if (!BotManager.INSTANCE.isAuthed())
//          System.exit(0);

        RendersFeature renders = Managers.FEATURE.getFeatureFromClass(RendersFeature.class);
        if (Managers.FRIEND.isFriend(player.getName().getString())) return renders.getFriendColor();

        return new Color(player.getTeamColorValue());
    }
}
