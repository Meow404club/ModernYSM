package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import rip.ysm.compat.gun.swarfare.SWarfareCompat;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerArmorLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerElytraLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerItemInHandLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerParrotLayer;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
//? if >=1.17 {
import net.minecraft.client.renderer.entity.EntityRendererProvider;
//? }
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.PlayerRenderState;
 *///?}
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.NotNull;

public class CustomPlayerRenderer extends GeoReplacedEntityRenderer<Player, CustomPlayerEntity> {

    private ResourceLocation currentTexture;

    //? if <1.17 {
    // public CustomPlayerRenderer(net.minecraft.client.renderer.entity.EntityRenderDispatcher context) {
    //? } else {
    public CustomPlayerRenderer(EntityRendererProvider.Context context) {
    //? }
        super(context);
        // 1.16.5 dispatcher 无 getItemInHandRenderer，自建（仅用于 renderItem 委托）
        //? if <1.17
        // addLayerRenderer(new CustomPlayerItemInHandLayer(new net.minecraft.client.renderer.ItemInHandRenderer(net.minecraft.client.Minecraft.getInstance())));
        // EntityRendererProvider.Context.getItemInHandRenderer 1.19.2 起（1182 Context 无该字段）
        //? if >=1.17 && <1.19.2
        /*addLayerRenderer(new CustomPlayerItemInHandLayer(new net.minecraft.client.renderer.ItemInHandRenderer(Minecraft.getInstance())));*/
        // 1.21.2 EntityRendererProvider.Context 删 getItemInHandRenderer（vanilla-1.21.3
        // EntityRendererProvider.java Context 面实证）→ GameRenderer 公有字段 itemInHandRenderer
        //（vanilla-1.21.3 GameRenderer.java:84）
        //? if >=1.19.2 && <1.21.2
        addLayerRenderer(new CustomPlayerItemInHandLayer(context.getItemInHandRenderer()));
        //? if >=1.21.2
        /*addLayerRenderer(new CustomPlayerItemInHandLayer(Minecraft.getInstance().gameRenderer.itemInHandRenderer));*/
        addLayerRenderer(new CustomPlayerElytraLayer(context));
        addLayerRenderer(new CustomPlayerParrotLayer(context));
        addLayerRenderer(new CustomPlayerArmorLayer(context));
    }

