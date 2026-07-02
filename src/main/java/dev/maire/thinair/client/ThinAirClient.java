package dev.maire.thinair.client;

import dev.maire.thinair.ThinAir;
import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.client.renderer.entity.layers.RespiratorRenderer;
import dev.maire.thinair.init.ModRegistry;
import dev.maire.thinair.integration.curios.CuriosClientIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = ThinAir.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ThinAirClient {

    public static final ResourceLocation AIR_QUALITY_LEVEL_MODEL_PROPERTY = ThinAir.id("air_quality_level");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                    ModRegistry.SAFETY_LANTERN_ITEM.get(),
                    AIR_QUALITY_LEVEL_MODEL_PROPERTY,
                    (ItemStack itemStack, ClientLevel level, LivingEntity entity, int seed) ->
                            LanternDisplayResolver.resolveModelProperty(itemStack, entity)
            );
            ItemBlockRenderTypes.setRenderLayer(ModRegistry.SIGNAL_TORCH_BLOCK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModRegistry.WALL_SIGNAL_TORCH_BLOCK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModRegistry.SAFETY_LANTERN_BLOCK.get(), RenderType.cutout());
        });

        if (ModList.get().isLoaded("curios")) {
            CuriosClientIntegration.registerCuriosRenderer();
        }

        // NeoForge.EVENT_BUS.addListener(ThinAirClient::onRenderGuiPost);
    }

    private static AirQualityLevel lastAirQualityLevel = null;
    private static long lastAirQualityChangeMs = 0L;

    private static void onRenderGuiPost(RenderGuiEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        AirQualityLevel level = ClientPlayerAirQualityCache.get(player.getId());
        if (level != AirQualityLevel.YELLOW && level != AirQualityLevel.RED) {
            return;
        }

        if (level != lastAirQualityLevel) {
            lastAirQualityLevel = level;
            lastAirQualityChangeMs = System.currentTimeMillis();
        }

        float alpha = Mth.clamp((System.currentTimeMillis() - lastAirQualityChangeMs) / 1000.0F, 0.0F, 1.0F);
        int levelColor = getLevelColor(level);
        int fadedLevelColor = withAlpha(levelColor, alpha);

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        var font = Minecraft.getInstance().font;

        boolean wearingGoggles = false;
        if (ModList.get().isLoaded("create")) {
            try {
                net.minecraft.resources.ResourceLocation gogglesId =
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "goggles");
                for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot
                        .values()) {
                    if (net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .getKey(player.getItemBySlot(slot).getItem()).equals(gogglesId)) {
                        wearingGoggles = true;
                        break;
                    }
                }
            } catch (NoClassDefFoundError ignored) {
            }
        }

        int panelWidth = 90;
        int padding = 6;
        int spacing = 4;

        int gogglesRowHeight = wearingGoggles ? font.lineHeight - 2 : 0;
        int quality0RowHeight = font.lineHeight;
        int barRowHeight = 4;

        int contentHeight = quality0RowHeight + spacing + barRowHeight;
        if (wearingGoggles) {
            contentHeight += gogglesRowHeight + spacing;
        }
        int panelHeight = padding * 2 + contentHeight;

        int hotbarY = screenHeight - 22;
        int panelBottom = hotbarY - 8;
        int panelRight = screenWidth - 8;
        int panelLeft = panelRight - panelWidth;
        int panelTop = panelBottom - panelHeight;

        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, withAlpha(0xCC0D0D0D, alpha));

        int contentX = panelLeft + padding;
        int contentWidth = panelWidth - padding * 2;
        int y = panelTop + padding;

        if (wearingGoggles) {
            guiGraphics.drawString(font, level.getSerializedName(), contentX, y, fadedLevelColor, false);
            y += gogglesRowHeight + spacing;
        }

        int circleSize = 6;
        int circleY = y + (quality0RowHeight - circleSize) / 2;
        drawCircleApprox(guiGraphics, contentX, circleY, circleSize, fadedLevelColor);

        String label = level == AirQualityLevel.RED ? "Red Air" : "Yellow Air";
        int textX = contentX + circleSize + 3;
        int textY = y + (quality0RowHeight - font.lineHeight) / 2;
        guiGraphics.drawString(font, label, textX, textY, fadedLevelColor, false);

        y += quality0RowHeight + spacing;

        int barX = contentX;
        int barY = y;
        guiGraphics.fill(barX, barY, barX + contentWidth, barY + barRowHeight, withAlpha(0xFF222222, alpha));

        int maxAirSupply = player.getMaxAirSupply();
        if (maxAirSupply > 0) {
            float fraction = Mth.clamp(player.getAirSupply() / (float) maxAirSupply, 0.0F, 1.0F);
            int filledWidth = Math.round(contentWidth * fraction);
            if (filledWidth > 0) {
                guiGraphics.fill(barX, barY, barX + filledWidth, barY + barRowHeight, fadedLevelColor);
            }
        }
    }

    private static void drawCircleApprox(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        int inset = 1;
        guiGraphics.fill(x + inset, y + inset, x + size - inset, y + size - inset, color);
        guiGraphics.fill(x + 2, y, x + size - 2, y + 1, color);
        guiGraphics.fill(x + 2, y + size - 1, x + size - 2, y + size, color);
        guiGraphics.fill(x, y + 2, x + 1, y + size - 2, color);
        guiGraphics.fill(x + size - 1, y + 2, x + size, y + size - 2, color);
    }

    private static int withAlpha(int argbColor, float alpha) {
        int scaledAlpha = Math.round(((argbColor >>> 24) & 0xFF) * alpha);
        return (scaledAlpha << 24) | (argbColor & 0x00FFFFFF);
    }

    private static int getLevelColor(AirQualityLevel level) {
        return switch (level) {
            case GREEN -> 0xFF55FF55;
            case YELLOW -> 0xFFFFD700;
            case RED -> 0xFFFF4444;
            case BLUE -> 0xFF5555FF;
        };
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                RespiratorRenderer.PLAYER_RESPIRATOR_LAYER,
                () -> LayerDefinition.create(
                        HumanoidModel.createMesh(new CubeDeformation(1.02F), 0.0F),
                        64,
                        32
                )
        );
    }

    @SubscribeEvent
    public static void onReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
                RespiratorRenderer.bakeModel(Minecraft.getInstance().getEntityModels());
            }
        });
    }
}
