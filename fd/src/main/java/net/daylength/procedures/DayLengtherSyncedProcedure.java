package net.daylength.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;

import net.daylength.init.DayLengthModGameRules;

import javax.annotation.Nullable;

import java.util.Calendar;

@Mod.EventBusSubscriber
public class DayLengtherSyncedProcedure {
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
		if (world.getLevelData().getGameRules().getBoolean(DayLengthModGameRules.REALTIMESYNC)) {
			if (world instanceof ServerLevel _level)
				_level.setDayTime((int) ((Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 3600 + Calendar.getInstance().get(Calendar.MINUTE) * 60 + Calendar.getInstance().get(Calendar.SECOND)) * (5 / 18)));
		}
	}
}
