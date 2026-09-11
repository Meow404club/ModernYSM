package com.elfmcys.yesstevemodel.client.animation.condition;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import com.elfmcys.yesstevemodel.util.YsmTag;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class ConditionVehicle {

    private static final String EMPTY = "";

    private final ObjectOpenHashSet<ResourceLocation> idTest = new ObjectOpenHashSet<>();

    private final ReferenceArrayList<YsmTag.EntityTypeTag> tagTest = new ReferenceArrayList<>();

    private final String idPre;
    private final String tagPre;

    public ConditionVehicle() {
        this.idPre = "vehicle$";
        this.tagPre = "vehicle#";
    }

    public void addTest(String name) {
        int preSize = this.idPre.length();
        if (name.length() <= preSize) {
            return;
        }
        String strSubstring = name.substring(preSize);
        if (name.startsWith(this.idPre) && ResourceLocation.isValidResourceLocation(strSubstring)) {
            this.idTest.add(new ResourceLocation(strSubstring));
        }
        if (!name.startsWith(this.tagPre) || !ResourceLocation.isValidResourceLocation(strSubstring)) {
            return;
        }
        this.tagTest.add(YsmTag.entityTypeTag(new ResourceLocation(strSubstring)));
    }

    public String doTest(LivingEntity entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle == null || !vehicle.isAlive()) {
            return EMPTY;
        }
        String result = doIdTest(vehicle);
        if (result.isEmpty()) {
            return doTagTest(vehicle);
        }
        return result;
    }

    private String doIdTest(Entity entity) {
        ResourceLocation key;
        if (!this.idTest.isEmpty() && (key = YsmTag.entityTypeKey(entity.getType())) != null && this.idTest.contains(key)) {
            return this.idPre + key;
        }
        return EMPTY;
    }

    private String doTagTest(Entity entity) {
        if (this.tagTest.isEmpty()) {
            return EMPTY;
        }
        return this.tagTest.stream().filter(tag -> tag.matches(entity.getType())).findFirst().map(tag -> this.tagPre + tag.location()).orElse(EMPTY);
    }
}