    public void render(Player player, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        PlayerCapability capability;
        if (SWarfareCompat.isPlayerAiming(player) || (capability = PlayerCapability.get(player).orElse(null)) == null) {
            return;
        }
        capability.tickModel();
        SpecialPlayerRenderEvent renderEvent = new SpecialPlayerRenderEvent(player, capability, capability.getModelId());
        this.currentTexture = renderEvent.getTextureLocation();
        if (SpecialPlayerRenderEvent.post(renderEvent).isFalse()) {
            return;
        }
        renderEntityWithTexture(capability, renderEvent.getTextureLocation(), entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    // 1.21.2 shouldShowName 增相机距离尾参（vanilla-1.21.3 EntityRenderer.java:202）
    //? if <1.21.2
    @Override
    public boolean shouldShowName(Player entity) {
        Minecraft minecraft;
        LocalPlayer localPlayer;
        double dDistanceToSqr = this.entityRenderDispatcher.distanceToSqr(entity);
        float nameRenderDistance = entity.isDiscrete() ? 32.0f : 64.0f;
        if (dDistanceToSqr >= nameRenderDistance * nameRenderDistance || (localPlayer = (minecraft = Minecraft.getInstance()).player) == null) {
            return false;
        }
        boolean isVisible = !entity.isInvisibleTo(localPlayer);
        if (entity != localPlayer) {
            Team team = entity.getTeam();
            Team team2 = localPlayer.getTeam();
            if (team != null) {
                switch (team.getNameTagVisibility()) {
                    case ALWAYS:
                        return isVisible;
                    case NEVER:
                        return false;
                    case HIDE_FOR_OTHER_TEAMS:
                        return team2 == null ? isVisible : team.isAlliedTo(team2) && (team.canSeeFriendlyInvisibles() || isVisible);
                    case HIDE_FOR_OWN_TEAM:
                        return team2 == null ? isVisible : !team.isAlliedTo(team2) && isVisible;
                    default:
                        throw new IncompatibleClassChangeError();
                }
            }
        }
        return Minecraft.renderNames() && entity != minecraft.getCameraEntity() && isVisible && !entity.isVehicle();
    }
    //? if >=1.21.2 {
    /*@Override
    public boolean shouldShowName(Player entity, double distanceToCameraSq) {
        Minecraft minecraft;
        LocalPlayer localPlayer;
        float nameRenderDistance = entity.isDiscrete() ? 32.0f : 64.0f;
        if (distanceToCameraSq >= (double)(nameRenderDistance * nameRenderDistance) || (localPlayer = (minecraft = Minecraft.getInstance()).player) == null) {
            return false;
        }
        boolean isVisible = !entity.isInvisibleTo(localPlayer);
        if (entity != localPlayer) {
            Team team = entity.getTeam();
            Team team2 = localPlayer.getTeam();
            if (team != null) {
                switch (team.getNameTagVisibility()) {
                    case ALWAYS:
                        return isVisible;
                    case NEVER:
                        return false;
                    case HIDE_FOR_OTHER_TEAMS:
                        return team2 == null ? isVisible : team.isAlliedTo(team2) && (team.canSeeFriendlyInvisibles() || isVisible);
                    case HIDE_FOR_OWN_TEAM:
                        return team2 == null ? isVisible : !team.isAlliedTo(team2) && isVisible;
                    default:
                        throw new IncompatibleClassChangeError();
                }
            }
        }
        return Minecraft.renderNames() && entity != minecraft.getCameraEntity() && isVisible && !entity.isVehicle();
    }*/
    //?}

    @NotNull
    public ResourceLocation getTextureLocation(Player player) {
        return this.currentTexture == null ? PlayerCapability.get(player).map((cap) -> cap.getTextureLocation()).orElse(MissingTextureAtlasSprite.getLocation()) : this.currentTexture;
    }

    // 1.20.5+ EntityRenderer.renderNameTag 增 partialTick 尾参（vanilla-1.20.6 EntityRenderer.java:79）：
    // 本方法 <1.20.5 直接覆写（5 参）；>=1.20.5 改私有 6 参实现 + 下方 6 参覆写转发，记分板下挂名逻辑不变
    //? if <1.20.5
    public void renderNameTag(Player player, Component component, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
    //? if >=1.20.5 && <1.21.2
    /*private void renderNameTagInner(Player player, Component component, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, float partialTick) {*/
    //? if >=1.21.2
    /*private void renderNameTagInner(PlayerRenderState state, Component component, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
        Player player = this.ysmEntity;*/
        Scoreboard scoreboard;
        Objective displayObjective;
        if (PlayerPreviewEntity.isPreviewPlayer(player)) {
            return;
        }
        double dDistanceToSqr = this.entityRenderDispatcher.distanceToSqr(player);
        poseStack.pushPose();
        //? if neoforge
        /*if (dDistanceToSqr < 100.0d && (displayObjective = (scoreboard = player.getScoreboard()).getDisplayObjective(net.minecraft.world.scores.DisplaySlot.BELOW_NAME)) != null) {*/
        //? if forge
        if (dDistanceToSqr < 100.0d && (displayObjective = (scoreboard = player.getScoreboard()).getDisplayObjective(2)) != null) {
            // Component.literal（1.19+）→ 1.16.5 new TextComponent；append 双版同名
            //? if <1.19.2
            // super.renderNameTag(player, new net.minecraft.network.chat.TextComponent(Integer.toString(scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), displayObjective).getScore())).append(" ").append(displayObjective.getDisplayName()), poseStack, multiBufferSource, i);
            //? if neoforge && >=1.19.2 && <1.20.5
            /*super.renderNameTag(player, Component.literal(Integer.toString(scoreboard.getOrCreatePlayerScore(player, displayObjective).get())).append(" ").append(displayObjective.getDisplayName()), poseStack, multiBufferSource, i);*/
            //? if neoforge && >=1.20.5 && <1.21.2
            /*super.renderNameTag(player, Component.literal(Integer.toString(scoreboard.getOrCreatePlayerScore(player, displayObjective).get())).append(" ").append(displayObjective.getDisplayName()), poseStack, multiBufferSource, i, partialTick);*/
            //? if neoforge && >=1.21.2
            /*super.renderNameTag(state, Component.literal(Integer.toString(scoreboard.getOrCreatePlayerScore(player, displayObjective).get())).append(" ").append(displayObjective.getDisplayName()), poseStack, multiBufferSource, i);*/
            //? if forge && >=1.19.2
            super.renderNameTag(player, Component.literal(Integer.toString(scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), displayObjective).getScore())).append(" ").append(displayObjective.getDisplayName()), poseStack, multiBufferSource, i);
            poseStack.translate(0.0d, 0.25875d, 0.0d);
        }
        //? if <1.20.5
        super.renderNameTag(player, component, poseStack, multiBufferSource, i);
        //? if >=1.20.5 && <1.21.2
        /*super.renderNameTag(player, component, poseStack, multiBufferSource, i, partialTick);*/
        //? if >=1.21.2
        /*super.renderNameTag(state, component, poseStack, multiBufferSource, i);*/
        poseStack.popPose();
    }

    //? if >=1.20.5 && <1.21.2 {
    /*@Override
    protected void renderNameTag(Player player, Component component, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, float partialTick) {
        this.renderNameTagInner(player, component, poseStack, multiBufferSource, i, partialTick);
    }*/
    //? }
    // 1.21.2 render-state 化：renderNameTag(S, Component, ...) 五参、无 partialTick
    //（vanilla-1.21.3 EntityRenderer.java:210）；player 经 GeoReplacedEntityRenderer.ysmEntity 暂存
    //? if >=1.21.2 {
    /*@Override
    protected void renderNameTag(PlayerRenderState state, Component component, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
        this.renderNameTagInner(state, component, poseStack, multiBufferSource, i);
    }*/
    //? }

    // 1.21.2 vanilla setupRotations(S,PoseStack,F,F)（vanilla-1.21.3
    // LivingEntityRenderer.java:160）→ 原 5 参覆写仅 <1.21.2
    //? if <1.21.2 {
    @Override
    public void setupRotations(Player player, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(player, poseStack, ageInTicks, rotationYaw, partialTicks);
        Entity vehicle = player.getVehicle();
        if (TouhouLittleMaidCompat.isSimplePlanesEntity(vehicle) || TouhouLittleMaidCompat.isImmersiveAircraftEntity(vehicle)) {
            poseStack.translate(0.0d, 0.5d, 0.0d);
        }
    }
    //?}
    //? if >=1.21.2 {
    /*@Override
    protected void setupRotations(PlayerRenderState state, PoseStack poseStack, float ageInTicks, float rotationYaw) {
        super.setupRotations(state, poseStack, ageInTicks, rotationYaw);
        Entity vehicle = this.ysmEntity.getVehicle();
        if (TouhouLittleMaidCompat.isSimplePlanesEntity(vehicle) || TouhouLittleMaidCompat.isImmersiveAircraftEntity(vehicle)) {
            poseStack.translate(0.0d, 0.5d, 0.0d);
        }
    }*/
    //?}
}