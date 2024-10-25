package net.sorenon.mixin.accessor;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraAcc {

    @Accessor("initialized")
    void ready(boolean ready);

    @Accessor("level")
    void area(BlockGetter area);

    @Accessor("entity")
    void focusedEntity(Entity entity);

    @Accessor("detached")
    void thirdPerson(boolean thirdPerson);

    @Accessor("xRot")
    void pitch(float pitch);

    @Accessor("yRot")
    void yaw(float yaw);

    @Accessor("left")
    Vector3f diagonalPlane();
}
