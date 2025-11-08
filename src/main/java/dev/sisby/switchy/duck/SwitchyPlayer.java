package dev.sisby.switchy.duck;

import dev.sisby.switchy.data.SwitchyPlayerData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;

public interface SwitchyPlayer {
    NBTTagCompound switchy$hotSwapData();

    void switchy$startReload();

    void switchy$finishReload();

    void switchy$hotSwap(NBTTagCompound nbt, ITextComponent reason);

    SwitchyPlayerData switchy$getOrCreatePlayerData();

    SwitchyPlayerData switchy$getPlayerData();
}
