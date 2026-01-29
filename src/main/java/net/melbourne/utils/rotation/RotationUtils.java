package net.melbourne.utils.rotation;


import net.melbourne.utils.Globals;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class RotationUtils implements Globals {
    public static Vec2f getRotationAsVec2f(Vec3d posTo, Vec3d posFrom) {
        return getRotationFromVec(posTo.subtract(posFrom));
    }

    public static float[] getRotations(Entity entity) {
        return getRotations(
                entity.getX(),
                entity.getY() + entity.getEyeHeight(entity.getPose()) / 2.0,
                entity.getZ()
        );
    }

    public static float[] getRotations(Vec3d vec3d) {
        return getRotations(vec3d.x, vec3d.y, vec3d.z);
    }

    public static float[] getRotations(double x, double y, double z) {
        Vec3d eyePos = mc.player.getEyePos();

        double dx = x - eyePos.x;
        double dy = y - eyePos.y;
        double dz = z - eyePos.z;

        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        yaw = MathHelper.wrapDegrees(yaw);
        pitch = MathHelper.clamp(pitch, -90.0f, 90.0f);

        return new float[]{yaw, pitch};
    }

    public static double normalizeAngle(Double angleIn) {
        double angle = angleIn;
        if ((angle %= 360.0) >= 180.0) {
            angle -= 360.0;
        }
        if (angle < -180.0) {
            angle += 360.0;
        }
        return angle;
    }

    public static Vec2f getRotationFromVec(Vec3d vec) {
        double xz = Math.hypot(vec.x, vec.z);
        float yaw = (float) normalizeAngle(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
        float pitch = (float) normalizeAngle(Math.toDegrees(-Math.atan2(vec.y, xz)));
        return new Vec2f(yaw, pitch);
    }

    public static float[] getRotationsTo(Vec3d start, Vec3d end) {
        float yaw = (float) (Math.toDegrees(Math.atan2(end.subtract(start).z, end.subtract(start).x)) - 90);
        float pitch = (float) Math.toDegrees(-Math.atan2(end.subtract(start).y, Math.hypot(end.subtract(start).x, end.subtract(start).z)));
        return new float[]
                {
                        MathHelper.wrapDegrees(yaw),
                        MathHelper.wrapDegrees(pitch)
                };
    }
}