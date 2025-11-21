package dev.sisby.switchy.mixin;

import dev.sisby.switchy.Switchy;
import dev.sisby.switchy.data.SwitchyPlayerData;
import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerMP.class)
public class MixinEntityPlayerMP implements SwitchyPlayer {
    @Unique
    private SwitchyPlayerData switchy$playerData = null;
    @Unique
    private NBTTagCompound switchy$hotSwap = null;
    @Unique
    private NBTTagCompound switchy$reloadData = null;

    @Override
    public NBTTagCompound switchy$hotSwapData() {
        return switchy$hotSwap;
    }

    @Override
    public void switchy$startReload() {
        if (switchy$playerData != null) {
            switchy$reloadData = new NBTTagCompound();
            switchy$playerData.writeNbt(switchy$reloadData);
        }
    }

    @Override
    public void switchy$finishReload() {
        if (switchy$reloadData != null) {
            switchy$playerData = SwitchyPlayerData.fromNbt(switchy$reloadData);
            switchy$reloadData = null;
        }
    }

    @Override
    public void switchy$hotSwap(NBTTagCompound nbt, ITextComponent reason) {
        EntityPlayerMP self = (EntityPlayerMP) (Object) this;
        switchy$hotSwap = nbt;
        Switchy.LOGGER.info("[Switchy] Queuing hot swap for {} with inventory {} and pos {}", self.getName(), nbt.getTag("Inventory"), nbt.getTag("Pos"));
        self.connection.disconnect(reason);
    }

    @Override
    public SwitchyPlayerData switchy$getOrCreatePlayerData() {
        EntityPlayerMP self = (EntityPlayerMP) (Object) this;
        if (switchy$playerData == null) {
            NBTTagCompound nbt = new NBTTagCompound();
            self.writeToNBT(nbt);
            switchy$playerData = SwitchyPlayerData.create(self, nbt);
            switchy$playerData.validate(self, nbt);
        }
        return switchy$playerData;
    }

    @Override
    public SwitchyPlayerData switchy$getPlayerData() {
        return switchy$playerData;
    }

    @Override
    public void switchy$setPlayerData(SwitchyPlayerData data) {
        switchy$playerData = data;
    }

    @Override
    public void switchy$clearHotSwap() {
        switchy$hotSwap = null;
    }

    @Inject(method = "readEntityFromNBT", at = @At("RETURN"))
    public void readPlayerData(NBTTagCompound nbt, CallbackInfo ci) {
        EntityPlayerMP self = (EntityPlayerMP) (Object) this;
        if (nbt.hasKey(Switchy.ID, 10)) {
            switchy$playerData = SwitchyPlayerData.fromNbt(nbt);
            switchy$playerData.validate(self, nbt);
        } else if (nbt.hasKey("switchy:presets", 10)) {
            switchy$playerData = SwitchyPlayerData.create(self, nbt);
            switchy$playerData.validate(self, nbt);
        }
    }

    @Inject(method = "writeEntityToNBT", at = @At("RETURN"))
    public void writePlayerData(NBTTagCompound nbt, CallbackInfo ci) {
        if (switchy$playerData != null && switchy$reloadData == null) {
            switchy$playerData.writeNbt(nbt);
        }
    }

    @Inject(method = "copyFrom", at = @At("RETURN"))
    public void copyPlayerData(EntityPlayerMP oldPlayer, boolean keepEverything, CallbackInfo ci) {
        switchy$playerData = ((MixinEntityPlayerMP) (Object) oldPlayer).switchy$playerData;
    }
}
