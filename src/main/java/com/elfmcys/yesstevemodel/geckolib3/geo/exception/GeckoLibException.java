package com.elfmcys.yesstevemodel.geckolib3.geo.exception;

//? if >=1.14
import net.minecraft.resources.ResourceLocation;
//? if <1.14
/*import net.minecraft.util.ResourceLocation;*/

public class GeckoLibException extends RuntimeException {
    private static final long serialVersionUID = 1;

    public GeckoLibException(ResourceLocation fileLocation, String message) {
        super(fileLocation + ": " + message);
    }

    public GeckoLibException(ResourceLocation fileLocation, String message, Throwable cause) {
        super(fileLocation + ": " + message, cause);
    }
}