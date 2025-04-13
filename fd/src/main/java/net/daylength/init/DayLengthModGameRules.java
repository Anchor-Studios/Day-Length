
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.daylength.init;

import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.level.GameRules;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class DayLengthModGameRules {
	public static final GameRules.Key<GameRules.BooleanValue> REALTIMESYNC = GameRules.register("realTimeSync", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
	public static final GameRules.Key<GameRules.IntegerValue> CUSTOMDAYLENGTH = GameRules.register("customDayLength", GameRules.Category.PLAYER, GameRules.IntegerValue.create(1440));
}
