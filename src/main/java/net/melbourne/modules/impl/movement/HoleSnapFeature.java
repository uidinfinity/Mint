package net.melbourne.modules.impl.movement;

import net.melbourne.Managers;
import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.MoveEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.events.impl.SettingChangeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.mixins.accessors.Vec3dAccessor;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.client.AntiCheatFeature;
import net.melbourne.modules.impl.player.FakeLagFeature;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.settings.types.WhitelistSetting;
import net.melbourne.utils.block.hole.HoleUtils;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.entity.player.movement.MovementUtils;
import net.melbourne.pathfinding.movement.TravelPathfinding;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@FeatureInfo(name = "HoleSnap", category = Category.Movement)
public class HoleSnapFeature extends Feature {

    public NumberSetting range = new NumberSetting("Range", "Range for the holesnap.", 5, 1, 8);
    public final WhitelistSetting holes = new WhitelistSetting("Holes", "Hole types to snap to", WhitelistSetting.Type.CUSTOM, new String[]{}, new String[]{"Single", "Double", "Quad", "Incomplete"});
    public BooleanSetting step = new BooleanSetting("Step", "Automatically steps when trying to holesnap.", false);
    public BooleanSetting timer = new BooleanSetting("Timer", "Activates timer when you snap into a hole.", true);
    public NumberSetting timerMultiplier = new NumberSetting("TimerMultiplier", "The world timer multiplier while snapping into a hole.", 2.f, 1.f, 3.f, () -> timer.getValue());
    public ColorSetting color = new ColorSetting("Color", "The color of the trail.", new Color(-1));
    public BooleanSetting swapping = new BooleanSetting("Swapping", "Swaps to another hole if one is available and within range.", false);
    public NumberSetting swappingRange = new NumberSetting("SwappingRange", "The range in which a new hole is searched when swapping.", 3, 1, 8, () -> swapping.getValue());
    public BooleanSetting reRoute = new BooleanSetting("ReRoute", "Tries to find a new hole if the targeted one is filled/gone.", true);
    public BooleanSetting pathfinding = new BooleanSetting("Pathfinding", "Use A* pathfinding to reach the hole.", true);

    public BooleanSetting fakeLag = new BooleanSetting("FakeLag", "Enable FakeLag while HoleSnap is enabled.", false);

    private boolean toggledFakeLag = false;

    private HoleUtils.Hole hole;
    private BlockPos holeAnchor;
    private List<BlockPos> currentPath = Collections.emptyList();
    private int pathIndex = 0;
    private boolean physicsOnce = true;
    private int ticksSinceLastPath = 0;
    private int stuckTicks = 0;
    private double lastDistToNode = Double.MAX_VALUE;

