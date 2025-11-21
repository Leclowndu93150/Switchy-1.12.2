package dev.sisby.switchy.mixin;

import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.storage.SaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SaveHandler.class)
public abstract class MixinSaveHandler {
    @Inject(method = "writePlayerData", at = @At("RETURN"))
    private void switchy$clearHotSwapData(EntityPlayer player, CallbackInfo ci) {
        if (player instanceof SwitchyPlayer) {
            SwitchyPlayer sp = (SwitchyPlayer) player;
            if (sp.switchy$hotSwapData() != null) {
                Switchy.LOGGER.info("[Switchy] Clearing hot swap data after save for {}", player.getName());
                sp.switchy$clearHotSwap();
            }
        }
    }
}
