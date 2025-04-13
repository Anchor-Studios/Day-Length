
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.daylength.init;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.GameRules;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class DayLengthModGameRules {
	public static GameRules.Key<GameRules.IntegerValue> CUSTOMDAYLENGTH;
	public static GameRules.Key<GameRules.BooleanValue> REALTIMESYNC;

	@SubscribeEvent
	public static void registerGameRules(FMLCommonSetupEvent event) {
		CUSTOMDAYLENGTH = GameRules.register("customDayLength", GameRules.Category.PLAYER, GameRules.IntegerValue.create(20));
		REALTIMESYNC = GameRules.register("realTimeSync", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
	}
}
