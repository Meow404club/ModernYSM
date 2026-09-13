package com.elfmcys.yesstevemodel.client.animation.condition;

import com.elfmcys.yesstevemodel.util.EquipmentUtil;
import com.elfmcys.yesstevemodel.util.YsmTag;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
//? if >=1.21 {
/*import rip.ysm.util.Rl;*/
//?}

public class ConditionUse {

    private static final String EMPTY = "";

    private final int preSize;

    private final String idPre;

    private final String tagPre;

    private final String extraPre;

    private final ObjectOpenHashSet<ResourceLocation> idTest = new ObjectOpenHashSet<>();

    private final ReferenceArrayList<YsmTag.ItemTag> tagTest = new ReferenceArrayList<>();

    private final ObjectOpenHashSet<UseAnim> extraTes = new ObjectOpenHashSet<>();

    private final ObjectOpenHashSet<String> innerTest = new ObjectOpenHashSet<>();

    public ConditionUse(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            this.idPre = "use_mainhand$";
            this.tagPre = "use_mainhand#";
            this.extraPre = "use_mainhand:";
            this.preSize = 13;
            return;
        }
        this.idPre = "use_offhand$";
        this.tagPre = "use_offhand#";
        this.extraPre = "use_offhand:";
        this.preSize = 12;
    }

    public void addTest(String name) {
        if (name.length() <= this.preSize) {
            return;
        }
        String strSubstring = name.substring(this.preSize);
        //? if >=1.21
        /*if (name.startsWith(this.idPre) && Rl.isValid(strSubstring)) {*/
        //? if <1.21
        if (name.startsWith(this.idPre) && ResourceLocation.isValidResourceLocation(strSubstring)) {
            //? if >=1.21
            /*this.idTest.add(ResourceLocation.parse(strSubstring));*/
            //? if <1.21
            this.idTest.add(new ResourceLocation(strSubstring));
        }
        //? if >=1.21
        /*if (name.startsWith(this.tagPre) && Rl.isValid(strSubstring)) {*/
        //? if <1.21
        if (name.startsWith(this.tagPre) && ResourceLocation.isValidResourceLocation(strSubstring)) {
            //? if >=1.21
            /*this.tagTest.add(YsmTag.itemTag(ResourceLocation.parse(strSubstring)));*/
            //? if <1.21
            this.tagTest.add(YsmTag.itemTag(new ResourceLocation(strSubstring)));
        }
        if (!name.startsWith(this.extraPre) || strSubstring.equals(UseAnim.NONE.name().toLowerCase(Locale.US))) {
            return;
        }
        Optional<UseAnim> optional = EquipmentUtil.getUseAnimByName(strSubstring);
        Objects.requireNonNull(this.extraTes);
        optional.ifPresent(extraTes::add);
        this.innerTest.add(name);
    }

    public String doTest(LivingEntity entity, InteractionHand hand) {
        if (entity.getItemInHand(hand).isEmpty()) {
            return EMPTY;
        }
        String result = doIdTest(entity, hand);
        if (result.isEmpty()) {
            result = doTagTest(entity, hand);
            if (result.isEmpty()) {
                return doExtraTest(entity, hand);
            }
            return result;
        }
        return result;
    }

    private String doIdTest(LivingEntity livingEntity, InteractionHand interactionHand) {
        if (this.idTest.isEmpty()) {
            return EMPTY;
        }
        ResourceLocation key = YsmTag.itemKey(livingEntity.getItemInHand(interactionHand).getItem());
        if (this.idTest.contains(key)) {
            return this.idPre + key;
        }
        return EMPTY;
    }

    private String doTagTest(LivingEntity livingEntity, InteractionHand interactionHand) {
        if (this.tagTest.isEmpty()) {
            return EMPTY;
        }
        ItemStack itemInHand = livingEntity.getItemInHand(interactionHand);
        Stream<YsmTag.ItemTag> stream = this.tagTest.stream();
        Objects.requireNonNull(itemInHand);
        return stream.filter(tag -> tag.matches(itemInHand)).findFirst().map(tag -> this.tagPre + tag.location()).orElse(EMPTY);
    }

    private String doExtraTest(LivingEntity entity, InteractionHand hand) {
        if (this.extraTes.isEmpty() && this.innerTest.isEmpty()) {
            return EMPTY;
        }
        String innerName = InnerClassify.doClassifyTest(this.extraPre, entity, hand);
        if (StringUtils.isNotBlank(innerName) && this.innerTest.contains(innerName)) {
            return innerName;
        }
        UseAnim anim = entity.getItemInHand(hand).getUseAnimation();
        if (this.extraTes.contains(anim)) {
            return this.extraPre + anim.name().toLowerCase(Locale.US);
        }
        return EMPTY;
    }
}