    private static final double HOLE_TOLERANCE = 0.15;
    private static final double NODE_ADVANCE_DISTANCE = 0.6;
    private static final int STUCK_TICK_LIMIT = 8;
    private static final double STUCK_EPS = 0.002;

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) { setToggled(false); return; }
        if (Managers.FEATURE.getFeatureFromClass(SpeedFeature.class).isEnabled()) Managers.FEATURE.getFeatureFromClass(SpeedFeature.class).setToggled(false);

        physicsOnce = true;
        hole = null;
        holeAnchor = null;
        currentPath = Collections.emptyList();
        pathIndex = 0;
        ticksSinceLastPath = 0;
        stuckTicks = 0;
        lastDistToNode = Double.MAX_VALUE;

        toggledFakeLag = false;

        Optional<AnchoredHole> currentHoleOpt = swapping.getValue() ? findCurrentHole() : Optional.empty();
        boolean isPlayerInHole = currentHoleOpt.isPresent();

        if (swapping.getValue() && isPlayerInHole) {
            AnchoredHole current = currentHoleOpt.get();
            AnchoredHole next = getBestOtherHole(swappingRange.getValue().intValue(), current);
            if (next != null) {
                this.hole = next.hole;
                this.holeAnchor = next.anchor;
            } else {
                disableSnap();
                return;
            }
        } else {
            List<AnchoredHole> foundHoles = getHoles(range.getValue().intValue());
            if (!foundHoles.isEmpty()) {
                this.hole = foundHoles.get(0).hole;
                this.holeAnchor = foundHoles.get(0).anchor;
            } else {
                disableSnap();
                return;
            }
        }

        if (pathfinding.getValue() && hole != null) recalculatePath();
        if (step.getValue()) Managers.FEATURE.getFeatureFromClass(StepFeature.class).setEnabled(true);

        startFakeLagIfNeeded();
    }

    private void disableSnap() {
        this.hole = null;
        this.holeAnchor = null;
        Services.WORLD.resetTimerMultiplier();
        stopFakeLagIfNeeded();
        setToggled(false);
    }

    @Override
    public void onDisable() {
        if (mc.player == null || mc.world == null) return;
        if (Managers.FEATURE.getFeatureFromClass(StepFeature.class).isEnabled()) Managers.FEATURE.getFeatureFromClass(StepFeature.class).setToggled(false);
        Services.WORLD.resetTimerMultiplier();
        stopFakeLagIfNeeded();
        physicsOnce = true;
        hole = null;
        holeAnchor = null;
        currentPath = Collections.emptyList();
        pathIndex = 0;
        ticksSinceLastPath = 0;
        stuckTicks = 0;
        lastDistToNode = Double.MAX_VALUE;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || !isEnabled()) return;

        ticksSinceLastPath++;

        if (hole == null || holeAnchor == null || !checkHoleValidity(hole, holeAnchor)) {
            if (reRoute.getValue()) {
                List<AnchoredHole> found = getHoles(range.getValue().intValue());
                if (!found.isEmpty()) {
                    hole = found.get(0).hole;
                    holeAnchor = found.get(0).anchor;
                    stuckTicks = 0;
                    lastDistToNode = Double.MAX_VALUE;
                    if (pathfinding.getValue()) recalculatePath();
                    return;
                }
            }
            disableSnap();
            return;
        }

        if (pathfinding.getValue()) {
            if (!currentPath.isEmpty() && pathIndex < currentPath.size()) {
                BlockPos nextPos = currentPath.get(pathIndex);
                Vec3d nodeCenter = new Vec3d(nextPos.getX() + 0.5, nextPos.getY(), nextPos.getZ() + 0.5);
                double dist = mc.player.getPos().distanceTo(nodeCenter);

                if (dist < NODE_ADVANCE_DISTANCE) {
                    pathIndex++;
                    stuckTicks = 0;
                    lastDistToNode = Double.MAX_VALUE;
                } else {
                    if (lastDistToNode != Double.MAX_VALUE && dist >= lastDistToNode - STUCK_EPS) stuckTicks++;
                    else stuckTicks = 0;
                    lastDistToNode = dist;

                    if (stuckTicks >= STUCK_TICK_LIMIT) {
                        stuckTicks = 0;
                        lastDistToNode = Double.MAX_VALUE;
                        recalculatePath();
                    }
                }
            }

            if (ticksSinceLastPath > 6 && (currentPath.isEmpty() || pathIndex >= currentPath.size())) recalculatePath();
        }
    }

    private void recalculatePath() {
        if (hole == null || holeAnchor == null || !isEnabled()) {
            currentPath = Collections.emptyList();
            pathIndex = 0;
            return;
        }
        ticksSinceLastPath = 0;

        BlockPos start = mc.player.getBlockPos();
        Set<BlockPos> goals = buildHoleGoals(hole);
        currentPath = TravelPathfinding.findPath(start, goals, range.getValue().intValue());
        pathIndex = !currentPath.isEmpty() ? 1 : 0;
    }

    private Set<BlockPos> buildHoleGoals(HoleUtils.Hole h) {
        Set<BlockPos> goals = new HashSet<>();
        if (h == null) return goals;

        Box b = h.box();
        int minX = (int) Math.floor(b.minX);
        int maxX = (int) Math.ceil(b.maxX) - 1;
        int minZ = (int) Math.floor(b.minZ);
        int maxZ = (int) Math.ceil(b.maxZ) - 1;
        int y = (int) Math.floor(b.minY);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                goals.add(new BlockPos(x, y, z));
            }
        }

        if (goals.isEmpty() && holeAnchor != null) goals.add(holeAnchor);
        return goals;
    }

    private boolean checkHoleValidity(HoleUtils.Hole h, BlockPos anchor) {
        if (h == null || anchor == null) return false;

        if (holes.getWhitelistIds().contains("Incomplete") && HoleUtils.isIncompleteHole(anchor)) return true;

        if (holes.getWhitelistIds().contains("Single")) {
            HoleUtils.Hole s = HoleUtils.getSingleHole(anchor, 1);
            if (s != null) return true;
        }
        if (holes.getWhitelistIds().contains("Double")) {
            HoleUtils.Hole d = HoleUtils.getDoubleHole(anchor, 1);
            if (d != null) return true;
        }
        if (holes.getWhitelistIds().contains("Quad")) {
            HoleUtils.Hole q = HoleUtils.getQuadHole(anchor, 1);
            if (q != null) return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onMove(MoveEvent event) {
        if (mc.player == null || mc.world == null || !isEnabled() || hole == null) return;

        Vec3d target;
        Box box = hole.box();
        Vec3d center = box.getCenter();

        if (pathfinding.getValue() && !currentPath.isEmpty() && pathIndex < currentPath.size()) {
            BlockPos nextPos = currentPath.get(pathIndex);
            target = new Vec3d(nextPos.getX() + 0.5, nextPos.getY(), nextPos.getZ() + 0.5);
        } else {
            target = center;
            if (Math.abs(mc.player.getX() - center.x) < HOLE_TOLERANCE &&
                    Math.abs(mc.player.getZ() - center.z) < HOLE_TOLERANCE &&
                    Math.abs(mc.player.getY() - box.minY) < 0.5) {
                disableSnap();
                return;
            }
        }

        handleTimer();

        double speed = MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED);

        Vec3d to = target.subtract(mc.player.getPos());
        double len = Math.hypot(to.x, to.z);
        if (len < 1.0E-4) {
            event.setCancelled(true);
            return;
        }

        double dirX = to.x / len;
        double dirZ = to.z / len;

        double moveX = dirX * speed;
        double moveZ = dirZ * speed;

        double diffX = target.x - mc.player.getX();
        double diffZ = target.z - mc.player.getZ();

        event.setMovement(new Vec3d(
                Math.abs(moveX) < Math.abs(diffX) ? moveX : diffX,
                event.getMovement().getY(),
                Math.abs(moveZ) < Math.abs(diffZ) ? moveZ : diffZ
        ));

        ((Vec3dAccessor) mc.player.getVelocity()).setX(0);
        ((Vec3dAccessor) mc.player.getVelocity()).setZ(0);
        event.setCancelled(true);
    }

    private void handleTimer() {
        if (timer.getValue()) {
            AntiCheatFeature antiCheat = (AntiCheatFeature) Managers.FEATURE.getFeatureByName("AntiCheat");
            String mode = antiCheat != null && antiCheat.isEnabled() ? antiCheat.timerMode.getValue() : "Normal";
            if (mode.equals("Physics")) {
                if (physicsOnce) {
                    physicsOnce = false;
                    for (int i = 0; i < timerMultiplier.getValue().intValue(); i++) mc.player.tick();
                }
            } else {
                Services.WORLD.setTimerMultiplier(timerMultiplier.getValue().floatValue());
            }
        } else {
            Services.WORLD.resetTimerMultiplier();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.player == null || mc.world == null || !isEnabled() || hole == null) return;
        Vec3d renderTarget = (pathfinding.getValue() && !currentPath.isEmpty() && pathIndex < currentPath.size())
                ? new Vec3d(currentPath.get(pathIndex).getX() + 0.5, currentPath.get(pathIndex).getY(), currentPath.get(pathIndex).getZ() + 0.5)
                : hole.box().getCenter();
        Renderer3D.renderLine(event.getContext(), renderTarget, mc.player.getPos(), color.getColor(), color.getColor());
    }

    private List<AnchoredHole> getHoles(int r) {
        List<AnchoredHole> found = new ArrayList<>();
        BlockPos playerPos = mc.player.getBlockPos();
        double rSq = r * r;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > rSq) continue;
                    if (pos.getY() > mc.player.getY() + 1) continue;

                    if (holes.getWhitelistIds().contains("Single")) {
                        HoleUtils.Hole h = HoleUtils.getSingleHole(pos, 1);
                        if (h != null) found.add(new AnchoredHole(h, getAnchorFromBox(h.box())));
                    }
                    if (holes.getWhitelistIds().contains("Double")) {
                        HoleUtils.Hole h = HoleUtils.getDoubleHole(pos, 1);
                        if (h != null) found.add(new AnchoredHole(h, getAnchorFromBox(h.box())));
                    }
                    if (holes.getWhitelistIds().contains("Quad")) {
                        HoleUtils.Hole h = HoleUtils.getQuadHole(pos, 1);
                        if (h != null) found.add(new AnchoredHole(h, getAnchorFromBox(h.box())));
                    }
                    if (holes.getWhitelistIds().contains("Incomplete")) {
                        if (HoleUtils.isIncompleteHole(pos)) {
                            Box b = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);
                            found.add(new AnchoredHole(new HoleUtils.Hole(b, HoleUtils.HoleType.INCOMPLETE, HoleUtils.HoleSafety.OBSIDIAN), pos));
                        }
                    }
                }
            }
        }

        found.sort(Comparator.comparing(a -> mc.player.squaredDistanceTo(a.hole.box().getCenter().x, a.hole.box().getCenter().y, a.hole.box().getCenter().z)));
        return dedupeByAnchor(found);
    }

    private List<AnchoredHole> dedupeByAnchor(List<AnchoredHole> holes) {
        List<AnchoredHole> out = new ArrayList<>();
        HashSet<BlockPos> seen = new HashSet<>();
        for (AnchoredHole h : holes) {
            if (h.anchor == null) continue;
            if (seen.add(h.anchor)) out.add(h);
        }
        return out;
    }

    private BlockPos getAnchorFromBox(Box b) {
        int ax = (int) Math.floor(b.minX);
        int ay = (int) Math.floor(b.minY);
        int az = (int) Math.floor(b.minZ);
        return new BlockPos(ax, ay, az);
    }

    private Optional<AnchoredHole> findCurrentHole() {
        List<AnchoredHole> list = getHoles(swappingRange.getValue().intValue());
        return list.stream()
                .filter(h -> isPlayerInsideHole(h.hole))
                .max(Comparator.<AnchoredHole>comparingDouble(h -> holeAreaXZ(h.hole)).thenComparingDouble(h -> -playerDistanceToHoleInterior(h.hole)));
    }

    private AnchoredHole getBestOtherHole(int r, AnchoredHole current) {
        Vec3d currentCenter = current.hole.box().getCenter();
        double currentArea = holeAreaXZ(current.hole);

        return getHoles(r).stream()
                .filter(h -> h.anchor != null && !h.anchor.equals(current.anchor))
                .filter(h -> h.hole.box().getCenter().distanceTo(currentCenter) > 0.05)
                .min(Comparator.<AnchoredHole>comparingInt(h -> h.hole.box().getCenter().distanceTo(currentCenter) < 0.0001 ? 1 : 0)
                        .thenComparingDouble(h -> Math.abs(holeAreaXZ(h.hole) - currentArea))
                        .thenComparingDouble(h -> mc.player.squaredDistanceTo(h.hole.box().getCenter().x, h.hole.box().getCenter().y, h.hole.box().getCenter().z)))
                .orElse(null);
    }

    private boolean isPlayerInsideHole(HoleUtils.Hole h) {
        if (h == null) return false;
        Box b = h.box();
        double x = mc.player.getX();
        double z = mc.player.getZ();
        double y = mc.player.getY();
        double eps = 1.0E-3;
        boolean inX = x > b.minX + eps && x < b.maxX - eps;
        boolean inZ = z > b.minZ + eps && z < b.maxZ - eps;
        boolean nearY = Math.abs(y - b.minY) < 1.05;
        return inX && inZ && nearY;
    }

    private double holeAreaXZ(HoleUtils.Hole h) {
        if (h == null) return 0.0;
        Box b = h.box();
        double w = Math.max(0.0, b.maxX - b.minX);
        double d = Math.max(0.0, b.maxZ - b.minZ);
        return w * d;
    }

    private double playerDistanceToHoleInterior(HoleUtils.Hole h) {
        if (h == null) return Double.MAX_VALUE;
        Box b = h.box();
        double x = mc.player.getX();
        double z = mc.player.getZ();
        double cx = clamp(x, b.minX + 0.5, b.maxX - 0.5);
        double cz = clamp(z, b.minZ + 0.5, b.maxZ - 0.5);
        double dx = x - cx;
        double dz = z - cz;
        return dx * dx + dz * dz;
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : Math.min(v, max);
    }

    private void startFakeLagIfNeeded() {
        if (!fakeLag.getValue()) return;

        FakeLagFeature fl = Managers.FEATURE.getFeatureFromClass(FakeLagFeature.class);
        if (fl == null) return;

        if (!fl.isEnabled()) {
            fl.setEnabled(true);
            toggledFakeLag = true;
        }
    }

    private void stopFakeLagIfNeeded() {
        FakeLagFeature fl = Managers.FEATURE.getFeatureFromClass(FakeLagFeature.class);
        if (fl == null) return;

        if (toggledFakeLag && fl.isEnabled()) {
            fl.setToggled(false);
        }
        toggledFakeLag = false;
    }

    @SubscribeEvent
    public void onSettingChange(SettingChangeEvent event) {
        if (event.getSetting() == fakeLag) {
            stopFakeLagIfNeeded();
            if (isEnabled()) startFakeLagIfNeeded();
        }
    }

    private static final class AnchoredHole {
        final HoleUtils.Hole hole;
        final BlockPos anchor;
        AnchoredHole(HoleUtils.Hole hole, BlockPos anchor) { this.hole = hole; this.anchor = anchor; }
    }
}
