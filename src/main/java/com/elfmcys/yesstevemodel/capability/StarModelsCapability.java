package com.elfmcys.yesstevemodel.capability;

//? if neoforge
/*import com.elfmcys.yesstevemodel.platform.neoforge.capability.StarModelsCapabilityProvider;*/
//? if forge
import com.elfmcys.yesstevemodel.platform.forge.capability.StarModelsCapabilityProvider;
import com.google.common.collect.Sets;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class StarModelsCapability {

    private Set<String> starModels = Sets.newHashSet();

    public static Optional<StarModelsCapability> get(Player player) {
        //? if neoforge
        /*return java.util.Optional.ofNullable(player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP));*/
        //? if forge
        //? if <1.16.2
        /*return java.util.Optional.ofNullable(player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP).orElse(null));*/
        //? if >=1.16.2
        return player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP).resolve();
    }

    public void addModel(String str) {
        this.starModels.add(str);
    }

    public void copyFrom(StarModelsCapability other) {
        this.starModels = other.starModels;
    }

    public void removeModel(String str) {
        this.starModels.remove(str);
    }

    public boolean containsModel(String str) {
        return this.starModels.contains(str);
    }

    public Set<String> getStarModels() {
        return this.starModels;
    }

    public void setStarModels(Set<String> set) {
        this.starModels = set;
    }

    public void clear() {
        this.starModels.clear();
    }

    public ListTag serializeNBT() {
        ListTag listTag = new ListTag();
        Iterator<String> it = this.starModels.iterator();
        while (it.hasNext()) {
            listTag.add(StringTag.valueOf(it.next()));
        }
        return listTag;
    }

    public void deserializeNBT(ListTag listTag) {
        this.starModels.clear();
        Iterator it = listTag.iterator();
        while (it.hasNext()) {
            // 1.21.5 Tag.getAsString 删除（Tag.java:51 asString 返回 Optional<String>）
            //? if <21.5
            this.starModels.add(((Tag) it.next()).getAsString());
            //? if >=21.5
            /*this.starModels.add(((Tag) it.next()).asString().orElse(""));*/
        }
    }
}