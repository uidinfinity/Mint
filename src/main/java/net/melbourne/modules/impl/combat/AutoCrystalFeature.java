package net.melbourne.modules.impl.combat;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Setter;
import net.melbourne.Managers;
import net.melbourne.engine.simulation.Simulations;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.EntitySpawnEvent;
import net.melbourne.events.impl.GameLoopEvent;
import net.melbourne.events.impl.PlayerUpdateEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.client.RendersFeature;
import net.melbourne.modules.impl.misc.PacketMineFeature;
import net.melbourne.services.Services;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.entity.CrystalUtils;
import net.melbourne.utils.entity.player.PlayerUtils;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.inventory.SwitchType;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.melbourne.utils.miscellaneous.Timer;
import net.melbourne.utils.rotation.RotationPoint;
import net.melbourne.utils.rotation.RotationUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.awt.Color;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@FeatureInfo(name = "AutoCrystal", category = Category.Combat)
public class AutoCrystalFeature extends Feature {

    private final BooleanSetting rotate = new BooleanSetting("Rotate", "Rotates to the place/break location", true);
    private final BooleanSetting gameLoop = new BooleanSetting("Gameloop", "Runs the ca on not only full ticks, but partial ticks aswell.", true);
    private final NumberSetting loopDelay = new NumberSetting("GameloopDelay", "Allows you to skip certain partial ticks.", 3.0, 0.0, 500.0);
    private final BooleanSetting eatingPause = new BooleanSetting("EatingPause", "Pause breaking and placing while eating a food item.", true);
    private final NumberSetting switchPause = new NumberSetting("SwitchPause", "A delay that occurs when you swap an item, the ca will be delayed for this amount.", 100, 0, 1000);
    private final NumberSetting targetRange = new NumberSetting("TargetRange", "The range that the ca will try to find a target within.", 12.0, 4.0, 20.0);
    private final NumberSetting updateDelay = new NumberSetting("UpdateDelay", "The delay inbetween position recalculations.", 50, 0, 1000);
    private final NumberSetting wallRange = new NumberSetting("WallRange", "The maximum range that the ca will try to utilise crystals through walls.", 6.0, 0.0, 6.0);
    private final NumberSetting minDamage = new NumberSetting("MinDamage", "The minimum damage requirement the ca has to hit while calculating the best possible position for placing.", 5.0, 0.0, 36.0);
    private final NumberSetting maxSelf = new NumberSetting("MaxSelf", "The maximum damage that the ca will allow to yourself while calculating the best position for placing.", 12.0, 0.0, 36.0);
    private final NumberSetting range = new NumberSetting("Range", "The range at which the client will try to utilise crystals.", 5.0, 1.0, 6.0);
    private final BooleanSetting baseplace = new BooleanSetting("BasePlace", "Places obsidian to help you kill someone when you have no obsidian or bedrock in your range.", true);
    private final BooleanSetting baseplaceAir = new BooleanSetting("BaseAirplace", "Allows you to place midair with your blocks.", false);
    private final NumberSetting baseplaceRange = new NumberSetting("BaseplaceRange", "Max obsidian placement range", 5.0, 1.0, 6.0);
    private final NumberSetting baseplaceBPT = new NumberSetting("BaseBPT", "Blocks per tick for baseplace", 2, 1, 8);
    private final NumberSetting baseplaceDelay = new NumberSetting("BaseDelay", "Delay between baseplace placements (ms)", 50, 0, 500);
    private final NumberSetting antiSuicide = new NumberSetting("AntiSuicide", "Tries to prevent you from killing yourself while trying to crystal an enemy.", 3.0, 0.0, 20.0);
    private final NumberSetting placeDelay = new NumberSetting("PlaceDelay", "The delay in between each placement.", 0, 0, 1000);
    private final ModeSetting switching = new ModeSetting("Switch", "The mode that the ca uses to switch to the item you need to use.", "Silent", new String[]{"None", "Normal", "Silent", "Swap", "Pickup"});
    private final BooleanSetting sequential = new BooleanSetting("Sequential", "Does everything one after another, to prevent doing the same thing twice in a row.", true);
    private final BooleanSetting boost = new BooleanSetting("Boost", "Boosts your ca speed, by breaking crystals as they are added to the entity pooling list.", true);
    private final BooleanSetting strictDirection = new BooleanSetting("StrictDirection", "Only places crystals on faces you can see.", true);
    private final NumberSetting extrapolation = new NumberSetting("Extrapolation", "Extrapolates the target's position to calculate positions ahead of time.", 0, 0, 20);
    private final NumberSetting breakDelay = new NumberSetting("BreakDelay", "The delay in between each destruction of a crystal.", 0, 0, 1000);
    private final BooleanSetting whileHolding = new BooleanSetting("WhileHolding", "The ca will only work if you are holding a crystal in your hands.", false);
    private final BooleanSetting setDead = new BooleanSetting("SetDead", "Sets the entity as dead client-side, so you won't see it on your side.", false);
    private final BooleanSetting smart = new BooleanSetting("Smart", "Performs extra calculations when calculating the damage to yourself.", true);
    private final BooleanSetting useThread = new BooleanSetting("UseThread", "Multithreads the calculations so it is performed on a different thread than the breaking and placing.", true);
    private final BooleanSetting instantCalc = new BooleanSetting("InstantCalc", "Calculate instantly after breaking and placing.", false);
    private final BooleanSetting lite = new BooleanSetting("Lite", "Removes many calculations in order to give you better performance.", false);
    private final BooleanSetting terrainIgnore = new BooleanSetting("TerrainIgnore", "Ignores blocks that your ca will be able to break while calculating the damage.", true);
    private final BooleanSetting timeout = new BooleanSetting("Timeout", "Lower min damage after delay", true);
    private final NumberSetting timeoutDelay = new NumberSetting("TimeoutDelay", "Time before lowering min damage", 600, 0, 2000);
    private final NumberSetting timeoutMin = new NumberSetting("TimeoutMin", "Min damage during timeout", 1.5, 0.0, 36.0);
    private final BooleanSetting forcePlace = new BooleanSetting("ForcePlace", "Force place on low enemy health", true);
    private final NumberSetting forceMaxHealth = new NumberSetting("ForceMax", "Target health threshold for force place", 7.0, 0.0, 36.0);
    private final NumberSetting forceMin = new NumberSetting("ForceMin", "Min damage required for force place", 1.5, 0.0, 36.0);
    private final BooleanSetting armorBreaker = new BooleanSetting("ArmorBreaker", "Allows you to spam crystals in order to break their armor pieces.", true);
    private final NumberSetting durability = new NumberSetting("Durability", "The durability amount in order for you to start spamming crystals.", 8, 0, 100);
    private final NumberSetting armorBreakerDamage = new NumberSetting("BreakerMin", "The minimum damage that the ca needs to meet before it places a crystal to break their armor.", 3.0, 0.0, 36.0);
    private final NumberSetting hurtTime = new NumberSetting("HurtTime", "Delays calculations until their hurttime hits a certain limit.", 10, 0, 10);
    private final BooleanSetting fade = new BooleanSetting("Fade", "Fades the last position if you cannot use your autocrystal any longer.", true);

