package dev.sisby.switchy.mixin;

import dev.sisby.switchy.data.SwitchyPlayerData;
import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@Mixin(Entity.class)
public class MixinEntity {
    @Inject(method = "writeToNBT", at = @At("HEAD"), cancellable = true)
    public void applyHotSwapData(NBTTagCompound compound, CallbackInfoReturnable<NBTTagCompound> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof SwitchyPlayer) {
            SwitchyPlayer sp = (SwitchyPlayer) self;
            NBTTagCompound hotSwap = sp.switchy$hotSwapData();
            if (hotSwap != null) {
                Set<String> existingKeys = new HashSet<>(compound.getKeySet());
                for (String key : existingKeys) {
                    compound.removeTag(key);
                }

                for (String key : hotSwap.getKeySet()) {
                    NBTBase value = hotSwap.getTag(key);
                    if (value != null) {
                        compound.setTag(key, value.copy());
                    }
                }

                SwitchyPlayerData playerData = sp.switchy$getPlayerData();
                if (playerData != null) {
                    playerData.writeNbt(compound);
                }

                cir.setReturnValue(compound);
                cir.cancel();
            }
        }
    }
}
