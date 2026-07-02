package dev.maire.thinair;

import dev.maire.thinair.client.config.ThinAirConfigScreen;
import dev.maire.thinair.config.ThinAirConfig;
import dev.maire.thinair.init.ModCapabilities;
import dev.maire.thinair.init.ModRegistry;
import dev.maire.thinair.integration.create.CreateCuriosCompat;
import dev.maire.thinair.integration.curios.CuriosIntegration;
import dev.maire.thinair.network.ThinAirNetwork;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.event.config.ModConfigEvent;

@Mod(ThinAir.MOD_ID)
public class ThinAirMod {

    public ThinAirMod(IEventBus modEventBus, ModContainer modContainer) {
        ModRegistry.register(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(
                    IConfigScreenFactory.class,
                    (container, screen) -> ThinAirConfigScreen.create(screen)
            );
        }
        ModCapabilities.register(modEventBus);
        modEventBus.addListener(ThinAirNetwork::register);
        modContainer.registerConfig(ModConfig.Type.SERVER, ThinAirConfig.SPEC);
        modEventBus.addListener(FMLConstructModEvent.class, event -> onConstruct(modEventBus));
        modEventBus.addListener(ModConfigEvent.Loading.class, ThinAirMod::onModConfigLoad);
        modEventBus.addListener(ModConfigEvent.Reloading.class, ThinAirMod::onModConfigReload);
    }

    private static void onModConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals(ThinAir.MOD_ID)) {
            ThinAirConfig.bind(event.getConfig());
        }
    }

    private static void onModConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(ThinAir.MOD_ID)) {
            ThinAirConfig.invalidateCache();
        }
    }

    private static void onConstruct(IEventBus modEventBus) {
        registerIntegrations(modEventBus);
    }

    private static void registerIntegrations(IEventBus modEventBus) {
        if (ModList.get().isLoaded("curios")) {
            CuriosIntegration.register(modEventBus);
        }
        if (ModList.get().isLoaded("create") && ModList.get().isLoaded("curios")) {
            CreateCuriosCompat.register();
        }
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToAllTracking(LevelChunk chunk, CustomPacketPayload packet) {
        if (chunk.getLevel() instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunk.getPos(), packet);
        }
    }
}