    private final BooleanSetting godSync = new BooleanSetting("GodSync", "Makes the attacking way faster by predicting entity IDs.", false);
    private final NumberSetting predictions = new NumberSetting("Predictions", "The amount of predictions that will be done after placing.", 10, 1, 20);
    private final NumberSetting offset = new NumberSetting("Offset", "The amount that the last entity ID should be offset by.", 0, 0, 2);
    private final ModeSetting godSwing = new ModeSetting("GodSwing", "The swinging that will be done for each predicted attack.", "Normal", new String[]{"None", "Normal", "Strict"});
    private final BooleanSetting antiKick = new BooleanSetting("AntiKick", "Prevents you from getting kicked by attacking invalid entity IDs.", false);
    private final NumberSetting kickThreshold = new NumberSetting("KickThreshold", "The tick threshold for the kick prevention.", 5, 1, 10);

    public final BooleanSetting debug = new BooleanSetting("Debug", "Show advanced information.", false);
    public final BooleanSetting infoTargetName = new BooleanSetting("Target", "Show target name.", true, () -> debug.getValue());
    public final BooleanSetting infoCalcTime = new BooleanSetting("CalculationTime", "Show calculation speed.", true, () -> debug.getValue());
    public final BooleanSetting infoCalcDmg = new BooleanSetting("CalculationDamage", "Show calculated damage.", true, () -> debug.getValue());
    public final BooleanSetting infoSelfDmg = new BooleanSetting("SelfDamage", "Show self damage.", true, () -> debug.getValue());
    public final BooleanSetting infoCounter = new BooleanSetting("CPSCounter", "Show crystal calculations count.", true, () -> debug.getValue());

