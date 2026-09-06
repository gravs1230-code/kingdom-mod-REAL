package com.example.kingdommod;

import com.example.kingdommod.kingdom.KingdomSavedData;
import com.example.kingdommod.npc.VillagerRoleCapability;
import com.example.kingdommod.player.PlayerKingdomCapability;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KingdomMod.MODID)
public class KingdomMod {
    public static final String MODID = "kingdommod";

    public KingdomMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        VillagerRoleCapability.register(event);
        PlayerKingdomCapability.register(event);
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class EventHandlers {
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            KingdomCommands.register(event.getDispatcher());
        }
    }
}
