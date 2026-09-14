package com.elfmcys.yesstevemodel.client.animation.molang;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import com.elfmcys.yesstevemodel.util.accessors.ProjectileStateAccessor;
import com.elfmcys.yesstevemodel.client.animation.molang.functions.ysm.*;
import rip.ysm.compat.cosmeticarmorreworked.CosmeticArmorHelper;
import rip.ysm.compat.curios.CuriosCompat;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ContextBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.util.YsmEntity;
import com.elfmcys.yesstevemodel.util.YsmTag;
import com.elfmcys.yesstevemodel.util.YsmText;
import com.elfmcys.yesstevemodel.geckolib3.util.MathInterpolation;
import com.elfmcys.yesstevemodel.mixin.client.ArrowEntityAccessor;
import com.elfmcys.yesstevemodel.mixin.client.FishingHookAccessor;
import com.elfmcys.yesstevemodel.mixin.client.ThrowableItemProjectileAccessor;
import com.elfmcys.yesstevemodel.geckolib3.core.EntityFrameStateTracker;
import com.elfmcys.yesstevemodel.util.CameraUtil;
import com.elfmcys.yesstevemodel.util.data.LazySupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
// 1.21.11 Parrot 移 animal.parrot 子包（>=21.11 版已在上方抛射物块补导）
//? if <21.11
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
// 1.21.11 抛射物类移子包（neoforge-21.11.45 sources：arrow/Arrow、arrow/SpectralArrow、
// throwableitemprojectile/ThrowableItemProjectile 实证；21.10 同包未动）
//? if >=21.11 {
/*import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
 *///?}
//? if <21.11 {
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
//?}
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
//? if >=1.18.2 {
import net.minecraft.core.Holder;
//?}
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.elfmcys.yesstevemodel.platform.YsmPlatform;
import rip.ysm.api.attribute.ForgeAttributes;

import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;

public class YSMBinding extends ContextBinding {

    public static final LazySupplier<YSMBinding> INSTANCE = new LazySupplier<>(YSMBinding::new);

