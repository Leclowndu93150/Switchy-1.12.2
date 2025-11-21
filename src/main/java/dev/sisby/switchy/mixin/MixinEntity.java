package dev.sisby.switchy.mixin;

import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {
    @Inject(method = "writeToNBT", at = @At("HEAD"), cancellable = true)
    public void applyHotSwapData(NBTTagCompound nbt, CallbackInfoReturnable<NBTTagCompound> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof SwitchyPlayer) {
            SwitchyPlayer sp = (SwitchyPlayer) self;
            NBTTagCompound hotSwap = sp.switchy$hotSwapData();
            if (hotSwap != null) {
                nbt.merge(hotSwap);
                sp.switchy$getPlayerData().writeNbt(nbt);
                cir.setReturnValue(nbt);
                cir.cancel();
            }
        }
    }
}
