package com.anchorstudios.daylength;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue USE_SERVER_TIME = BUILDER
            .comment("Use server time instead of system clock.")
            .define("useServerTime", true);

    public static final ModConfigSpec.IntValue MANUAL_UTC_OFFSET = BUILDER
            .comment("Sets your custom time zone offset (in hours) relative to UTC (Coordinated Universal Time). This is only used if useServerTime is set to false. It lets you simulate real-time sync based on any time zone, regardless of your server or system clock.")
            .defineInRange("manualUtcOffset", 0, -12, 14);

    public static final ModConfigSpec.BooleanValue SMOOTH_TIME_TRANSITION = BUILDER
            .comment("Controls whether time changes — such as switching time zones, enabling real-time sync or updating custom day length — happen instantly or gradually.")
            .define("smoothTimeTransition", true);

    public static final ModConfigSpec.IntValue SMOOTH_TIME_TRANSITION_DURATION = BUILDER
            .comment("How many real-world seconds the transition to a new time should take when smoothTimeTransition is true.")
            .defineInRange("smoothTransitionDuration", 10, 1, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();
}