    private YSMBinding() {
        function("dump_equipped_item", new DumpEquippedItem());
        function("dump_relative_block", new DumpRelativeBlock());
        var("dump_mods", YSMBinding::dumpMods);
        entityVar("dump_effects", YSMBinding::dumpEffects);
        entityVar("dump_biome", YSMBinding::dumpBiome);
        function("mod_version", new ModVersion());
        function("equipped_enchantment_level", new EquippedEnchantmentLevel());
        function("effect_level", new EffectLevel());

        function("relative_block_name", new RelativeBlockName());
        function("relative_block_name_any", new RelativeBlockNameAny());

        function("bone_rot", new BoneRotation());
        function("bone_pos", new BonePosition());
        function("bone_scale", new BoneScale());
        function("bone_pivot_abs", new BonePivotAbs());

        var("head_yaw", ctx -> ctx.data().netHeadYaw);
        var("head_pitch", ctx -> ctx.data().headPitch);

        var("weather", ctx -> getWeather(ctx.level()));
        // 1.21.11 ResourceKey.location() → identifier()（2111 ResourceKey.java:57；TagKey.location
        // 保留为 record 组件不变）
        //? if >=21.11
        /*var("dimension_name", ctx -> ctx.level().dimension().identifier().toString());*/
        //? if <21.11
        var("dimension_name", ctx -> ctx.level().dimension().location().toString());
        // getFps() 1.18+（1.16.5 为 fpsString 字段/无取值器）：帧率不显示数字时 1.16.5 退化 parse fpsString
        // getFps 1.19.4+（1192 merged jar 无此方法，仅 fpsString 字段）：中段+1.16.5 降级 0
        //? if <1.19.4
        /*var("fps", ctx -> 0);*/
        //? if >=1.19.4
        var("fps", ctx -> Minecraft.getInstance().getFps());
        var("time_delta", ctx -> ctx.geoInstance().getPositionTracker().getTimeDelta() / 20.0f);
        entityVar("ground_speed2", YSMBinding::getGroundSpeed2);

        entityVar("input_vertical", MathInterpolation::getYawInterpolation);
        entityVar("input_horizontal", MathInterpolation::getPitchInterpolation);

        entityVar("person_view", CameraUtil::getCameraType);
        entityVar("rendering_in_paperdoll", ctx -> ModelPreviewRenderer.isExtraPlayer());
        entityVar("rendering_in_inventory", CameraUtil::isThirdPerson);
        entityVar("block_light", ctx -> ctx.level().getBrightness(LightLayer.BLOCK, ctx.entity().blockPosition()));
        entityVar("sky_light", ctx -> ctx.level().getBrightness(LightLayer.SKY, ctx.entity().blockPosition()));
        entityVar("is_passenger", ctx -> ctx.entity().isPassenger());
        entityVar("is_sleep", ctx -> ctx.entity().getPose() == Pose.SLEEPING);
        entityVar("is_sneak", ctx -> YsmEntity.onGround(ctx.entity()) && ctx.entity().getPose() == Pose.CROUCHING);
        entityVar("biome_category", ctx -> getBiomeCategory(ctx.entity()));
        entityVar("is_open_air", ctx -> isOpenAir(ctx.entity()));
        entityVar("eye_in_water", ctx -> ctx.entity().isUnderWater());
        // getTicksFrozen 1.17+（Powder Snow）；1.16.5 无冰冻机制 → 恒 0（语义不匹配态，行为差记回报）
        //? if <1.17
        /*entityVar("frozen_ticks", ctx -> 0);*/
        //? if >=1.17
        entityVar("frozen_ticks", ctx -> ctx.entity().getTicksFrozen());
        entityVar("air_supply", ctx -> ctx.entity().getAirSupply());
        entityVar("delta_movement_length", ctx -> ctx.entity().getDeltaMovement().length());
        livingEntityVar("has_helmet", ctx -> hasEquipment(ctx.entity(), EquipmentSlot.HEAD));
        livingEntityVar("has_chest_plate", ctx -> hasEquipment(ctx.entity(), EquipmentSlot.CHEST));
        livingEntityVar("has_leggings", ctx -> hasEquipment(ctx.entity(), EquipmentSlot.LEGS));
        livingEntityVar("has_boots", ctx -> hasEquipment(ctx.entity(), EquipmentSlot.FEET));
        livingEntityVar("has_mainhand", ctx -> hasEquipment(ctx.entity(), EquipmentSlot.MAINHAND));
        livingEntityVar("has_offhand", ctx -> hasEquipment(ctx.entity(), EquipmentSlot.OFFHAND));
        livingEntityVar("has_elytra", ctx -> !CosmeticArmorHelper.getElytraItem(ctx.entity()).isEmpty());
        livingEntityVar("is_riptide", ctx -> ctx.entity().isAutoSpinAttack());
        livingEntityVar("armor_value", ctx -> ctx.entity().getArmorValue());
        livingEntityVar("hurt_time", ctx -> ctx.entity().hurtTime);
        livingEntityVar("is_close_eyes", ctx -> isCloseEyes(ctx.animationEvent(), ctx.entity()));
        livingEntityVar("on_ladder", ctx -> ctx.entity().onClimbable());
        livingEntityVar("ladder_facing", new LadderFacing());
        livingEntityVar("arrow_count", ctx -> ctx.entity().getArrowCount());
        livingEntityVar("stinger_count", ctx -> ctx.entity().getStingerCount());
        livingEntityVar("entity_type", YSMBinding::getEntityTypeName);
        livingEntityVar("is_player", ctx -> "player".equals(getEntityTypeName(ctx)));
        livingEntityVar("is_maid", ctx -> "maid".equals(getEntityTypeName(ctx)));
        livingEntityVar("food_level", YSMBinding::getFoodLevel);

        livingEntityVar("xxa", YSMBinding::getXxa);
        livingEntityVar("yya", YSMBinding::getYya);
        livingEntityVar("zza", YSMBinding::getZza);

        livingEntityVar("mainhand_charged_crossbow", ctx -> isChargedCrossbow(ctx, InteractionHand.MAIN_HAND));
        livingEntityVar("offhand_charged_crossbow", ctx -> isChargedCrossbow(ctx, InteractionHand.OFF_HAND));

        livingEntityVar("is_fishing", YSMBinding::isFishing);
        livingEntityVar("swinging", ctx -> ctx.entity().swinging);
        livingEntityVar("swing_time", ctx -> ctx.entity().swingTime);
        livingEntityVar("swinging_arm", ctx -> ctx.entity().swingingArm == InteractionHand.MAIN_HAND ? 0 : 1);
        livingEntityVar("attack_time", ctx -> ctx.entity().getAttackAnim(ctx.animationEvent().getFrameTime()));
        playerEntityVar("texture_name", new TextureName());
        playerEntityVar("first_person_mod_hide", new FirstPersonModHide());

        playerEntityVar("has_left_shoulder_parrot", ctx -> hasShoulderParrot(ctx.entity(), true));
        playerEntityVar("has_right_shoulder_parrot", ctx -> hasShoulderParrot(ctx.entity(), false));

        playerEntityVar("left_shoulder_parrot_variant", ctx -> getShoulderParrotVariant(ctx.entity(), true));
        playerEntityVar("right_shoulder_parrot_variant", ctx -> getShoulderParrotVariant(ctx.entity(), false));

        playerEntityVar("attack_damage", ctx -> ctx.entity().getAttributeValue(Attributes.ATTACK_DAMAGE));
        playerEntityVar("attack_speed", ctx -> ctx.entity().getAttributeValue(Attributes.ATTACK_SPEED));
        playerEntityVar("attack_knockback", ctx -> ctx.entity().getAttributeValue(Attributes.ATTACK_KNOCKBACK));

        playerEntityVar("movement_speed", ctx -> ctx.entity().getAttributeValue(Attributes.MOVEMENT_SPEED));
        playerEntityVar("knockback_resistance", ctx -> ctx.entity().getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        playerEntityVar("luck", ctx -> ctx.entity().getAttributeValue(Attributes.LUCK));
        playerEntityVar("block_reach", ctx -> ForgeAttributes.getValue(ctx.entity(), ForgeAttributes.blockReach(), 4.5D));
        playerEntityVar("entity_reach", ctx -> ForgeAttributes.getValue(ctx.entity(), ForgeAttributes.entityReach(), 3.0D));
        playerEntityVar("swim_speed", ctx -> ForgeAttributes.getValue(ctx.entity(), ForgeAttributes.swimSpeed(), 1.0D));
        playerEntityVar("entity_gravity", ctx -> ForgeAttributes.getValue(ctx.entity(), ForgeAttributes.entityGravity(), 0.08D));
        playerEntityVar("step_height_addition", ctx -> ForgeAttributes.getValue(ctx.entity(), ForgeAttributes.stepHeightAddition(), 0.0D));
        playerEntityVar("nametag_distance", ctx -> ForgeAttributes.getValue(ctx.entity(), ForgeAttributes.nametagDistance(), 64.0D));
        playerEntityVar("in_shield_block_cooldown", YSMBinding::isInShieldBlockCooldown);

        // 1.21.9 elytraRotX/Y/Z 移 HumanoidRenderState（render state 抽取面），实体无访问器
        // → 21.9+ molang 恒 0，功能债入账
        //? if >=21.9 {
        /*clientPlayerEntityVar("elytra_rot_x", ctx -> 0.0F);
         *///?}
        //? if <21.9
        clientPlayerEntityVar("elytra_rot_x", ctx -> Math.toDegrees(ctx.entity().elytraRotX));
        //? if >=21.9 {
        /*clientPlayerEntityVar("elytra_rot_y", ctx -> 0.0F);
         *///?}
        //? if <21.9
        clientPlayerEntityVar("elytra_rot_y", ctx -> Math.toDegrees(ctx.entity().elytraRotY));
        //? if >=21.9 {
        /*clientPlayerEntityVar("elytra_rot_z", ctx -> 0.0F);
         *///?}
        //? if <21.9
        clientPlayerEntityVar("elytra_rot_z", ctx -> Math.toDegrees(ctx.entity().elytraRotZ));

        localPlayerEntityVar("hit_target_id", YSMBinding::getHitTargetId);
        localPlayerEntityVar("hit_target_type", YSMBinding::getHitTargetType);

        function("first_order", new FirstOrderFunction());
        function("second_order", new SecondOrderFunction())
        ;
        function("particle", new Particle(false));
        function("abs_particle", new Particle(true));

        function("perlin_noise", new PerlinNoise());

        function("play_sound", new SoundFunction.PlaySoundFunction());
        function("stop_sound", new SoundFunction.StopSoundFunction());
        function("stop_all_sounds", new SoundFunction.StopAllSoundsFunction());

        function("keyboard", new InputKeyDetectionFunction.Keyboard());
        function("mouse", new InputKeyDetectionFunction.Mouse());
        function(MolangEventDispatcher.SYNC, new Sync());
        function(MolangEventDispatcher.DEFER, new Defer());
        projectileEntityVar("projectile_owner", ctx -> ctx.createChild(ctx.entity().getOwner()));
        throwableProjectileEntityVar("throwable_item", YSMBinding::getThrowableItemId);
        fishHookEntityVar("hooked_in", YSMBinding::getHookedEntityType);
        fishHookEntityVar("is_biting", ctx -> ((FishingHookAccessor) ctx.entity()).isBiting());
        abstractArrowEntityVar("on_ground_time", ctx -> ((ProjectileStateAccessor) ctx.entity()).getInGroundTime());
        // >=21.3 ProjectileStateAccessor.isInGround 被 Mixin 丢弃（与目标 protected 同签名，
        // AbstractArrowEntityMixin 头注）→ inGroundTime>0 近似（滞后一 tick，功能债）
        //? if <21.3
        abstractArrowEntityVar("in_ground", ctx -> ((ProjectileStateAccessor) ctx.entity()).isInGround());
        //? if >=21.3
        /*abstractArrowEntityVar("in_ground", ctx -> ((ProjectileStateAccessor) ctx.entity()).getInGroundTime() > 0);*/
        abstractArrowEntityVar("is_spectral_arrow", ctx -> ctx.entity() instanceof SpectralArrow);
        abstractArrowEntityVar("shoot_item_id", ctx -> ((ProjectileStateAccessor) ctx.entity()).getOwnerItemId());
        CuriosCompat.registerCuriosItems(this);
    }

    private static String getHitTargetId(IContext<LocalPlayer> context) {
        ClientLevel clientLevel;
        HitResult hitResult = Minecraft.getInstance().hitResult;
        if (hitResult instanceof BlockHitResult) {
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            if (blockHitResult.getType() == HitResult.Type.MISS || (clientLevel = Minecraft.getInstance().level) == null) {
                return StringPool.EMPTY;
            }
            ResourceLocation key = YsmTag.blockKey(clientLevel.getBlockState(blockHitResult.getBlockPos()).getBlock());
            if (key != null) {
                return key.toString();
            }
            return StringPool.EMPTY;
        }
        if (hitResult instanceof EntityHitResult) {
            ResourceLocation key2 = YsmTag.entityTypeKey(((EntityHitResult) hitResult).getEntity().getType());
            if (key2 != null) {
                return key2.toString();
            }
            return StringPool.EMPTY;
        }
        return StringPool.EMPTY;
    }

    private static String getHitTargetType(IContext<LocalPlayer> context) {
        HitResult hitResult = Minecraft.getInstance().hitResult;
        if (hitResult == null) {
            return StringPool.EMPTY;
        }
        switch (hitResult.getType()) {
            case BLOCK:
                break;
            case ENTITY:
                break;
        }
        return StringPool.EMPTY;
    }

    private static String getHookedEntityType(IContext<FishingHook> context) {
        ResourceLocation key;
        Entity entity = ((FishingHookAccessor) context.entity()).getHookedIn();
        if (entity != null && (key = YsmTag.entityTypeKey(entity.getType())) != null) {
            return key.toString();
        }
        return StringPool.EMPTY;
    }

    private static String getThrowableItemId(IContext<ThrowableItemProjectile> context) {
        ThrowableItemProjectile throwableItemProjectile = context.entity();
        if (throwableItemProjectile instanceof ThrowableItemProjectileAccessor) {
            ResourceLocation key = YsmTag.itemKey(((ThrowableItemProjectileAccessor) throwableItemProjectile).invokeGetDefaultItem());
            if (key != null) {
                return key.toString();
            }
            return StringPool.EMPTY;
        }
        return StringPool.EMPTY;
    }

    private static float getGroundSpeed2(IContext<Entity> context) {
        EntityFrameStateTracker<?> c0269x82e473c1Mo1215x3cfc56ba = context.geoInstance().getPositionTracker();
        Vec3 vec3M1419xc2097f01 = c0269x82e473c1Mo1215x3cfc56ba.getPositionDelta();
        return (20.0f * Mth.sqrt((float) ((vec3M1419xc2097f01.x * vec3M1419xc2097f01.x) + (vec3M1419xc2097f01.z * vec3M1419xc2097f01.z)))) / c0269x82e473c1Mo1215x3cfc56ba.getTimeDelta();
    }

    private static float getXxa(IContext<LivingEntity> context) {
        AnimatableEntity<?> abstractC0235x5da32a01Mo322x83eb685f = context.geoInstance();
        if (abstractC0235x5da32a01Mo322x83eb685f instanceof PlayerCapability) {
            PlayerCapability playerCapability = (PlayerCapability) abstractC0235x5da32a01Mo322x83eb685f;
            if (!playerCapability.isLocalPlayerModel()) {
                return playerCapability.getPositionTracker().getStrafeInput();
            }
        }
        return context.entity().xxa;
    }

    private static float getYya(IContext<LivingEntity> context) {
        AnimatableEntity<?> abstractC0235x5da32a01Mo322x83eb685f = context.geoInstance();
        if (abstractC0235x5da32a01Mo322x83eb685f instanceof PlayerCapability) {
            PlayerCapability playerCapability = (PlayerCapability) abstractC0235x5da32a01Mo322x83eb685f;
            if (!playerCapability.isLocalPlayerModel()) {
                return playerCapability.getPositionTracker().getVerticalInput();
            }
        }
        return context.entity().yya;
    }

    private static float getZza(IContext<LivingEntity> context) {
        AnimatableEntity<?> abstractC0235x5da32a01Mo322x83eb685f = context.geoInstance();
        if (abstractC0235x5da32a01Mo322x83eb685f instanceof PlayerCapability) {
            PlayerCapability playerCapability = (PlayerCapability) abstractC0235x5da32a01Mo322x83eb685f;
            if (!playerCapability.isLocalPlayerModel()) {
                return playerCapability.getPositionTracker().getForwardInput();
            }
        }
        return context.entity().zza;
    }

    private static boolean isInShieldBlockCooldown(IContext<Player> context) {
        AnimatableEntity<?> abstractC0235x5da32a01Mo322x83eb685f = context.geoInstance();
        if (abstractC0235x5da32a01Mo322x83eb685f instanceof PlayerCapability) {
            return ((PlayerCapability) abstractC0235x5da32a01Mo322x83eb685f).getPositionTracker().isShieldBlocking();
        }
        return false;
    }

    /** ComponentUtils.copyOnClickText 1.19.4+（1192 sources 零命中）：<1.19.4 原串直返
     *（丢点击复制修饰，仅 debug dump 输出，行为差见回报）。 */
    //? if <1.19.4 {
    /*private static net.minecraft.network.chat.Component copyOnClickTextCompat(String str) {
        return YsmText.literal(str);
    }
     *///?}
    //? if >=1.19.4 {
    private static net.minecraft.network.chat.Component copyOnClickTextCompat(String str) {
        return net.minecraft.network.chat.ComponentUtils.copyOnClickText(str);
    }
    //?}

    private static boolean isFishing(IContext<LivingEntity> context) {
        LivingEntity livingEntity = context.entity();
        if (livingEntity instanceof Player) {
            return ((Player) livingEntity).fishing != null;
        }
        return TouhouLittleMaidCompat.isMaidSitting(livingEntity);
    }

    private static boolean isChargedCrossbow(IContext<LivingEntity> context, InteractionHand interactionHand) {
        ItemStack itemInHand = context.entity().getItemInHand(interactionHand);
                //? if <1.17
        /*return itemInHand.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(itemInHand);*/
        //? if >=1.17
        return itemInHand.is(Items.CROSSBOW) && CrossbowItem.isCharged(itemInHand);
    }

    private static String getEntityTypeName(IContext<LivingEntity> context) {
        LivingEntity livingEntityMo327xaffeef43 = context.entity();
        if (livingEntityMo327xaffeef43 instanceof Player) {
            return "player";
        }
        ResourceLocation key = YsmTag.entityTypeKey(livingEntityMo327xaffeef43.getType());
        if (key == null) {
            return StringPool.EMPTY;
        }
        if ("touhou_little_maid".equals(key.getNamespace()) && "maid".equals(key.getPath())) {
            return "maid";
        }
        return key.toString();
    }

    private static Object getFoodLevel(IContext<LivingEntity> context) {
        AnimatableEntity<?> abstractC0235x5da32a01Mo322x83eb685f = context.geoInstance();
        if (abstractC0235x5da32a01Mo322x83eb685f instanceof PlayerCapability) {
            PlayerCapability playerCapability = (PlayerCapability) abstractC0235x5da32a01Mo322x83eb685f;
            if (!playerCapability.isLocalPlayerModel()) {
                return Integer.valueOf(playerCapability.getPositionTracker().getFoodLevel());
            }
        }
        LivingEntity livingEntity = context.entity();
        if (livingEntity instanceof Player) {
            return Integer.valueOf(((Player) livingEntity).getFoodData().getFoodLevel());
        }
        return 20;
    }

    private static boolean isCloseEyes(AnimationEvent<?> event, LivingEntity livingEntity) {
        float blinkPhase = (event.getCurrentTick() + (Math.abs(livingEntity.getUUID().getLeastSignificantBits()) % 10)) % 90.0f;
        return livingEntity.isSleeping() || ((85.0f > blinkPhase ? 1 : (85.0f == blinkPhase ? 0 : -1)) < 0 && (blinkPhase > 90.0f ? 1 : (blinkPhase == 90.0f ? 0 : -1)) < 0);
    }

    private static boolean hasEquipment(LivingEntity livingEntity, EquipmentSlot equipmentSlot) {
        return !CosmeticArmorHelper.getArmorItem(livingEntity, equipmentSlot).isEmpty();
    }

    private static int getWeather(ClientLevel clientLevel) {
        if (clientLevel.isThundering()) {
            return 2;
        }
        if (clientLevel.isRaining()) {
            return 1;
        }
        return 0;
    }

    @Deprecated
    private String getBiomeCategory(Entity entity) {
        return null;
    }

    private static Object dumpMods(IContext<?> context) {
        if (!context.isDebugMode()) {
            return null;
        }
        YsmPlatform.getMods().stream().sorted(Comparator.comparing(mod -> mod.getName())).forEach(mod -> {
            context.logWarningComponent(YsmText.literal("Mod: display ").append(copyOnClickTextCompat(mod.getName())).append(YsmText.literal("  id ").append(copyOnClickTextCompat(mod.getModId()))));
        });
        return null;
    }

    private static Object dumpEffects(IContext<Entity> context) {
        Collection<MobEffectInstance> activeEffects;
        if (!context.isDebugMode()) {
            return null;
        }
        if (context.entity() instanceof Arrow) {
            // 1.20.5+ Arrow.effects 字段删除 → PotionContents.getAllEffects()（Invoker 孪生）
            //? if neoforge && >=1.20.5
            /*java.util.ArrayList<MobEffectInstance> arrowEffects = new java.util.ArrayList<>();
            ((com.elfmcys.yesstevemodel.mixin.client.ArrowPotionAccessor) context.entity()).ysm$getPotionContents().getAllEffects().forEach(arrowEffects::add);
            activeEffects = arrowEffects;*/
            //? if forge || neoforge && <1.20.5
            activeEffects = ((ArrowEntityAccessor) context.entity()).getEffects();
        } else if (context.entity() instanceof LivingEntity) {
            activeEffects = ((LivingEntity) context.entity()).getActiveEffects();
        } else {
            return null;
        }
        for (MobEffectInstance mobEffectInstance : activeEffects) {
            // 1.20.5+ getEffect 返回 Holder → .value() 还原 MobEffect（显示名/键名门面不变）
            //? if neoforge && >=1.20.5
            /*context.logWarningComponent(YsmText.literal("Effect: display ").append(copyOnClickTextCompat(mobEffectInstance.getEffect().value().getDisplayName().getString(99))).append(YsmText.literal("  name ").append(copyOnClickTextCompat(YsmTag.mobEffectKey(mobEffectInstance.getEffect().value()).toString()))).append("  lv=").append(String.valueOf(mobEffectInstance.getAmplifier() + 1)));*/
            //? if forge || neoforge && <1.20.5
            context.logWarningComponent(YsmText.literal("Effect: display ").append(copyOnClickTextCompat(mobEffectInstance.getEffect().getDisplayName().getString(99))).append(YsmText.literal("  name ").append(copyOnClickTextCompat(YsmTag.mobEffectKey(mobEffectInstance.getEffect()).toString()))).append("  lv=").append(String.valueOf(mobEffectInstance.getAmplifier() + 1)));
        }
        return null;
    }

    private static Object dumpBiome(IContext<Entity> context) {
        if (!context.isDebugMode()) {
            return null;
        }
        // 1.20.1 getBiome 返回 Holder<Biome>（unwrapKey/tags）↔ 1.16.5 getBiome 返回 Biome 本体；
        // 1.16.5 轴：dump name 走 level.getBiomeName(pos)（Optional<ResourceLocation>），tag 信息 1.16.5
        // biome 无标签 API（ITag 体系不在 Biome 上）→ 省略该行（debug dump 输出项，行为差见回报）
        //? if <1.16.2 {
        /*// net.minecraft.data.BuiltinRegistries 1.16.2 才有（1.16.1 走 Registry.BIOME.getKey，官方 1161 实证）
        ResourceLocation biomeName = net.minecraft.core.Registry.BIOME.getKey(context.entity().level.getBiome(context.entity().blockPosition()));*/
        //?}
        //? if >=1.16.2 && <1.18 {
        /*ResourceLocation biomeName = net.minecraft.data.BuiltinRegistries.BIOME.getKey(context.entity().level.getBiome(context.entity().blockPosition()));
        if (biomeName != null) {
            context.logWarningComponent(YsmText.literal("Name ").append(copyOnClickTextCompat(biomeName.toString())));
        }
         *///?}
        //? if >=1.18 && <1.18.2 {
        /*
        // 1.18.0 getBiome 仍返回 Biome 本体（Holder 化 1.18.2 起，1182 实证为 >=1.18.2 边界）；
        // Registry 无类型化 BIOME 字段（1182 Registry.java 仅 BIOME_REGISTRY ResourceKey）→
        // 经 level.registryAccess().registryOrThrow 取 Registry<Biome> 再 getKey
        ResourceLocation biomeName = context.entity().getLevel().registryAccess().registryOrThrow(net.minecraft.core.Registry.BIOME_REGISTRY).getKey(context.entity().getLevel().getBiome(context.entity().blockPosition()));
        if (biomeName != null) {
            context.logWarningComponent(YsmText.literal("Name ").append(copyOnClickTextCompat(biomeName.toString())));
        }
         */
        //?}
        // 1.18.2~1.19.2：Holder 体系已在（1182 Level.getBiome 返回 Holder），实体访问器为 getLevel()
        //? if >=1.18.2 && <1.19.4 {
        /*
        Holder<Biome> biome = context.entity().getLevel().getBiome(context.entity().blockPosition());
        biome.unwrapKey().ifPresent(resourceKey -> {
            context.logWarningComponent(YsmText.literal("Name ").append(copyOnClickTextCompat(ysmKeyString(resourceKey))));
        });
        biome.tags().forEach(tagKey -> {
            context.logWarningComponent(YsmText.literal("Tag ").append(copyOnClickTextCompat(tagKey.location().toString())));
        });
         *///?}
        //? if >=1.19.4 && <1.20 {
        /*
        Holder<Biome> biome = context.entity().getLevel().getBiome(context.entity().blockPosition());
        biome.unwrapKey().ifPresent(resourceKey -> {
            context.logWarningComponent(YsmText.literal("Name ").append(copyOnClickTextCompat(ysmKeyString(resourceKey))));
        });
        biome.tags().forEach(tagKey -> {
            context.logWarningComponent(YsmText.literal("Tag ").append(copyOnClickTextCompat(tagKey.location().toString())));
        });
         *///?}
        //? if >=1.20 {
        Holder<Biome> biome = context.entity().level().getBiome(context.entity().blockPosition());
        biome.unwrapKey().ifPresent(resourceKey -> {
            context.logWarningComponent(YsmText.literal("Name ").append(copyOnClickTextCompat(ysmKeyString(resourceKey))));
        });
        biome.tags().forEach(tagKey -> {
            context.logWarningComponent(YsmText.literal("Tag ").append(copyOnClickTextCompat(tagKey.location().toString())));
        });
        //?}
        return null;
    }

    private static boolean isOpenAir(Entity entity) {
        BlockPos blockPosBlockPosition = entity.blockPosition();
                //? if <1.18.2
        /*return entity.level.canSeeSky(blockPosBlockPosition) && entity.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPosBlockPosition).getY() <= blockPosBlockPosition.getY();*/
        //? if >=1.18.2 && <1.19.4
        /*return entity.getLevel().canSeeSky(blockPosBlockPosition) && entity.getLevel().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPosBlockPosition).getY() <= blockPosBlockPosition.getY();*/
        //? if >=1.19.4 && <1.20
        /*return entity.getLevel().canSeeSky(blockPosBlockPosition) && entity.getLevel().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPosBlockPosition).getY() <= blockPosBlockPosition.getY();*/
        //? if >=1.20
        return entity.level().canSeeSky(blockPosBlockPosition) && entity.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPosBlockPosition).getY() <= blockPosBlockPosition.getY();
    }

