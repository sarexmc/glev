package com.supernova.mixin;

import com.supernova.client.SceneFx;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Abre el campo de vision al acelerar (salto temporal) y da el "empujon" de FOV de la explosion. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void supernova$fov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        if (SceneFx.fovMul != 1f) cir.setReturnValue(cir.getReturnValueD() * SceneFx.fovMul);
    }
}
