package net.melbourne.modules.impl.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderHudEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.animations.Animation;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.entity.EntityUtils;
import net.melbourne.utils.graphics.impl.Renderer2D;
import net.melbourne.utils.graphics.impl.font.FontUtils;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.melbourne.utils.miscellaneous.math.MathUtils;
import net.melbourne.utils.miscellaneous.irc.BotManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import javax.annotation.Nullable;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@FeatureInfo(name = "Hud", category = Category.Client)
public class HudFeature extends Feature {
    private final Animation chatAnimation = new Animation(300, Easing.Method.EASE_OUT_QUAD);

    public BooleanSetting watermark = new BooleanSetting("Watermark", "Displays the clients logo on your screen.", true);
    public BooleanSetting moduleList = new BooleanSetting("ModuleList", "Displays a list of modules you have enabled.", true);
    public ModeSetting sortingMode = new ModeSetting("Sorting", "Mode", "Length", new String[]{"Length", "Alphabetical"}, () -> moduleList.getValue());

    public BooleanSetting coordinates = new BooleanSetting("Coordinates", "Displays your ingame world coordinates.", true);
    public BooleanSetting direction = new BooleanSetting("Direction", "Displays your ingame direction.", true);
    public BooleanSetting nether = new BooleanSetting("Nether", "Displays your coordinates if you were in the nether.", true, () -> coordinates.getValue());

    public BooleanSetting armor = new BooleanSetting("Armor", "Displays equipped armor and durability percentages.", true);
    public NumberSetting armorScale = new NumberSetting("ArmorScale", "Scale of text for armor percentage.", 0.7, 0.3, 1.0, () -> armor.getValue());
    public ColorSetting highDurabilityColor = new ColorSetting("ArmorHigh", "Color for high durability.", new Color(0, 255, 0), () -> armor.getValue());
    public ColorSetting lowDurabilityColor = new ColorSetting("ArmorLow", "Color for low durability.", new Color(255, 0, 0), () -> armor.getValue());

    public BooleanSetting armorWarning = new BooleanSetting("ArmorWarning", "Warns when armor durability is low.", true);
    public NumberSetting durabilityThreshold = new NumberSetting("DurabilityThreshold", "Percentage threshold for warnings.", 20, 1, 100, () -> armorWarning.getValue());

    public BooleanSetting potions = new BooleanSetting("Potions", "Displays a list of your potion effects.", true);
//    public BooleanSetting mintUsername = new BooleanSetting("ClientInfo", "Displays your Mint username in the bottom-right.", true);

    public BooleanSetting fps = new BooleanSetting("FPS", "Displays your current FPS.", true);
    public BooleanSetting tps = new BooleanSetting("TPS", "Displays the servers current tickrate.", true);
    public BooleanSetting averageTps = new BooleanSetting("AverageTPS", "Displays additional average tickrate.", false, () -> tps.getValue());
    public BooleanSetting ping = new BooleanSetting("Ping", "Displays the ping to the server.", true);
    public BooleanSetting speedometer = new BooleanSetting("Speedometer", "Displays your current speed.", true);
    public BooleanSetting brand = new BooleanSetting("Brand", "Displays server software brand.", true);
    public BooleanSetting durability = new BooleanSetting("Durability", "Displays the durability of your held item.", true);
    public BooleanSetting pearlCooldown = new BooleanSetting("PearlCooldown", "Displays a timer with pearl cooldown.", true);
    public BooleanSetting serverLag = new BooleanSetting("ServerLag", "Displays lag timer.", true);

    public BooleanSetting chestCounter = new BooleanSetting("ChestCounter", "Displays the amount of loaded chests.", true);

    public ModeSetting healthBarMode = new ModeSetting("HealthBar", "Health bar above hotbar", "Sync", new String[]{"None", "Sync", "Dynamic"});

