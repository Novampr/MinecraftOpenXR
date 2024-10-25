package net.sorenon;

import net.minecraft.world.phys.Vec3;
import org.joml.*;

public class JOMLUtil {
    /**
     * Vector 3 start
     */
    public static Vec3 convert(Vector3dc vec) {
        return new Vec3(vec.x(), vec.y(), vec.z());
    }

    public static Vec3 convert(Vector3fc vec) {
        return new Vec3(vec.x(), vec.y(), vec.z());
    }

    public static Vector3d convert(Vec3 vec) {
        return new Vector3d(vec.x, vec.y, vec.z);
    }
}
