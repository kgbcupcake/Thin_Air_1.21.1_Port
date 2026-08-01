package dev.maire.thinair.client;

import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.world.level.block.SafetyLanternBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class LanternDisplayResolver {

    private LanternDisplayResolver() {
    }

    public static AirQualityLevel resolveAirQualityLevel(ItemStack itemStack, @Nullable LivingEntity entity) {
        if (entity == null && itemStack.getEntityRepresentation() instanceof LivingEntity livingEntity) {
            entity = livingEntity;
        }

        if (entity != null) {
            AirQualityLevel cached = ClientPlayerAirQualityCache.get(entity.getId());
            if (cached != null) {
                return cached;
            }
        }

        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag compoundTag = customData.copyTag();
            if (compoundTag.contains(SafetyLanternBlock.TAG_AIR_QUALITY_LEVEL, Tag.TAG_INT)) {
                int airQualityLevel = compoundTag.getInt(SafetyLanternBlock.TAG_AIR_QUALITY_LEVEL);
                return AirQualityLevel.values()[airQualityLevel];
            }
        }

        return AirQualityLevel.YELLOW;
    }

    public static float resolveModelProperty(ItemStack itemStack, @Nullable LivingEntity entity) {
        return resolveAirQualityLevel(itemStack, entity).getItemModelProperty();
    }
}