    public ModeSetting colorMode = new ModeSetting("ColorMode", "Mode", "Default", new String[]{"Default", "Wave", "Transition"});
    public ColorSetting secondColor = new ColorSetting("SecondColor", "Second color for transition.", new Color(255, 255, 255),
            () -> colorMode.getValue().equals("Transition"));

    private float chatOffset;
    private final List<FeatureEntry> featureEntries = new ArrayList<>();
    private final List<PotionEntry> potionEntries = new ArrayList<>();
    private final List<InfoEntry> infoEntries = new ArrayList<>();
    private final List<MiscEntry> miscEntries = new ArrayList<>();
    private Float healthAnim = null;

    private double loadedChests = 0.0;
    private long lastChestCountMs = 0L;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        if (moduleList.getValue()) {
            List<FeatureEntry> entries = new ArrayList<>();
            for (Feature feature : Managers.FEATURE.getFeatures()) {
                String text = feature.getName();
                if (!feature.hiddenMode.getValue().equalsIgnoreCase("Info")) {
                    String info = feature.getInfo();
                    if (!info.isEmpty()) {
                        text += Formatting.GRAY + " [" + Formatting.WHITE + info + Formatting.GRAY + "]";
                    }
                }

                if (feature.hiddenMode.getValue().equalsIgnoreCase("Hidden")) {
                    continue;
                }

                if (!feature.isEnabled() && feature.getAnimationX().get(0) <= (float) (FontUtils.getWidth(text) + 2) / 2) {
                    continue;
                }

                entries.add(new FeatureEntry(feature, text));
            }

            if (sortingMode.getValue().equalsIgnoreCase("Alphabetical")) {
                entries.sort(Comparator.comparing(e -> e.text.toLowerCase()));
            } else {
                entries.sort(Comparator.comparingInt(e -> -FontUtils.getWidth(e.text)));
            }

            featureEntries.clear();
            featureEntries.addAll(entries);
        }

        if (potions.getValue()) {
            for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
                if (potionEntries.stream().noneMatch(e -> e.type.equals(effect.getEffectType()))) {
                    PotionEntry entry = new PotionEntry(effect.getEffectType(), getPotionText(effect), new Color(effect.getEffectType().value().getColor()));
                    entry.setNewEntry(true);
                    potionEntries.add(entry);
                } else {
                    for (PotionEntry entry : potionEntries) {
                        if (entry.getType() != effect.getEffectType()) continue;
                        entry.setText(getPotionText(effect));
                        entry.setState(true);
                    }
                }
            }

            potionEntries.sort(Comparator.comparingInt(m -> -FontUtils.getWidth(m.text)));

