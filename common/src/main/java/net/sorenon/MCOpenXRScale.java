package net.sorenon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
//import virtuoel.pehkui.api.ScaleTypes;
//import virtuoel.pehkui.util.ScaleUtils;

public class MCOpenXRScale {
    public static float getScale(Entity entity) {
        return getScale(entity, 1.0f);
    }

    public static float getScale(Entity entity, float delta) {
        //if (Platform.isModLoaded("pehkui")) {
            //var scaleData = ScaleTypes.BASE.getScaleData(entity);
            //return scaleData.getScale(delta);
        /*} else */if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.getScale();
        }
        return 1;
    }

    public static float getMotionScale(Entity entity) {
        //if (Platform.isModLoaded("pehkui")) {
            //return ScaleUtils.getMotionScale(entity);
        //}
        return 1;
    }
}
