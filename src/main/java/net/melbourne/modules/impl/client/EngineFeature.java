package net.melbourne.modules.impl.client;

import net.melbourne.Managers;
import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PlayerUpdateEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.combat.AutoMineFeature;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.settings.types.WhitelistSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.HashSet;
import java.util.Set;

@FeatureInfo(name = "Engine", category = Category.Client)
public class EngineFeature extends Feature {

    public WhitelistSetting socials = new WhitelistSetting("Socials", "Social checks to respect.", WhitelistSetting.Type.CUSTOM, new String[]{"Friends"}, new String[]{"Friends", "Teams"});
    public WhitelistSetting predictions = new WhitelistSetting("Predictions", "Types of predictions to calculate.", WhitelistSetting.Type.CUSTOM, new String[]{"Mining"}, new String[]{"Mining", "Movement"});
    public ModeSetting breakingMode = new ModeSetting("Breaking", "Calculation logic for mining simulation.", "Generic", new String[]{"Generic", "Compute"}, () -> predictions.getWhitelistIds().contains("Mining"));
    public NumberSetting magnitude = new NumberSetting("Magnitude", "How much to scale the predicted movement vector.", 100, 50, 300, () -> predictions.getWhitelistIds().contains("Movement"));
    public ModeSetting incompleteMode = new ModeSetting("Incompletes", "Mode for incomplete hole detection.", "Above", new String[]{"Above", "Normal"});

    private final Set<BlockPos> predictedPositions = new HashSet<>();

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull()) return;

        predictedPositions.clear();

        if (predictions.getWhitelistIds().contains("Mining")) {
            AutoMineFeature autoMine = Managers.FEATURE.getFeatureFromClass(AutoMineFeature.class);
            if (autoMine != null) {
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player == mc.player || !player.isAlive() || Managers.FRIEND.isFriend(player.getName().getString())) continue;

                    double rangeSq = MathHelper.square(autoMine.range.getValue().floatValue());
                    if (mc.player.squaredDistanceTo(player) > rangeSq) continue;

                    BlockPos predicted = Services.SIMULATION.predictMiningTarget(player);
                    if (predicted != null) {
                        predictedPositions.add(predicted);
                    }
                }
            }
        }
    }

    public Vec3d getPredictedPos(PlayerEntity player) {
        if (!predictions.getWhitelistIds().contains("Movement")) return player.getPos();
        float magMultiplier = magnitude.getValue().floatValue() / 100f;
        return Services.SIMULATION.getMovementPrediction(player, magMultiplier);
    }

    public boolean isPredicted(BlockPos pos) {
        return predictions.getWhitelistIds().contains("Mining") && predictedPositions.contains(pos);
    }

    public Set<BlockPos> getPredictedPositions() {
        return predictedPositions;
    }
}