            float offset = 0;
            for (PotionEntry entry : new ArrayList<>(potionEntries)) {
                if (mc.player.getStatusEffect(entry.getType()) == null) {
                    entry.setState(false);
                    if (entry.getAnimationX().get(0) <= 0) {
                        potionEntries.remove(entry);
                        continue;
                    }
                }

                float targetY = offset;
                if (entry.isNewEntry()) {
                    entry.getAnimationY().setPrev(targetY + FontUtils.getHeight());
                    entry.setNewEntry(false);
                }
                entry.setTargetY(targetY);
                offset += FontUtils.getHeight();
            }
        }
    }

    @SubscribeEvent
    public void tickInfo(TickEvent event) {
        if (getNull()) return;
        List<InfoEntry> currentEntries = new ArrayList<>();

        if (fps.getValue())
            currentEntries.add(new InfoEntry("FPS", String.valueOf(MinecraftClient.getInstance().getCurrentFps())));

        if (ping.getValue())
            currentEntries.add(new InfoEntry("Ping", EntityUtils.getLatency(mc.player) + "ms"));

        if (tps.getValue()) {
            String tpsText = MathUtils.round(Services.SERVER.getTps(), 2) + "";

            if (averageTps.getValue()) {
                float avg = Services.SERVER.getAverageTps();
                tpsText += Formatting.GRAY + " [" + Formatting.WHITE + MathUtils.round(avg, 2) + Formatting.GRAY + "]";
            }

            currentEntries.add(new InfoEntry("TPS", tpsText));
        }

        if (speedometer.getValue())
            currentEntries.add(new InfoEntry("Speed", new DecimalFormat("0.0").format(EntityUtils.getSpeed(mc.player, EntityUtils.SpeedUnit.KILOMETERS)) + "km/h"));

        if (brand.getValue() && mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null)
            currentEntries.add(new InfoEntry("ServerBrand", mc.getNetworkHandler().getBrand().toString()));

        if (durability.getValue() && mc.player.getMainHandStack().isDamageable()) {
            int dur = mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage();
            currentEntries.add(new InfoEntry("Durability", (dur * 100 / mc.player.getMainHandStack().getMaxDamage()) + "%"));
        }

        if (chestCounter.getValue()) {
            long now = System.currentTimeMillis();
            if (now - lastChestCountMs >= 500L) {
                loadedChests = countLoadedChests();
                lastChestCountMs = now;
            }
            currentEntries.add(new InfoEntry("Chests", new DecimalFormat("0.0").format(loadedChests)));
        }

        infoEntries.sort(Comparator.comparingInt(m -> -FontUtils.getWidth(m.getText() + " " + m.getInfo())));
        for (InfoEntry current : currentEntries) {
            if (infoEntries.stream().noneMatch(e -> e.getText().equals(current.getText()))) {
                infoEntries.add(current);
            } else {
                for (InfoEntry entry : infoEntries) {
                    if (!entry.getText().equals(current.getText())) continue;
                    entry.setInfo(current.getInfo());
                    entry.setState(true);
                }
            }
        }
        for (InfoEntry entry : new ArrayList<>(infoEntries)) {
            if (currentEntries.stream().noneMatch(e -> e.getText().equals(entry.getText()))) {
                entry.setState(false);

                if (entry.getAnimationX().get(0) <= 0)
                    infoEntries.remove(entry);
            }
        }
    }

    @SubscribeEvent
    public void tickMisc(TickEvent event) {
        if (getNull()) return;

        List<MiscEntry> misc = new ArrayList<>();
        float progress = getCooldown();

        if (pearlCooldown.getValue() && progress != 0.0f)
            misc.add(new MiscEntry("Active pearl cooldown", new Color(120, 0, 255), progress, "s"));
        if (serverLag.getValue() && Services.SERVER.getTimer().hasTimeElapsed(1000))
            misc.add(new MiscEntry("Server is currently not responding.", new Color(255, 100, 100), null, null));
        if (armorWarning.getValue()) {
            int threshold = durabilityThreshold.getValue().intValue();

            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack stack = mc.player.getEquippedStack(slot);
                if (stack.isEmpty() || !stack.isDamageable()) continue;

                float max = stack.getMaxDamage();
                float dmg = stack.getDamage();
                float perc = (max - dmg) * 100 / max;

                if (perc > threshold) continue;

                String piece = slot == EquipmentSlot.HEAD ? "helmet" : slot == EquipmentSlot.CHEST ? "chestplate" : slot == EquipmentSlot.LEGS ? "leggings" : "boots";
                String verb = (slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) ? "are" : "is";
                String text = "Your " + piece + " " + verb + " low!";

                misc.add(new MiscEntry(text, new Color(255, 0, 0), perc, "%"));
            }
        }
        for (MiscEntry current : misc) {
            if (miscEntries.stream().noneMatch(e -> e.getText().equals(current.getText()))) {
                miscEntries.add(current);
            } else {
                for (MiscEntry entry : miscEntries) {
                    if (!entry.getText().equals(current.getText())) continue;
                    entry.setText(current.getText());
                    entry.setColor(current.getColor());
                    entry.setTime(current.getTime());
                    entry.setState(true);
                }
            }
        }

        for (MiscEntry entry : new ArrayList<>(miscEntries)) {
            if (misc.stream().noneMatch(e -> e.getText().equals(entry.getText()))) {
                entry.setState(false);
                if (entry.getAnimationY().get(-(FontUtils.getHeight() + 1))
                        <= -(FontUtils.getHeight() + 1)) {
                    miscEntries.remove(entry);
                }
            }
        }
    }

    @SubscribeEvent
    public void renderHud(RenderHudEvent event) {
        if (mc.player == null) return;
        if (watermark.getValue()) {
            String text = Melbourne.NAME + Formatting.GRAY + " - " + Formatting.WHITE + "v" + Melbourne.MOD_VERSION + "+" + Melbourne.GIT_REVISION + "." + Melbourne.GIT_HASH;
            FontUtils.drawTextWithShadow(event.getContext(), text, 1, 1, getHudColor(1));
        }
        if (moduleList.getValue()) {
            float offset = 0;
            for (FeatureEntry entry : featureEntries) {
                float width = entry.feature.isEnabled() ? FontUtils.getWidth(entry.text) + 1 : 0;
                float x = mc.getWindow().getScaledWidth() - entry.feature.getAnimationX().get(width);
                float y = 2 + entry.feature.getAnimationY().get(offset);

                if (entry.feature.isNewEntry()) {
                    entry.feature.getAnimationY().get(1 + offset);
                    entry.feature.setNewEntry(false);
                }

                event.getContext().getMatrices().pushMatrix();
                event.getContext().getMatrices().translate(x, y);
                FontUtils.drawTextWithShadow(event.getContext(), entry.text, 0, 0, getHudColor(y));
                event.getContext().getMatrices().popMatrix();

                offset += FontUtils.getHeight();
            }
        }

        if (armor.getValue()) {
            float scale = armorScale.getValue().floatValue();
            int offset = 0;
            ItemStack[] armor = new ItemStack[]{mc.player.getEquippedStack(EquipmentSlot.FEET), mc.player.getEquippedStack(EquipmentSlot.LEGS), mc.player.getEquippedStack(EquipmentSlot.CHEST), mc.player.getEquippedStack(EquipmentSlot.HEAD)};
            for (ItemStack stack : armor) {
                if (stack.isEmpty()) continue;
                int wateroffset = (mc.player.isSubmergedInWater() || mc.player.getAir() < mc.player.getMaxAir()) ? 10 : 0;
                int x = mc.getWindow().getScaledWidth() / 2 + 69 - (18 * offset);
                int y = mc.getWindow().getScaledHeight() - 55 - wateroffset;
                event.getContext().drawItem(stack, x, y);
                event.getContext().drawStackOverlay(mc.textRenderer, stack, x, y);
                int damage = stack.getDamage();
                int maxDamage = stack.getMaxDamage();
                if (maxDamage > 0) {
                    String text = (((maxDamage - damage) * 100) / maxDamage) + "%";

                    float percentage = (float) (maxDamage - damage) / maxDamage;
                    int r = (int) (highDurabilityColor.getColor().getRed() * percentage + lowDurabilityColor.getColor().getRed() * (1f - percentage));
                    int g = (int) (highDurabilityColor.getColor().getGreen() * percentage + lowDurabilityColor.getColor().getGreen() * (1f - percentage));
                    int b = (int) (highDurabilityColor.getColor().getBlue() * percentage + lowDurabilityColor.getColor().getBlue() * (1f - percentage));

                    event.getContext().getMatrices().pushMatrix();
                    event.getContext().getMatrices().translate(x + 8 - (FontUtils.getWidth(text) * scale) / 2.0F, y - (6 * scale));
                    event.getContext().getMatrices().pushMatrix();
                    event.getContext().getMatrices().scale(scale, scale);
                    FontUtils.drawTextWithShadow(event.getContext(), text, 0, 0, new Color(r, g, b));
                    event.getContext().getMatrices().popMatrix();
                    event.getContext().getMatrices().popMatrix();
                }
                offset++;
            }
        }

        if (!healthBarMode.getValue().equalsIgnoreCase("None")) {
            if (mc.player == null) return;

            float width = event.getContext().getScaledWindowWidth();
            float height = event.getContext().getScaledWindowHeight();
            float maxBarWidth = 60f;
            float barHeight = 2f;
            float centerX = (width - maxBarWidth) / 2f;
            float y = (height + 10) / 2f;

            Renderer2D.renderQuad(event.getContext(), centerX, y, centerX + maxBarWidth, y + barHeight, new Color(0, 0, 0, 100));

            float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            float maxHealth = mc.player.getMaxHealth() + mc.player.getAbsorptionAmount();
            float targetProgress = MathHelper.clamp(health / maxHealth, 0f, 1f);

            if (this.healthAnim == null) this.healthAnim = targetProgress;
            this.healthAnim = this.healthAnim + (targetProgress - this.healthAnim) * 0.2f;

            float filledWidth = maxBarWidth * Easing.ease(this.healthAnim, Easing.Method.EASE_OUT_QUAD);

            if (filledWidth >= 1f) {
                Formatting format = getHealthColor(health);
                Color fillColor = healthBarMode.getValue().equalsIgnoreCase("Sync")
                        ? ColorUtils.getGlobalColor()
                        : new Color(format.getColorValue() != null ? format.getColorValue() : 0xFFFFFF);

                Renderer2D.renderQuad(event.getContext(), centerX, y, centerX + filledWidth, y + barHeight, fillColor);
            }

            Renderer2D.renderOutline(event.getContext(), centerX, y, centerX + maxBarWidth, y + barHeight, Color.black);
        }
    }

    @SubscribeEvent
    public void renderInfo(RenderHudEvent event) {
        int infoOffsetCount = 0;
        chatOffset = chatAnimation.get(mc.currentScreen instanceof ChatScreen ? 14 : 0);

//        if (mintUsername.getValue()) {
//            String mint = null;
//            if (BotManager.INSTANCE != null) mint = BotManager.INSTANCE.getAuthedMintUsername();
//            if (mint == null || mint.trim().isEmpty()) mint = "Not logged in";
//
//            String rank = "User";
//            if (BotManager.INSTANCE != null) {
//                String r = BotManager.INSTANCE.getAuthedMintRank();
//                if (r != null && !r.trim().isEmpty()) {
//                    String rl = r.trim().toLowerCase();
//                    if (rl.length() > 0) rank = rl.substring(0, 1).toUpperCase() + rl.substring(1);
//                }
//            }
//
//            String text = mint + Formatting.GRAY + " | " + Formatting.GRAY + "(" + Formatting.WHITE + rank + Formatting.GRAY + ")";
//
//            float w = FontUtils.getWidth(text);
//            float x = mc.getWindow().getScaledWidth() - 2 - w;
//            float y = mc.getWindow().getScaledHeight() - chatOffset - 2 - FontUtils.getHeight() - (FontUtils.getHeight() * infoOffsetCount);
//
//            FontUtils.drawTextWithShadow(event.getContext(), text, x, y, getHudColor(y));
//            infoOffsetCount++;
//        }

        if (potions.getValue()) {
            float baseY = mc.getWindow().getScaledHeight() - chatOffset - 2 - FontUtils.getHeight() - (FontUtils.getHeight() * infoOffsetCount);

            for (PotionEntry entry : potionEntries) {
                float width = entry.isState() ? FontUtils.getWidth(entry.text) : 0;
                float x = mc.getWindow().getScaledWidth() - 2 - entry.getAnimationX().get(width);
                float y = baseY - entry.getAnimationY().get(entry.getTargetY());
                FontUtils.drawTextWithShadow(event.getContext(), entry.text, x, y, entry.color);
                infoOffsetCount++;
            }
        }

        for (InfoEntry entry : infoEntries) {
            float x = mc.getWindow().getScaledWidth() - 2 - (entry.getAnimationX().get(entry.isState() ? FontUtils.getWidth(entry.text + " " + entry.info) : 0));
            float y = mc.getWindow().getScaledHeight() - chatOffset - 2 - FontUtils.getHeight() - (FontUtils.getHeight() * infoOffsetCount);
            FontUtils.drawTextWithShadow(event.getContext(), entry.text + Formatting.WHITE + " " + entry.info, x, y, getHudColor(y));
            infoOffsetCount++;
        }

        int coordOffset = 0;
        if (coordinates.getValue()) {
            float y = mc.getWindow().getScaledHeight() - chatOffset - FontUtils.getHeight() - 1;
            String str = "XYZ: " + Formatting.WHITE +
                    MathUtils.round(mc.player.getX(), 2) + Formatting.GRAY + ", " + Formatting.WHITE +
                    MathUtils.round(mc.player.getY(), 2) + Formatting.GRAY + ", " + Formatting.WHITE +
                    MathUtils.round(mc.player.getZ(), 2);

            if (nether.getValue()) {
                str += Formatting.GRAY + " [" + Formatting.WHITE +
                        MathUtils.round(getDimensionCoord(mc.player.getX()), 2) + Formatting.GRAY + ", " + Formatting.WHITE +
                        MathUtils.round(getDimensionCoord(mc.player.getZ()), 2) + Formatting.GRAY + "]";
            }


            FontUtils.drawTextWithShadow(event.getContext(), str, 1.F, y, getHudColor(y));
            coordOffset++;
        }

        if (direction.getValue()) {
            float y = mc.getWindow().getScaledHeight() - chatOffset - FontUtils.getHeight() - 1 - (FontUtils.getHeight() * coordOffset);
            String str = getDirections();
            FontUtils.drawTextWithShadow(event.getContext(), str, 1.F, y, getHudColor(y));
        }

        float miscOffset = 0;
        for (MiscEntry entry : miscEntries) {
            float targetY = 2 + miscOffset + FontUtils.getHeight();
            String text = entry.getText();
            if (entry.getTime() != null)
                text += String.format(" (%.1f%s)", entry.getTime(), entry.getSuffix());

            float y = entry.getAnimationY().get(entry.isState() ? targetY : -(FontUtils.getHeight() + 1));
            float x = (mc.getWindow().getScaledWidth() / 2.f) - (FontUtils.getWidth(text) / 2.f);
            FontUtils.drawTextWithShadow(event.getContext(), text, x, y - FontUtils.getHeight(), entry.getColor());
            miscOffset += FontUtils.getHeight() + 1;
        }
    }

    private float getCooldown() {
        ItemStack pearl = findInventoryItemStack();
        return pearl != null && mc.player.getItemCooldownManager().isCoolingDown(pearl) ? mc.player.getItemCooldownManager().getCooldownProgress(pearl, 0.0f) : 0.0f;
    }

    private ItemStack findInventoryItemStack() {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.ENDER_PEARL)) return stack;
        }
        return null;
    }

    private String getPotionText(StatusEffectInstance effect) {
        int amplifier = effect.getAmplifier() + 1;
        String ampText = amplifier == 1 ? "" : " " + MathUtils.convertNumberToRoman(amplifier);
        return effect.getEffectType().value().getName().getString() + ampText + " " + Formatting.WHITE +
                StatusEffectUtil.getDurationText(effect, 1.0F, mc.world.getTickManager().getTickRate()).getString()
                        .replace(Text.translatable("effect.duration.infinite").getString(), "**:**");
    }

    public boolean isHealthBarEnabled() {
        return !healthBarMode.getValue().equalsIgnoreCase("None");
    }

    private double getDimensionCoord(double coord) {
        return mc.world.getRegistryKey().getValue().getPath().contains("nether") ? coord * 8.0 : coord / 8.0;
    }


    private String getDirections() {
        String[] directions = new String[]{"South ", "South West ", "West ", "North West ", "North ", "North East ", "East ", "South East "};
        String[] axis = new String[]{"+Z", "+Z -X", "-X", "-Z -X", "-Z", "-Z +X", "+X", "+Z +X"};

        String gang = axis[MathUtils.angleDirection(MathHelper.wrapDegrees(mc.player.getYaw()), axis.length)];
        String cool = directions[MathUtils.angleDirection(MathHelper.wrapDegrees(mc.player.getYaw()), directions.length)];

        return cool + Formatting.GRAY + "[" + Formatting.WHITE + gang + Formatting.GRAY + "]";
    }

    private Color getHudColor(float offset) {
        long index = (long) ((offset / FontUtils.getHeight()) * (200L));

        if (Managers.FEATURE.getFeatureFromClass(ColorFeature.class).mode.getValue().equalsIgnoreCase("Static")) {
            if (colorMode.getValue().equals("Wave") || colorMode.getValue().equals("Transition")) {
                return colorMode.getValue().equals("Wave") ? ColorUtils.getOffsetWave(ColorUtils.getGlobalColor(), index) : ColorUtils.getTransitionColor(ColorUtils.getGlobalColor(), secondColor.getColor(), index);
            }

            return ColorUtils.getGlobalColor();
        } else {
            ColorFeature color = Managers.FEATURE.getFeatureFromClass(ColorFeature.class);
            return ColorUtils.getRainbowColor(color.rainbowSpeed.getValue().longValue(),
                    color.rainbowLength.getValue().longValue(),
                    color.rainbowSaturation.getValue().floatValue(), (long) offset);
        }
    }
    
    private double countLoadedChests() {
        if (mc.world == null || mc.player == null) return 0.0;

        int vd;
        try {
            vd = mc.options.getViewDistance().getValue();
        } catch (Throwable t) {
            vd = 8;
        }

        int pcx = mc.player.getBlockX() >> 4;
        int pcz = mc.player.getBlockZ() >> 4;

        double count = 0.0;

        for (int dx = -vd; dx <= vd; dx++) {
            for (int dz = -vd; dz <= vd; dz++) {
                Chunk chunk = mc.world.getChunk(pcx + dx, pcz + dz, ChunkStatus.FULL, false);
                if (!(chunk instanceof WorldChunk wc)) continue;

                for (BlockEntity be : wc.getBlockEntities().values()) {
                    if (!(be instanceof ChestBlockEntity cbe)) continue;

                    if (cbe.getCachedState().getBlock() instanceof net.minecraft.block.ChestBlock) {
                        net.minecraft.block.enums.ChestType type =
                                cbe.getCachedState().get(net.minecraft.block.ChestBlock.CHEST_TYPE);

                        if (type == net.minecraft.block.enums.ChestType.SINGLE) count += 0.5;
                        else count += 0.5;
                    }
                }
            }
        }

        return count;
    }

    private Formatting getHealthColor(double health) {
        if (health > 18.0) return Formatting.GREEN;
        else if (health > 16.0) return Formatting.DARK_GREEN;
        else if (health > 12.0) return Formatting.YELLOW;
        else if (health > 8.0) return Formatting.GOLD;
        else if (health > 5.0) return Formatting.RED;
        return Formatting.DARK_RED;
    }

    @Getter
    @Setter
    private static class AnimationEntry {
        protected boolean state = false;
        private boolean newEntry = false;
        private float targetY = 0f;
        private final Animation animationX = new Animation(300, Easing.Method.EASE_OUT_QUAD);
        private final Animation animationY = new Animation(300, Easing.Method.EASE_OUT_QUAD);
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private static class PotionEntry extends AnimationEntry {
        private final RegistryEntry<StatusEffect> type;
        private String text;
        private Color color;
    }

    public record FeatureEntry(Feature feature, String text) {}

    @AllArgsConstructor
    @Getter
    @Setter
    public static class InfoEntry extends AnimationEntry {
        private String text;
        private String info;
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class MiscEntry extends AnimationEntry {
        private String text;
        private Color color;

        @Nullable
        private Float time;
        @Nullable
        private String suffix;
    }
}
