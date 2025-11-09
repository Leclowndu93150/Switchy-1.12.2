package dev.sisby.switchy;

import dev.sisby.switchy.data.SwitchyComponentTypes;
import dev.sisby.switchy.data.SwitchyPlayerData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = Switchy.ID,
    name = Switchy.NAME,
    version = Switchy.VERSION,
    acceptableRemoteVersions = "*"
)
public class Switchy {
    public static final String ID = "switchy";
    public static final String NAME = "Switchy";
    public static final String VERSION = "2.0.0";
    
    public static final Logger LOGGER = LogManager.getLogger(NAME);
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Initializing Switchy...");
        SwitchyComponentTypes.init();
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new SwitchyCommands());
        LOGGER.info("Registered Switchy commands");
    }
    
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            SwitchyPlayerData data = SwitchyPlayerData.ofEarly(player);
            if (data != null && data.greeting().isPresent()) {
                player.sendMessage(data.greet());
            }
        }
    }
}
