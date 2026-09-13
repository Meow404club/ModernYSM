package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.upload.UploadManager;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.AnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.client.upload.IResourceLocatable;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.client.model.ProjectileModelBundle;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GeckoProjectileEntity extends GeoEntity<Projectile> {

    private ProjectileModelBundle projectileModelContext;

    public GeckoProjectileEntity(Projectile projectile) {
        super(projectile, true);
    }

    @Override
    public void registerAnimationControllers() {
        if (this.projectileModelContext != null) {
            this.projectileModelContext.getControllerInitializer().accept(this);
        }
    }

    @Override
    @Nullable
    public GeoEntity.ModelWrapper buildRenderShape(ModelAssembly modelAssembly, boolean isDefault) {
        ProjectileModelBundle modelBundle;
        // 1171 EntityType 无 builtInRegistryHolder（merged jar 实证）→ EntityType.getKey 静态（1165/1171 同款）；
        // 1.21.11 ResourceKey.location() → identifier()（2111 ResourceKey.java:57）
        // 1165/1171 分支用存储态块（条件真展开/假保持注释），勿用裸块：裸块对 <1.18.2 生成线
        // 会被包裹致 buildRenderShape 退化恒 null（双在产线终验实证，1165 抛射物模型全灭）
        //? if <1.18.2 {
        /*if (!isDefault && (modelBundle = modelAssembly.getProjectileModels().get(net.minecraft.world.entity.EntityType.getKey(this.entity.getType()))) != null) {
            return new ProjectileModelWrapper(modelAssembly, false, modelBundle);
        }*/
        //?}
        //? if >=21.11 {
        /*if (!isDefault && (modelBundle = modelAssembly.getProjectileModels().get(this.entity.getType().builtInRegistryHolder().key().identifier())) != null) {
            return new ProjectileModelWrapper(modelAssembly, false, modelBundle);
        }*/
        //?}
        //? if >=1.18.2 && <21.11 {
        if (!isDefault && (modelBundle = modelAssembly.getProjectileModels().get(this.entity.getType().builtInRegistryHolder().key().location())) != null) {
            return new ProjectileModelWrapper(modelAssembly, false, modelBundle);
        }
        //?}
        return null;
    }

    @Override
    public void onModelLoaded(ModelAssembly modelAssembly) {
        super.onModelLoaded(modelAssembly);
        //? if <1.18.2
        // this.projectileModelContext = modelAssembly.getProjectileModels().get(net.minecraft.world.entity.EntityType.getKey(this.entity.getType()));
        //? if >=1.18.2 && <21.11
        // this.projectileModelContext = modelAssembly.getProjectileModels().get(this.entity.getType().builtInRegistryHolder().key().location());
        //? if >=21.11
        // this.projectileModelContext = modelAssembly.getProjectileModels().get(this.entity.getType().builtInRegistryHolder().key().identifier());
    }

    @Override
    public void clearModel() {
        super.clearModel();
        this.projectileModelContext = null;
    }

    @Override
    public GeoModel getAnimationProcessor() {
        return this.projectileModelContext.getModel();
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation() {
        return ((ProjectileModelWrapper) getRenderShape()).textureLocatable.getResourceLocation().orElseGet(MissingTextureAtlasSprite::getLocation);
    }

    @Override
    public Animation getAnimation(String str) {
        return this.projectileModelContext.getAnimations().get(str);
    }

    @Override
    @Nullable
    public AnimationController getAnimationEntries(String str) {
        return this.projectileModelContext.getAnimationControllers().get(str);
    }

    @Override
    public boolean isModelReady() {
        return super.isModelReady() && this.projectileModelContext != null && getRenderShape().isValid();
    }

    @Override
    public float getHeightScale() {
        return 0.7f;
    }

    @Override
    public float getWidthScale() {
        return 0.7f;
    }

    private static class ProjectileModelWrapper extends ModelWrapper {

        private final IResourceLocatable textureLocatable;

        public ProjectileModelWrapper(ModelAssembly modelAssembly, boolean isDefault, ProjectileModelBundle modelBundle) {
            super(modelAssembly, isDefault);
            this.textureLocatable = UploadManager.getOrCreateLocatable(modelBundle.getTexture(), true);
        }

        @Override
        public boolean isValid() {
            return this.textureLocatable.getResourceLocation().isPresent();
        }
    }
}