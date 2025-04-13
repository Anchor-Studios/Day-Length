package net.daylength.procedures;

import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;

import net.daylength.init.DayLengthModGameRules;

import javax.annotation.Nullable;

import java.util.Calendar;

@EventBusSubscriber
public class DayLengtherSyncedProcedure {
	@SubscribeEvent
	public static void onWorldTick(LevelTickEvent.Post event) {
		execute(event, event.getLevel());
	}

	public static void execute(LevelAccessor world) {
		execute(null, world);
	}

	private static void execute(@Nullable Event event, LevelAccessor world) {
		if (world instanceof ServerLevel _serverLevelGR0 && _serverLevelGR0.getGameRules().getBoolean(DayLengthModGameRules.REALTIMESYNC)) {
			if (world instanceof ServerLevel _level)
				_level.setDayTime((int) ((Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 3600 + Calendar.getInstance().get(Calendar.MINUTE) * 60 + Calendar.getInstance().get(Calendar.SECOND)) * (5 / 18)));
		}
	}
}
