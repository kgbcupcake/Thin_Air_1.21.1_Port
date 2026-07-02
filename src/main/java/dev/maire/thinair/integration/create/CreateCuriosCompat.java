package dev.maire.thinair.integration.create;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.List;
import java.util.Optional;

public class CreateCuriosCompat {

    public static void register() {
        BacktankUtil.addBacktankSupplier(CreateCuriosCompat::getBackSlotAirSources);
    }

    private static List<ItemStack> getBackSlotAirSources(LivingEntity entity) {
        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(entity);
        if (curios.isEmpty()) {
            return List.of();
        }
        return curios.get().findCurios("back").stream()
                .map(SlotResult::stack)
                .filter(AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES::matches)
                .toList();
    }
}