    private final Timer switchTimer = new Timer();
    private final Timer delayTimer = new Timer();
    private final Timer placeTimer = new Timer();
    private final Timer breakTimer = new Timer();
    private final Timer obbyTimer = new Timer();
    private final Timer updateTimer = new Timer();
    private final Timer loopTimer = new Timer();
    private final Timer baseplaceTimer = new Timer();

    private volatile BlockPos crystalPos = null;
    private volatile BlockPos tempPos = null;
    private volatile BlockPos obsidianPos = null;

    private final ConcurrentHashMap<BlockPos, Long> baseplaceRender = new ConcurrentHashMap<>();
    private volatile float tempDamage = 0f;
    private volatile float tempSelfDamage = 0f;
    private volatile BlockPos currentCrystalBlock = null;

    private final ExecutorService calculationExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            new ThreadFactoryBuilder().setDaemon(true).setPriority(Thread.MAX_PRIORITY).build()
    );
    private Future<?> calculationTask;

    private int highestID = -100000;
    private int kickTicks = 0;
    private final ConcurrentHashMap<Integer, Long> attackedCrystals = new ConcurrentHashMap<>();
    private boolean flag = false;

    private String calcDamage = "0.00";
    private String calcSelfDamage = "0.00";
    private String calculationTime = "0.00ms";
    private int calculationCount = 0;
    private volatile PlayerEntity currentTarget = null;
    private final DecimalFormat df = new DecimalFormat("0.00");
    private final ConcurrentHashMap<BlockPos, Long> crystalFadeMap = new ConcurrentHashMap<>();
    @Setter
    public static volatile boolean externalPause = false;

    @Override
    public void onEnable() {
        if (getNull()) return;
        breakTimer.reset();
        obbyTimer.reset();
        if (useThread.getValue()) startThread();
    }

    @Override
    public void onDisable() {
        crystalPos = null;
        tempPos = null;
        obsidianPos = null;
        currentCrystalBlock = null;
        baseplaceRender.clear();
        crystalFadeMap.clear();
        attackedCrystals.clear();
        highestID = -100000;
        kickTicks = 0;
        externalPause = false;
        stopThread();
    }

    @SubscribeEvent
    public void onPacketReceive(Object event) {
        if (mc.player == null || mc.world == null) return;

        if (godSync.getValue()) {
            try {
                if (event.getClass().getSimpleName().equals("EntitySpawnS2CPacket")) {
                    int entityId = (int) event.getClass().getMethod("getEntityId").invoke(event);
                    if (entityId > highestID) highestID = entityId;
                }
            } catch (Exception ignored) {
            }
        }
    }

    @SubscribeEvent
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (getNull()) return;
        if (isExternalPause()) return;
        if (boost.getValue()) {
            if (crystalPos != null) doBreak(crystalPos);
        }
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull()) return;
        if (isExternalPause()) return;

        if (godSync.getValue())
            attackedCrystals.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 1000);

        if (useThread.getValue()) {
            if (calculationTask == null || calculationTask.isDone()) startThread();
            else if (updateTimer.hasTimeElapsed(updateDelay.getValue())) {
                updateCrystalPos();
                updateTimer.reset();
            }
        } else {
            updateCrystalPos();
        }

        if (!gameLoop.getValue()) {
            if (crystalPos != null) doCrystal(crystalPos);
            else if (baseplace.getValue() && obsidianPos != null && obbyTimer.hasTimeElapsed(250))
                doBaseplace(obsidianPos);
        }

        PacketMineFeature feature = Managers.FEATURE.getFeatureFromClass(PacketMineFeature.class);
        if (feature.isEnabled()) {
            PacketMineFeature.MiningData data = feature.getMiningData();
            if (data == null || data.getPos() == null) return;

            BlockPos miningPos = data.getPos();
            float progress = data.getBlockDamage() / feature.progressBreak.getValue().floatValue();
            PlayerEntity target = currentTarget;
            if (target == null) return;

            BlockPos topPos = miningPos.up();
            boolean isBlockBroken = mc.world.getBlockState(miningPos).isAir();

            PlayerEntity nearestEnemy = mc.world.getEntitiesByClass(PlayerEntity.class, new Box(miningPos).expand(1.5),
                            e -> e != mc.player && !Managers.FRIEND.isFriend(e.getName().getString()))
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (nearestEnemy != null && mc.world.getBlockState(miningPos).getBlock() == Blocks.OBSIDIAN) {
                if (!isBlockBroken) {
                    if (progress >= 0.95f) {
                        doPlace(miningPos);
                    }
                } else {
                    List<EndCrystalEntity> crystals = mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(topPos), e -> true);
                    if (!crystals.isEmpty()) doBreak(topPos);
                }
            }

            if (!isBlockBroken) {
                List<EndCrystalEntity> topCrystals = mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(topPos), e -> true);
                if (!topCrystals.isEmpty()) doBreak(topPos);

                if (progress > 0.92f && mc.world.getBlockState(topPos.up()).isAir()) {
                    doPlace(topPos);
                    doBreak(topPos);
                }
            } else {
                List<BlockPos> sideSpots = Simulations.getSidePlacementSpots(miningPos, target, 6.0f);
                for (BlockPos sidePos : sideSpots) {
                    List<EndCrystalEntity> sideCrystals = mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(sidePos.up()), e -> true);
                    if (!sideCrystals.isEmpty()) doBreak(sidePos.up());
                    doPlace(sidePos);
                    doBreak(sidePos);
                }
            }
        }
    }

    @SubscribeEvent
    public void onGameLoop(GameLoopEvent event) {
        if (getNull()) return;
        if (isExternalPause()) return;

        if (gameLoop.getValue()) {
            if (!loopTimer.hasTimeElapsed(loopDelay.getValue().longValue())) return;

            if (crystalPos != null) doCrystal(crystalPos);
            else if (baseplace.getValue() && obsidianPos != null && obbyTimer.hasTimeElapsed(250))
                doBaseplace(obsidianPos);

            loopTimer.reset();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (getNull()) return;
        RendersFeature renders = Managers.FEATURE.getFeatureFromClass(RendersFeature.class);
        if (renders == null || !renders.isEnabled()) return;

        long now = System.currentTimeMillis();
        long lifetime = renders.getRenderTimeMillis();

        BlockPos targetBlock = crystalPos != null ? crystalPos.down() : (obsidianPos != null ? obsidianPos : null);

        if (targetBlock != null) {
            if (!targetBlock.equals(currentCrystalBlock)) {
                if (currentCrystalBlock != null && !fade.getValue()) crystalFadeMap.put(currentCrystalBlock, now);
                currentCrystalBlock = targetBlock;
            }
        } else if (currentCrystalBlock != null) {
            crystalFadeMap.put(currentCrystalBlock, now);
            currentCrystalBlock = null;
        }

        if (currentCrystalBlock != null) {
            Box box = new Box(currentCrystalBlock);
            Renderer3D.renderBox(event.getContext(), box, ColorUtils.getGlobalColor(55));
            Renderer3D.renderBoxOutline(event.getContext(), box, ColorUtils.getGlobalColor());
        }

        Iterator<ConcurrentHashMap.Entry<BlockPos, Long>> fadeIt = crystalFadeMap.entrySet().iterator();
        while (fadeIt.hasNext()) {
            var e = fadeIt.next();
            BlockPos pos = e.getKey();
            long age = now - e.getValue();
            if (age > lifetime) {
                fadeIt.remove();
                continue;
            }

            float progress = 1.0f - (age / (float) lifetime);
            Box box = new Box(pos);

            Color c = ColorUtils.getGlobalColor();
            Color fill = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (55 * progress));
            Color line = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * progress));

            Renderer3D.renderBox(event.getContext(), box, fill);
            Renderer3D.renderBoxOutline(event.getContext(), box, line);
        }

        Color block = renders.getBlockColor();
        int fillAlpha = block.getAlpha();

        Iterator<ConcurrentHashMap.Entry<BlockPos, Long>> baseIt = baseplaceRender.entrySet().iterator();
        while (baseIt.hasNext()) {
            var e = baseIt.next();
            BlockPos pos = e.getKey();
            long age = now - e.getValue();
            if (age > lifetime) {
                baseIt.remove();
                continue;
            }

            float p = 1.0f - (age / (float) lifetime);
            Box box = new Box(pos);

            Color fill = new Color(block.getRed(), block.getGreen(), block.getBlue(), (int) (fillAlpha * p));
            Color line = new Color(block.getRed(), block.getGreen(), block.getBlue(), 255);

            Renderer3D.renderBox(event.getContext(), box, fill);
            Renderer3D.renderBoxOutline(event.getContext(), box, line);
        }
    }

    private void startThread() {
        stopThread();
        calculationTask = calculationExecutor.submit(() -> {
            while (isEnabled() && useThread.getValue() && !Thread.currentThread().isInterrupted()) {
                try {
                    updateCrystalPos();
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private void stopThread() {
        if (calculationTask != null) {
            calculationTask.cancel(true);
            calculationTask = null;
        }
    }

    private void updateCrystalPos() {
        long startTime = System.nanoTime();
        int totalSpotsChecked = 0;
        update();
        calculationTime = df.format((System.nanoTime() - startTime) / 1000000.0) + "ms";
        calculationCount = totalSpotsChecked;
        crystalPos = tempPos;
    }

    private void update() {
        if (getNull()) return;

        if (isExternalPause()) {
            resetCalc();
            return;
        }

        calcDamage = df.format(tempDamage);
        calcSelfDamage = df.format(tempSelfDamage);

        if (!delayTimer.hasTimeElapsed(updateDelay.getValue().longValue())) return;

        if (eatingPause.getValue() && mc.player.isUsingItem()) {
            resetCalc();
            return;
        }

        if (whileHolding.getValue() &&
                !(mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL || mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) &&
                findCrystalSlot() == -1) {
            resetCalc();
            return;
        }

        if (!switchTimer.hasTimeElapsed(switchPause.getValue().longValue())) return;

        delayTimer.reset();

        tempPos = null;
        obsidianPos = null;
        tempDamage = 0f;

        List<PlayerEntity> enemies = new ArrayList<>();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive()) continue;
            if (Managers.FRIEND.isFriend(p.getName().getString())) continue;
            if (mc.player.squaredDistanceTo(p) > MathHelper.square(targetRange.getValue().floatValue())) continue;
            enemies.add(p);
        }

        if (enemies.isEmpty()) {
            resetCalc();
            return;
        }

        PlayerEntity bestTarget = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        BlockPos bestPos = null;
        float bestDmg = 0f;
        BlockPos bestObby = null;
        float bestObbyDmg = 0f;

        int r = (int) Math.ceil(range.getValue().floatValue() + 1f);
        BlockPos base = mc.player.getBlockPos();

        for (PlayerEntity t : enemies) {
            BlockPos targetBest = null;
            float targetDmg = 0f;
            BlockPos targetObby = null;
            float targetObbyDmg = 0f;
            BlockPos targetFeet = t.getBlockPos();

            for (int dx = -r; dx <= r; dx++)
                for (int dy = -2; dy <= 2; dy++)
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos pos = base.add(dx, dy, dz);
                        if (behindWall(pos)) continue;

                        if (mc.player.getEyePos().distanceTo(pos.toCenterPos().add(0, -0.5, 0)) > range.getValue().floatValue())
                            continue;
                        if (canTouch(pos.down())) continue;
                        if (t.hurtTime > hurtTime.getValue().intValue()) continue;
                        if (lite.getValue() && liteCheck(pos.toCenterPos().add(0, -0.5, 0), t.getPos())) continue;

                        float dmg = CrystalUtils.calculateDamage(pos, t, t, terrainIgnore.getValue());
                        float self = CrystalUtils.calculateDamage(pos, mc.player, mc.player, terrainIgnore.getValue());

                        if (self > maxSelf.getValue().floatValue()) continue;
                        if (antiSuicide.getValue().floatValue() > 0f && self > PlayerUtils.getHealth(mc.player) - antiSuicide.getValue().floatValue())
                            continue;

                        boolean validPlace = CrystalUtils.canPlaceCrystal(pos, true, false);
                        boolean face = CrystalUtils.isFacePlace(pos, t);
                        float minAllowed = getDamageMinFor(t);

                        if (validPlace) {
                            if (face) {
                                if (!forcePlace.getValue() || CrystalUtils.getTotalHealth(t) > forceMaxHealth.getValue().floatValue())
                                    continue;
                                if (dmg < forceMin.getValue().floatValue()) continue;
                            } else {
                                if (dmg < minAllowed) continue;

                                if (smart.getValue() && dmg < CrystalUtils.getTotalHealth(t)) {
                                    if (minAllowed == forceMin.getValue().floatValue()) {
                                        if (dmg < self - 2.5f) continue;
                                    } else {
                                        if (dmg < self) continue;
                                    }
                                }
                            }

                            if (targetBest == null || dmg > targetDmg) {
                                targetBest = pos;
                                targetDmg = dmg;
                                tempSelfDamage = self;
                            }
                        } else if (baseplace.getValue() && self <= maxSelf.getValue().floatValue()) {
                            BlockPos obbyPos = pos.down();

                            boolean isFacePlaceLevel = pos.getY() > targetFeet.getY();

                            if (isFacePlaceLevel && !forcePlace.getValue()) continue;
                            if (isFacePlaceLevel && forcePlace.getValue() && CrystalUtils.getTotalHealth(t) > forceMaxHealth.getValue().floatValue())
                                continue;

                            if (mc.player.getEyePos().distanceTo(obbyPos.toCenterPos()) > baseplaceRange.getValue().floatValue())
                                continue;
                            if (!mc.world.getBlockState(obbyPos).isReplaceable()) continue;
                            if (!mc.world.getBlockState(pos).isAir() && !mc.world.getBlockState(pos).isOf(Blocks.FIRE))
                                continue;
                            if (isEntityBlocking(obbyPos)) continue;
                            if (!baseplaceAir.getValue() && getHitResult(obbyPos) == null) continue;
                            if (dmg < minDamage.getValue().floatValue()) continue;

                            int horizontalDist = Math.max(Math.abs(obbyPos.getX() - targetFeet.getX()), Math.abs(obbyPos.getZ() - targetFeet.getZ()));

                            if (targetObby == null ||
                                    dmg > targetObbyDmg + 0.4f ||
                                    (Math.abs(dmg - targetObbyDmg) < 0.6f && horizontalDist <
                                            Math.max(Math.abs(targetObby.getX() - targetFeet.getX()), Math.abs(targetObby.getZ() - targetFeet.getZ())))) {
                                targetObby = obbyPos;
                                targetObbyDmg = dmg;
                            }
                        }
                    }

            if (targetBest != null) {
                float score = targetDmg - mc.player.distanceTo(t);
                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = t;
                    bestPos = targetBest;
                    bestDmg = targetDmg;
                }
            }

            if (targetObby != null && (bestObby == null || targetObbyDmg > bestObbyDmg)) {
                bestObby = targetObby;
                bestObbyDmg = targetObbyDmg;
            }
        }

        if (bestTarget != null) {
            tempPos = bestPos;
            tempDamage = bestDmg;
            tempSelfDamage = CrystalUtils.calculateDamage(bestPos, mc.player, mc.player, terrainIgnore.getValue());

            if (instantCalc.getValue() && tempPos != null) doCrystal(tempPos);
        } else if (bestObby != null && tempPos == null) {
            obsidianPos = bestObby;
            tempDamage = bestObbyDmg;
            tempSelfDamage = CrystalUtils.calculateDamage(bestObby.up(), mc.player, mc.player, terrainIgnore.getValue());
        } else tempSelfDamage = 0f;

        currentTarget = bestTarget;
        calcDamage = new DecimalFormat("0.00").format(tempDamage);
        calcSelfDamage = new DecimalFormat("0.00").format(tempSelfDamage);
    }

    private void resetCalc() {
        calcDamage = "0.00";
        calcSelfDamage = "0.00";
        calculationTime = "0.00ms";
        calculationCount = 0;
        currentTarget = null;
        tempPos = null;
        obsidianPos = null;
    }

    public void doCrystal(BlockPos pos) {
        if (getNull()) return;
        if (isExternalPause()) return;

        if (CrystalUtils.canPlaceCrystal(pos, false, true)) {
            if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL || mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL || findCrystalSlot() != -1)
                doPlace(pos);
        } else {
            doBreak(pos);
        }
    }

    public void doBreakFromOtherModule(BlockPos pos) {
        doBreakInternal(pos, true, false);
    }

    private void doBreak(BlockPos pos) {
        doBreakInternal(pos, false, true);
    }

    private void doBreakInternal(BlockPos pos, boolean force, boolean allowPlace) {
        if (isExternalPause()) return;
        if (!force && !breakTimer.hasTimeElapsed(breakDelay.getValue().longValue())) return;

        Box area = new Box(pos).expand(1).offset(0, 1, 0);

        for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class, area, e -> true)) {
            breakTimer.reset();

            if (rotate.getValue()) {
                float[] rotations = RotationUtils.getRotationsTo(mc.player.getEyePos(), c.getPos());
                Services.ROTATION.setRotationPoint(new RotationPoint(rotations[0], rotations[1], 100, true));
            }

            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(c, mc.player.isSneaking()));
            Hand hand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL ? Hand.OFF_HAND : Hand.MAIN_HAND;
            mc.player.swingHand(hand);
            mc.player.resetLastAttackedTicks();
            if (!mc.player.isOnGround()) mc.player.addCritParticles(c);

            if (setDead.getValue()) mc.world.removeEntity(c.getId(), Entity.RemovalReason.KILLED);
            if (godSync.getValue()) doGodSync();

            if (allowPlace && tempDamage >= minDamage.getValue().floatValue() && sequential.getValue()) doPlace(pos);
            break;
        }
    }

    private void doGodSync() {
        boolean flag = !antiKick.getValue() || mc.player.getMainHandStack().getItem() == Items.OBSIDIAN;
        if ((!antiKick.getValue() || kickTicks > kickThreshold.getValue().intValue()) && flag) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity.getId() <= highestID) continue;
                highestID = entity.getId();
            }

            for (int i = 1 - offset.getValue().intValue(); i < predictions.getValue().intValue(); ++i) {
                Entity entity = mc.world.getEntityById(highestID);
                if (entity == null || entity instanceof EndCrystalEntity) {
                    int id = highestID + i;

                    PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(mc.player, mc.player.isSneaking());
                    try {
                        Field field = PlayerInteractEntityC2SPacket.class.getDeclaredField("entityId");
                        field.setAccessible(true);
                        field.setInt(packet, id);
                    } catch (Exception ignored) {
                    }

                    mc.player.networkHandler.sendPacket(packet);
                    attackedCrystals.put(id, System.currentTimeMillis());

                    if (godSwing.getValue().equals("Strict")) {
                        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    }
                }
            }

            if (godSwing.getValue().equals("Normal")) {
                mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }
        kickTicks++;
    }

    public void doPlace(BlockPos pos) {
        if (isExternalPause()) return;

        if (flag) {
            flag = false;
            return;
        }

        if ((!(mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL || mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) &&
                findCrystalSlot() == -1) || canTouch(pos.down()))
            return;

        Direction side = CrystalUtils.clickSide(pos.down(), strictDirection.getValue());
        if (side == null) return;

        if (!placeTimer.hasTimeElapsed(placeDelay.getValue().longValue())) return;

        if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL || mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) {
            placeCrystal(pos.down(), side);
            return;
        }

        int old = mc.player.getInventory().getSelectedSlot();
        int crystal = findCrystalSlot();
        if (crystal == -1) return;

        doSwitch(crystal);
        placeCrystal(pos.down(), side);
        doSwitch(old);

        placeTimer.reset();
    }

    private void doBaseplace(BlockPos pos) {
        if (isExternalPause()) return;
        if (!baseplace.getValue()) return;
        if (!mc.world.getBlockState(pos).isReplaceable()) return;
        if (isEntityBlocking(pos)) return;
        if (!baseplaceTimer.hasTimeElapsed(baseplaceDelay.getValue().intValue())) return;

        Slot obbySlot = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.OBSIDIAN);
        if (obbySlot == null) return;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        boolean switched = false;
        if (mc.player.getMainHandStack().getItem() != Items.OBSIDIAN) {
            Services.INVENTORY.switchTo(obbySlot.getIndex(), SwitchType.Silent);
            switched = true;
        }

        for (int i = 0; i < baseplaceBPT.getValue().intValue(); i++) {
            BlockHitResult hit = getHitResult(pos);
            if (hit != null) {
                if (rotate.getValue()) {
                    float[] rotations = RotationUtils.getRotationsTo(mc.player.getEyePos(), hit.getPos());
                    Services.ROTATION.setRotationPoint(new RotationPoint(rotations[0], rotations[1], 100, true));
                }
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);
                baseplaceRender.put(pos, System.currentTimeMillis());
            }
        }

        if (switched) Services.INVENTORY.switchTo(oldSlot, SwitchType.Silent);
        baseplaceTimer.reset();
    }

    private boolean isEntityBlocking(BlockPos pos) {
        Box box = new Box(pos);
        return !mc.world.getEntitiesByClass(Entity.class, box, e -> !(e instanceof PlayerEntity && e == mc.player)).isEmpty();
    }

    private BlockHitResult getHitResult(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.offset(dir.getOpposite());
            BlockState state = mc.world.getBlockState(adj);
            if (!state.isAir() && !state.isReplaceable()) {
                Vec3d hitVec = Vec3d.ofCenter(adj).add(Vec3d.of(dir.getVector()).multiply(0.5));
                return new BlockHitResult(hitVec, dir, adj, false);
            }
        }
        if (baseplaceAir.getValue()) {
            return new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        }
        return null;
    }

    private void placeCrystal(BlockPos obs, Direction side) {
        Vec3d hit = obs.toCenterPos().add(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5);
        if (side.getAxis().isHorizontal()) hit = hit.add(0, 0.45, 0);

        if (rotate.getValue()) {
            float[] rotations = RotationUtils.getRotationsTo(mc.player.getEyePos(), hit);
            Services.ROTATION.setRotationPoint(new RotationPoint(rotations[0], rotations[1], 100, true));
        }

        BlockHitResult bhr = new BlockHitResult(hit, side, obs, false);
        Hand hand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL ? Hand.OFF_HAND : Hand.MAIN_HAND;

        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, bhr, 0));
        mc.player.swingHand(hand);
    }

    private int findCrystalSlot() {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL)
                return i;
        return -1;
    }

    private boolean canTouch(BlockPos pos) {
        Direction s = CrystalUtils.clickSide(pos, strictDirection.getValue());
        return s == null || !(pos.toCenterPos().add(s.getVector().getX() * 0.5, s.getVector().getY() * 0.5, s.getVector().getZ() * 0.5).distanceTo(mc.player.getEyePos()) <= range.getValue().floatValue());
    }

    private boolean behindWall(BlockPos pos) {
        Vec3d test = pos.toCenterPos().add(0, 1.7, 0);
        HitResult hit = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), test, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() != HitResult.Type.MISS && mc.player.getEyePos().distanceTo(pos.toCenterPos().add(0, -0.5, 0)) > wallRange.getValue().floatValue();
    }

    private static boolean liteCheck(Vec3d from, Vec3d to) {
        return raycastVisible(from, to) && raycastVisible(from, to.add(0, 1.8, 0));
    }

    private static boolean raycastVisible(Vec3d from, Vec3d to) {
        HitResult res = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return res != null && res.getType() != HitResult.Type.MISS;
    }

    private float getDamageMinFor(PlayerEntity t) {
        if (timeout.getValue() && breakTimer.hasTimeElapsed(timeoutDelay.getValue().longValue()))
            return timeoutMin.getValue().floatValue();

        if (forcePlace.getValue() && CrystalUtils.getTotalHealth(t) <= forceMaxHealth.getValue().floatValue())
            return forceMin.getValue().floatValue();

        if (armorBreaker.getValue()) {
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack st = t.getEquippedStack(s);
                if (st.isEmpty() || st.getMaxDamage() == 0) continue;
                int pct = Math.round((st.getMaxDamage() - st.getDamage()) * 100f / st.getMaxDamage());
                if (pct <= durability.getValue().intValue()) return armorBreakerDamage.getValue().floatValue();
            }
        }

        return minDamage.getValue().floatValue();
    }

    public void doSwitch(int slot) {
        if (!switching.getValue().equalsIgnoreCase("None")) {
            Services.INVENTORY.switchTo(slot, getSwitchType());
        }
    }

    private SwitchType getSwitchType() {
        switch (switching.getValue()) {
            case "Normal" -> {
                return SwitchType.Normal;
            }
            case "Silent" -> {
                return SwitchType.Silent;
            }
            case "Swap" -> {
                return SwitchType.Swap;
            }
            case "Pickup" -> {
                return SwitchType.PickUp;
            }
        }
        return SwitchType.None;
    }

    private static boolean isExternalPause() {
        return externalPause;
    }

    @Override
    public String getInfo() {
        PlayerEntity target = currentTarget;

        if (!debug.getValue()) {
            String info = calcDamage + ", " + calcSelfDamage;
            if (target != null) info += ", " + target.getName().getString();
            return info;
        }

        String info = "";

        if (infoCalcTime.getValue()) info += calculationTime;

        if (infoCalcDmg.getValue()) {
            if (!info.isEmpty()) info += ", ";
            info += calcDamage;
        }

        if (infoSelfDmg.getValue()) {
            if (!info.isEmpty()) info += ", ";
            info += calcSelfDamage;
        }

        if (infoTargetName.getValue() && target != null) {
            if (!info.isEmpty()) info += ", ";
            info += target.getName().getString();
        }

        if (infoCounter.getValue()) {
            if (!info.isEmpty()) info += ", ";
            info += calculationCount;
        }

        return info.isEmpty() ? null : info;
    }
}