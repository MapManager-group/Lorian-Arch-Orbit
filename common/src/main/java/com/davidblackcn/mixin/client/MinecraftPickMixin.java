package com.davidblackcn.mixin.client;

import com.davidblackcn.lorianarchorbit.client.ClientSmartPickRuntime;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftPickMixin {
    @Inject(method = "pickBlockOrEntity()V", at = @At("HEAD"))
    private void lorianArchOrbit$armSmartPickAlongsideVanillaPick(CallbackInfo callback) {
        ClientSmartPickRuntime.observeVanillaPick();
    }
}
