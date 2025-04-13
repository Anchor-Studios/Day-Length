package net.daylength.procedures;

import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;

import net.daylength.network.DayLengthModVariables;
import net.daylength.init.DayLengthModGameRules;

import javax.annotation.Nullable;

@EventBusSubscriber
public class DayLengtherCustomProcedure {
	@SubscribeEvent
	public static void onWorldTick(LevelTickEvent.Post event) {
		execute(event, event.getLevel());
	}

	public static void execute(LevelAccessor world) {
		execute(null, world);
	}

	private static void execute(@Nullable Event event, LevelAccessor world) {
		if (!(world instanceof ServerLevel _serverLevelGR0 && _serverLevelGR0.getGameRules().getBoolean(DayLengthModGameRules.REALTIMESYNC))) {
			if (0 == (world instanceof ServerLevel _serverLevelGR1 ? _serverLevelGR1.getGameRules().getInt(DayLengthModGameRules.CUSTOMDAYLENGTH) : 0)) {
				if (world instanceof ServerLevel _level)
					_level.setDayTime((int) (world.dayTime() - 1));
			} else if (20 <= (world instanceof ServerLevel _serverLevelGR4 ? _serverLevelGR4.getGameRules().getInt(DayLengthModGameRules.CUSTOMDAYLENGTH) : 0)) {
				if ((world instanceof ServerLevel _serverLevelGR5 ? _serverLevelGR5.getGameRules().getInt(DayLengthModGameRules.CUSTOMDAYLENGTH) : 0) / 20 <= DayLengthModVariables.MapVariables.get(world).counter) {
					DayLengthModVariables.MapVariables.get(world).counter = 1;
					DayLengthModVariables.MapVariables.get(world).syncData(world);
				} else {
					if (world instanceof ServerLevel _level)
						_level.setDayTime((int) (world.dayTime() - 1));
					DayLengthModVariables.MapVariables.get(world).counter = DayLengthModVariables.MapVariables.get(world).counter + 1;
					DayLengthModVariables.MapVariables.get(world).syncData(world);
				}
			} else {
				if (world instanceof ServerLevel _level)
					_level.setDayTime((int) ((world.dayTime() + 20 / (world instanceof ServerLevel _serverLevelGR9 ? _serverLevelGR9.getGameRules().getInt(DayLengthModGameRules.CUSTOMDAYLENGTH) : 0)) - 1));
			}
		}
	}
}