    //? if <1.19.4
    /*private static final String[] PARROT_VARIANT_NAMES = {"red_blue", "blue", "green", "yellow_blue", "silver"};

    private static String getParrotVariantName(int variant) {
        return variant >= 0 && variant < PARROT_VARIANT_NAMES.length ? PARROT_VARIANT_NAMES[variant] : PARROT_VARIANT_NAMES[0];
    }*/
    public static String getShoulderParrotVariant(Player player, boolean leftShoulder) {
        // 1.21.9 肩膀鹦鹉数据迁移：Player.getShoulderEntityLeft/Right()（CompoundTag）删除 →
        // ClientAvatarEntity#getParrotVariantOnShoulder(boolean left)（AbstractClientPlayer 覆写，
        // neoforge-21.10.64 AbstractClientPlayer.java:93 实证，left=true 语义同旧参）；
        // 调用方为客户端玩家渲染绑定，强转 AbstractClientPlayer 安全
        //? if >=21.9 {
        /*Parrot.Variant shoulderVariant = ((net.minecraft.client.player.AbstractClientPlayer) player).getParrotVariantOnShoulder(leftShoulder);
        return shoulderVariant == null ? "empty" : shoulderVariant.name().toLowerCase(Locale.ENGLISH);
        *///?}
        //? if <21.9 {
        CompoundTag shoulderEntityLeft = leftShoulder ? player.getShoulderEntityLeft() : player.getShoulderEntityRight();
        // 1.21.5 CompoundTag getString/getInt 返回 Optional（CompoundTag.java:357/325）
        //? if <21.5
        return EntityType.byString(shoulderEntityLeft.getString("id")).filter(entityType -> {
        //? if >=21.5
        /*return EntityType.byString(shoulderEntityLeft.getStringOr("id", "")).filter(entityType -> {*/
            return entityType == EntityType.PARROT;
        }).map(entityType2 -> {
            // Parrot.Variant 内枚举 1.18+（1.16.5 javap 无）：变体名序 vanilla 同源，1.16.5 走名字表
        // Parrot.Variant 1.19.4+（1192 merged jar 无 Variant 内类）：中段+1.16.5 走名字表
        //? if <1.19.4
        /*return getParrotVariantName(shoulderEntityLeft.getInt("Variant"));*/
        //? if >=1.19.4 && <21.5
        return Parrot.Variant.byId(shoulderEntityLeft.getInt("Variant")).name().toLowerCase(Locale.ENGLISH);
        //? if >=21.5
        /*return Parrot.Variant.byId(shoulderEntityLeft.getIntOr("Variant", 0)).name().toLowerCase(Locale.ENGLISH);*/
        }).orElse("empty");
        //?}
    }

    // 1.21.11 ResourceKey.location() → identifier()（2111 ResourceKey.java:57）；TagKey/自有
    // ItemTag 的 location() 为 record 组件/自有方法，保留不变。表达式位不可内嵌条件 → 收编 helper
    private static String ysmKeyString(net.minecraft.resources.ResourceKey<?> key) {
        //? if >=21.11
        /*return key.identifier().toString();*/
        //? if <21.11
        return key.location().toString();
    }

    private static boolean hasShoulderParrot(Player player, boolean leftShoulder) {
        //? if >=21.9 {
        /*return ((net.minecraft.client.player.AbstractClientPlayer) player).getParrotVariantOnShoulder(leftShoulder) != null;
        *///?}
        //? if <21.9 {
        return !(leftShoulder ? player.getShoulderEntityLeft() : player.getShoulderEntityRight()).isEmpty();
        //?}
    }
}
