package dev.maire.thinair.api;

import dev.maire.thinair.ThinAir;
import dev.maire.thinair.config.ThinAirConfig;
import dev.maire.thinair.init.ModRegistry;
import dev.maire.thinair.world.level.block.SafetyLanternBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Locale;
import java.util.Objects;

public enum AirQualityLevel implements StringRepresentable {
    /**
     * Full freedom to breathe.
     */
    GREEN(true, true) {
        @Override
        boolean isProtected(LivingEntity entity) {
            return false;
        }

        @Override
        int getAirAmount(LivingEntity entity) {
            return 4;
        }
    },
    /**
     * No loss, no gain.
     */
    BLUE(true, false) {
        @Override
        boolean isProtected(LivingEntity entity) {
            return false;
        }

        @Override
        int getAirAmount(LivingEntity entity) {
            return 0;
        }
    },
    /**
     * Slowly loose oxygen.
     */
    YELLOW(false, false, "breathing_equipment") {
        @Override
        int getAirAmount(LivingEntity entity) {
            return entity.level().getGameTime() % 4 == 0 ? super.getAirAmount(entity) : 0;
        }
    },
    /**
     * Completely unable to breathe (like underwater).
     */
    RED(false, false, "heavy_breathing_equipment");

    public static final StringRepresentable.EnumCodec<AirQualityLevel> CODEC = StringRepresentable.fromEnum(
            AirQualityLevel::values);

    public final boolean canBreathe;
    public final boolean canRefillAir;
    @Nullable
    private final TagKey<Item> breathingEquipment;
    private final TagKey<Block> airProviders;

    AirQualityLevel(boolean canBreathe, boolean canRefillAir) {
        this(canBreathe, canRefillAir, null);
    }

    AirQualityLevel(boolean canBreathe, boolean canRefillAir, @Nullable String breathingEquipment) {
        this.canBreathe = canBreathe;
        this.canRefillAir = canRefillAir;
        this.breathingEquipment = breathingEquipment != null ? TagKey.create(Registries.ITEM,
                ThinAir.id(breathingEquipment)
        ) : null;
        this.airProviders = TagKey.create(Registries.BLOCK, ThinAir.id(this.getSerializedName() + "_air_providers"));
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public TagKey<Item> getBreathingEquipment() {
        Objects.requireNonNull(this.breathingEquipment, "breathing equipment is null");
        return this.breathingEquipment;
    }

    public TagKey<Block> getAirProvidersTag() {
        return this.airProviders;
    }

    public double getAirProviderRadius() {
        return switch (this) {
            case RED -> ThinAirConfig.get().redAirProviderRadius();
            case GREEN -> ThinAirConfig.get().greenAirProviderRadius();
            case YELLOW -> ThinAirConfig.get().yellowAirProviderRadius();
            case BLUE -> ThinAirConfig.get().blueAirProviderRadius();
        };
    }

    public int getLightLevel() {
        return 15 - this.ordinal() * 3;
    }

    public int getOutputSignal() {
        return this.ordinal() + 1;
    }

    public boolean isBetterThan(AirQualityLevel other) {
        return this.ordinal() < other.ordinal();
    }

    public int getAirAmountAfterProtection(LivingEntity entity) {
        return this.isProtected(entity) ? 0 : this.getAirAmount(entity);
    }

    boolean isProtected(LivingEntity entity) {
        return MobEffectUtil.hasWaterBreathing(entity) || entity.isUsingItem() && entity.getUseItem()
                .is(ModRegistry.AIR_REFILLER_ITEM_TAG) || this.isProtectedViaBreathingEquipment(entity);
    }

    int getAirAmount(LivingEntity entity) {
        var enchantments = entity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int respiration = EnchantmentHelper.getEnchantmentLevel(
                enchantments.getOrThrow(Enchantments.RESPIRATION), entity);
        return entity.getRandom().nextInt(respiration + 1) == 0 ? -1 : 0;
    }

    private ItemStack findEquippedBreathingEquipment(LivingEntity entity) {
        if (this.breathingEquipment == null) {
            return ItemStack.EMPTY;
        }
        if (ModList.get().isLoaded("curios")) {
            var result = CuriosApi.getCuriosInventory(entity)
                    .map(cap -> cap.findFirstCurio(stack -> stack.is(this.breathingEquipment)))
                    .flatMap(java.util.function.Function.identity())
                    .map(SlotResult::stack)
                    .orElse(ItemStack.EMPTY);
            if (!result.isEmpty()) {
                return result;
            }
        }
        for (var slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.is(this.breathingEquipment)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean isProtectedViaBreathingEquipment(LivingEntity entity) {
        if (this.breathingEquipment != null) {
            ItemStack itemStack = this.findEquippedBreathingEquipment(entity);
            if (!itemStack.isEmpty() && entity.level().getGameTime() % (20 * 15) == 0) {
                itemStack.hurtAndBreak(1, entity, EquipmentSlot.HEAD);
            }
            if (!itemStack.isEmpty()) {
                return true;
            }
        }
        if (ModList.get().isLoaded("create")) {
            try {
                java.util.List<ItemStack> backtanks = com.simibubi.create.content.equipment.armor.BacktankUtil
                        .getAllWithAir(entity);
                if (!backtanks.isEmpty()) {
                    if (entity.level().getGameTime() % 20 == 0) {
                        com.simibubi.create.content.equipment.armor.BacktankUtil
                                .consumeAir(entity, backtanks.getFirst(), 1);
                    }
                    return true;
                }
            } catch (NoClassDefFoundError ignored) {
            }
        }
        return false;
    }

    public float getItemModelProperty() {
        return this.ordinal() / 10.0F;
    }

    @Nullable
    public static AirQualityLevel getAirQualityAtEyes(BlockState blockState) {
        if (blockState.is(Blocks.BUBBLE_COLUMN)) return GREEN;
        if (!blockState.getFluidState().isEmpty()) return RED;
        return getAirQualityFromBlock(blockState);
    }

    @Nullable
    public static AirQualityLevel getAirQualityFromBlock(BlockState blockState) {
        if (blockState.hasProperty(BlockStateProperties.LIT) && !blockState.getValue(BlockStateProperties.LIT)) {
            return null;
        }
        if (blockState.hasProperty(SafetyLanternBlock.AIR_QUALITY)) {
            return blockState.getValue(SafetyLanternBlock.AIR_QUALITY);
        }
        for (AirQualityLevel airQualityLevel : AirQualityLevel.values()) {
            if (blockState.is(airQualityLevel.airProviders)) {
                return airQualityLevel;
            }
        }
        return null;
    }
}
