package com.elfmcys.yesstevemodel.capability;

//? if neoforge
/*import com.elfmcys.yesstevemodel.platform.neoforge.capability.AuthModelsCapabilityProvider;*/
//? if forge
import com.elfmcys.yesstevemodel.platform.forge.capability.AuthModelsCapabilityProvider;
import com.google.common.collect.Sets;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class AuthModelsCapability {

    private Set<String> authModels = Sets.newHashSet();

    public static Optional<AuthModelsCapability> get(Player player) {
        //? if neoforge
        /*return java.util.Optional.ofNullable(player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP));*/
        //? if forge && <1.16.2
        /*return java.util.Optional.ofNullable(player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).orElse(null));*/
        //? if forge && >=1.16.2
        return player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).resolve();
    }

    public void addModel(String str) {
        this.authModels.add(str);
    }

    public void copyFrom(AuthModelsCapability other) {
        this.authModels = other.authModels;
    }

    public void removeModel(String str) {
        this.authModels.remove(str);
    }

    public boolean containsModel(String str) {
        return this.authModels.contains(str);
    }

    public Set<String> getAuthModels() {
        return this.authModels;
    }

    public void setAuthModels(Set<String> set) {
        this.authModels = set;
    }

    public void clear() {
        this.authModels.clear();
    }

    public ListTag serializeNBT() {
        ListTag listTag = new ListTag();
        for (String authModel : this.authModels) {
            listTag.add(StringTag.valueOf(authModel));
        }
        return listTag;
    }

    public void deserializeNBT(ListTag listTag) {
        this.authModels.clear();
        for (Tag tag : listTag) {
            // 1.21.5 Tag.getAsString 删除（neoforge-21.5.98-sources Tag.java:51 asString 返回 Optional<String>）
            //? if <21.5
            this.authModels.add(tag.getAsString());
            //? if >=21.5
            /*this.authModels.add(tag.asString().orElse(""));*/
        }
    }
}