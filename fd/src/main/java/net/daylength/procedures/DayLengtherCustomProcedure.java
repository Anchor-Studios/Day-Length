package net.daylength.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;

import net.daylength.init.DayLengthModGameRules;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class DayLengtherCustomProcedure {
	@SubscribeEvent
	public static void onWorldTick(TickEvent.LevelTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.level);
		}
	}

	public static void execute(LevelAccessor world) {
		execute(null, world);
	}

	private static void execute(@Nullable Event event, LevelAccessor world) {
		if (!world.getLevelData().getGameRules().getBoolean(DayLengthModGameRules.REALTIMESYNC)) {
			if (0 == (world.getLevelData().getGameRules().getInt(DayLengthModGameRules.CUSTOMDAYLENGTH))) {
				if (world instanceof ServerLevel _level)
					_level.setDayTime((int) ((world.dayTime() + 20) - 1));
			} else {
				if (world instanceof ServerLevel _level)
					_level.setDayTime((int) ((world.dayTime() + 20 / (world.getLevelData().getGameRules().getInt(DayLengthModGameRules.CUSTOMDAYLENGTH))) - 1));
			}
		}
	}
}
