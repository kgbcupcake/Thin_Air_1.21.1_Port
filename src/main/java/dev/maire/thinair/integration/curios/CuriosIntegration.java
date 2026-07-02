package dev.maire.thinair.integration.curios;

import dev.maire.thinair.init.ModRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class CuriosIntegration {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CuriosIntegration::onRegisterCapabilities);
        modEventBus.addListener(CuriosIntegration::onInterModEnqueue);
    }

    private static void onInterModEnqueue(InterModEnqueueEvent event) {
        InterModComms.sendTo(
                CuriosApi.MODID,
                SlotTypeMessage.REGISTER_TYPE,
                () -> SlotTypePreset.HEAD.getMessageBuilder().build()
        );
        InterModComms.sendTo(
                CuriosApi.MODID,
                SlotTypeMessage.REGISTER_TYPE,
                () -> SlotTypePreset.BELT.getMessageBuilder().build()
        );
        if (ModList.get().isLoaded("create")) {
            InterModComms.sendTo(
                    "curios",
                    SlotTypeMessage.REGISTER_TYPE,
                    () -> new SlotTypeMessage.Builder("back").size(1).build()
            );
        }
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        registerCurioItem(event, ModRegistry.RESPIRATOR_ITEM.get());
        registerCurioItem(event, ModRegistry.SAFETY_LANTERN_ITEM.get());

        if (ModList.get().isLoaded("create")) {
            try {
                registerCurioItem(event,
                        net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "copper_backtank")));
                registerCurioItem(event,
                        net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "netherite_backtank")));
            } catch (NoClassDefFoundError ignored) {
            }
        }
    }

    private static void registerCurioItem(RegisterCapabilitiesEvent event, Item item) {
        event.registerItem(
                CuriosCapability.ITEM,
                (ItemStack stack, Void ctx) -> new ICurio() {
                    @Override
                    public ItemStack getStack() {
                        return stack;
                    }

                    @Override
                    public boolean canEquipFromUse(SlotContext slotContext) {
                        return true;
                    }
                },
                item
        );
    }
}
