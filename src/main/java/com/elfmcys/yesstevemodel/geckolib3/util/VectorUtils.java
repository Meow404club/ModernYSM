package com.elfmcys.yesstevemodel.geckolib3.util;

// 1.12.2=Vec3d（vanilla-mc-1122 Vec3d.java:6 ZERO/:7-9 public x,y,z/:11 (double,double,double)
// /:50 subtract(Vec3d) 实证）；joml Vector3f 两代同名
//? if >=1.13
import net.minecraft.world.phys.Vec3;
//? if <1.13
/*import net.minecraft.util.math.Vec3d;*/
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.joml.Vector3f;

public class VectorUtils {
    //? if >=1.13 {
    public static Vec3 fromArray(double[] array) {
        Validate.validIndex(ArrayUtils.toObject(array), 2);
        return new Vec3(array[0], array[1], array[2]);
    }
    //?}
    //? if <1.13 {
    /*public static Vec3d fromArray(double[] array) {
        Validate.validIndex(ArrayUtils.toObject(array), 2);
        return new Vec3d(array[0], array[1], array[2]);
    }*/
    //?}

    public static Vector3f fromArray(float[] array) {
        Validate.validIndex(ArrayUtils.toObject(array), 2);
        return new Vector3f(array[0], array[1], array[2]);
    }

    //? if >=1.13 {
    public static Vector3f convertDoubleToFloat(Vec3 vector) {
        return new Vector3f((float) vector.x, (float) vector.y, (float) vector.z);
    }

    public static Vec3 convertFloatToDouble(Vector3f vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
    //?}
    //? if <1.13 {
    /*public static Vector3f convertDoubleToFloat(Vec3d vector) {
        return new Vector3f((float) vector.x, (float) vector.y, (float) vector.z);
    }

    public static Vec3d convertFloatToDouble(Vector3f vector) {
        return new Vec3d(vector.x(), vector.y(), vector.z());
    }*/
    //?}
}
