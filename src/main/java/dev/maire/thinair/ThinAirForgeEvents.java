package dev.maire.thinair;

import dev.maire.thinair.handler.AirBubbleTracker;
import dev.maire.thinair.handler.DrownedAttackHandler;
import dev.maire.thinair.handler.ReinforcedBladderCraftHandler;
import dev.maire.thinair.handler.TickAirHandler;
import dev.maire.thinair.init.ModRegistry;
import dev.maire.thinair.network.ClientboundPlayerAirQualityPacket;
import dev.maire.thinair.player.PlayerAirQuality;
import dev.maire.thinair.world.level.block.SignalTorchBlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;

@EventBusSubscriber(modid = ThinAir.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ThinAirForgeEvents {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel
                && event.getChunk() instanceof LevelChunk chunk) {
            AirBubbleTracker.onChunkLoad(serverLevel, chunk);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel
                && event.getChunk() instanceof LevelChunk chunk) {
            AirBubbleTracker.onChunkUnload(serverLevel, chunk);
        }
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        AirBubbleTracker.onChunkWatch(event.getPlayer(), event.getChunk(), event.getLevel());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            AirBubbleTracker.onLevelUnload(serverLevel.getServer(), serverLevel);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            AirBubbleTracker.onEndLevelTick(serverLevel.getServer(), serverLevel);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingBreathe(LivingBreatheEvent event) {
        TickAirHandler.onLivingBreathe(event);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerAirQuality.syncToClients(player, PlayerAirQuality.get(player));
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer tracked
                && event.getEntity() instanceof ServerPlayer tracker) {
            ThinAirMod.sendToPlayer(
                    tracker,
                    new ClientboundPlayerAirQualityPacket(tracked.getId(), PlayerAirQuality.get(tracked))
            );
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        DrownedAttackHandler.onLivingIncomingDamage(event);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ReinforcedBladderCraftHandler.onItemCrafted(event);
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        InteractionResult result = SignalTorchBlock.onUseBlock(
                event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec()
        );
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        injectLootPool(event, BuiltInLootTables.BURIED_TREASURE, ModRegistry.SOULFIRE_BOTTLE_BURIED_LOOT_TABLE);
        injectLootPool(event, BuiltInLootTables.SHIPWRECK_TREASURE, ModRegistry.SOULFIRE_BOTTLE_SHIPWRECK_LOOT_TABLE);
        injectLootPool(event, BuiltInLootTables.UNDERWATER_RUIN_BIG, ModRegistry.SOULFIRE_BOTTLE_BIG_RUIN_LOOT_TABLE);
        injectLootPool(event, BuiltInLootTables.UNDERWATER_RUIN_SMALL, ModRegistry.SOULFIRE_BOTTLE_SMALL_RUIN_LOOT_TABLE);
        injectLootPool(event, BuiltInLootTables.SIMPLE_DUNGEON, ModRegistry.SAFETY_LANTERN_DUNGEON_LOOT_TABLE);
        injectLootPool(event, BuiltInLootTables.ABANDONED_MINESHAFT, ModRegistry.SAFETY_LANTERN_MINESHAFT_LOOT_TABLE);
        injectLootPool(event, BuiltInLootTables.STRONGHOLD_CORRIDOR, ModRegistry.SAFETY_LANTERN_STRONGHOLD_LOOT_TABLE);
    }

    private static void injectLootPool(LootTableLoadEvent event, ResourceKey<LootTable> target,
                                       ResourceLocation injection) {
        if (event.getName().equals(target.location())) {
            event.getTable().addPool(
                    LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0F))
                            .add(NestedLootTable.lootTableReference(ResourceKey.create(Registries.LOOT_TABLE, injection)))
                            .build()
            );
        }
    }
}
