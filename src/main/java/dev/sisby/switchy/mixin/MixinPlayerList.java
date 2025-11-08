package dev.sisby.switchy.mixin;

import com.mojang.authlib.GameProfile;
import dev.sisby.switchy.duck.SwitchyPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.PlayerProfileCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PlayerList.class)
public class MixinPlayerList {
    
    @Inject(
        method = "initializeConnectionToPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayerMP;setWorld(Lnet/minecraft/world/World;)V",
            ordinal = 0,
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void applyHotSwapData(NetworkManager netManager, EntityPlayerMP player, NetHandlerPlayServer nethandlerplayserver, CallbackInfo ci, 
                                   GameProfile gameprofile, PlayerProfileCache playerprofilecache, GameProfile gameprofile1, String s, NBTTagCompound nbttagcompound) {
        SwitchyPlayer switchyPlayer = (SwitchyPlayer) player;
        NBTTagCompound hotSwapData = switchyPlayer.switchy$hotSwapData();
        
        if (hotSwapData != null) {
            player.readFromNBT(hotSwapData);
        }
    }
}
