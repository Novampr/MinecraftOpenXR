package net.sorenon;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Pose {
    public static final StreamCodec<ByteBuf, Pose> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull Pose decode(ByteBuf buf) {
            Pose pose = new Pose();
            pose.pos.set(buf.readFloat(), buf.readFloat(), buf.readFloat());
            return pose;
        }

        @Override
        public void encode(ByteBuf buf, Pose pose) {
            buf.writeFloat(pose.pos.x);
            buf.writeFloat(pose.pos.y);
            buf.writeFloat(pose.pos.z);
        }
    };

    public final Quaternionf orientation = new Quaternionf();
    public final Vector3f pos = new Vector3f();

    public void set(Pose pose) {
        pos.set(pose.pos);
        orientation.set(pose.orientation);
    }

    public Vector3f getPos() {
        return pos;
    }

    public Quaternionf getOrientation() {
        return orientation;
    }

    public float getMCYaw() {
        return getMCYaw(orientation);
    }

    public float getMCPitch() {
        return getMCPitch(orientation);
    }

    public void write(ByteBuf buf) {
        buf.writeFloat(pos.x).writeFloat(pos.y).writeFloat(pos.z);
        buf.writeFloat(orientation.x).writeFloat(orientation.y).writeFloat(orientation.z).writeFloat(orientation.w);
    }

    public void read(ByteBuf buf) {
        this.pos.set(buf.readFloat(), buf.readFloat(), buf.readFloat());
        this.orientation.set(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static float getMCYaw(Quaternionf orientation) {
        return getMCYaw(orientation, new Vector3f(0, 0, -1));
    }

    public static float getMCYaw(Quaternionf orientation, Vector3f normal) {
        orientation.transform(normal);
        float yaw = getYawFromNormal(normal);
        return (float) -Math.toDegrees(yaw) + 180;
    }

    public static float getMCPitch(Quaternionf orientation) {
        return getMCPitch(orientation, new Vector3f(0, 0, -1));
    }

    public static float getMCPitch(Quaternionf orientation, Vector3f normal) {
        orientation.transform(normal);
        float pitch = (float) Math.asin(Mth.clamp(normal.y, -0.999999999, 0.999999999));
        return (float) -Math.toDegrees(pitch);
    }

    public static float getYawFromNormal(Vector3f normal) {
        if (normal.z < 0) {
            return (float) java.lang.Math.atan(normal.x / normal.z);
        }
        if (normal.z == 0) {
            return (float) (Math.PI / 2 * -Mth.sign(normal.x));
        }
        if (normal.z > 0) {
            return (float) (java.lang.Math.atan(normal.x / normal.z) + Math.PI);
        }
        return 0;
    }
}
