package com.anchorstudios.daylength;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Mod(DayLength.MODID)
public class DayLength {
    public static final String MODID = "daylength";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long VANILLA_DAY_LENGTH = 24000L; // Ticks in a vanilla Minecraft day
    private static long lastRealTimeTick = 0;
    private static long transitionStartTime = 0;
    private static long transitionStartTick = 0;
    private static long transitionTargetTick = 0;
    private static boolean isTransitioning = false;

    // Custom game rules
    public static final GameRules.Key<GameRules.IntegerValue> RULE_CUSTOMDAYLENGTH = GameRules.register(
            "customDayLength",
            GameRules.Category.UPDATES,
            GameRules.IntegerValue.create(20) // Default 20 minutes (vanilla)
    );

    public static final GameRules.Key<GameRules.BooleanValue> RULE_REALTIMESYNC = GameRules.register(
            "realTimeSync",
            GameRules.Category.UPDATES,
            GameRules.BooleanValue.create(false)
    );

    public DayLength(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Day Length mod initialized!");
    }

    @Mod.EventBusSubscriber(modid = DayLength.MODID)
    public static class TimeTickHandler {
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;

            for (ServerLevel level : server.getAllLevels()) {
                long newTime = calculateTime(level, server);
                level.setDayTime(newTime);
            }
        }

        private static long calculateTime(ServerLevel level, MinecraftServer server) {
            GameRules rules = level.getGameRules();

            // Check if real time sync is enabled
            if (rules.getBoolean(RULE_REALTIMESYNC)) {
                return calculateRealTime(level, server);
            }

            // Custom day length handling
            int customDayLength = rules.getInt(RULE_CUSTOMDAYLENGTH);
            if (customDayLength <= 0) customDayLength = 20; // Default to vanilla if invalid

            if (customDayLength == 20) {
                // Vanilla day length - no adjustment needed
                return level.getDayTime() + 1L;
            }

            // Calculate adjusted time progression
            double speedFactor = (20.0 * 60.0) / (customDayLength * 60.0); // Ratio of vanilla to custom day length
            long currentTime = level.getDayTime();
            return currentTime + (long)(speedFactor * 1.0); // Adjust progression speed
        }

        private static long calculateRealTime(ServerLevel level, MinecraftServer server) {
            GameRules rules = level.getGameRules();
            Config.Common config = Config.COMMON;

            // Get current time based on config
            ZonedDateTime now;
            if (config.useServerTime.get()) {
                now = ZonedDateTime.now(ZoneId.of("UTC")); // Server time is always UTC
            } else {
                int utcOffset = config.manualUtcOffset.get();
                now = ZonedDateTime.now(ZoneId.of(utcOffset >= 0 ?
                        String.format("UTC+%d", utcOffset) :
                        String.format("UTC%d", utcOffset)));
            }

            // Calculate total seconds in the day
            LocalTime localTime = now.toLocalTime();
            int totalSeconds = localTime.toSecondOfDay(); // 0-86399

            // Calculate target tick
            long targetTick = (long)((totalSeconds / 86400.0) * VANILLA_DAY_LENGTH);

            // Handle smooth transition if enabled
            if (config.smoothTimeTransition.get() && isTransitioning) {
                long currentTime = server.getTickCount() * 50; // Convert ticks to milliseconds
                long transitionDuration = config.smoothTransitionDuration.get() * 1000L;

                if (currentTime < transitionStartTime + transitionDuration) {
                    // Calculate interpolation factor (0.0 to 1.0)
                    double factor = (double)(currentTime - transitionStartTime) / transitionDuration;
                    // Linear interpolation between start and target
                    return (long)(transitionStartTick + (transitionTargetTick - transitionStartTick) * factor);
                } else {
                    isTransitioning = false;
                }
            }

            // Check if we need to start a new transition
            if (config.smoothTimeTransition.get() && Math.abs(targetTick - lastRealTimeTick) > 100) {
                startTransition(server, level.getDayTime(), targetTick);
            }

            lastRealTimeTick = targetTick;
            return targetTick;
        }

        private static void startTransition(MinecraftServer server, long currentTick, long targetTick) {
            transitionStartTime = server.getTickCount() * 50;
            transitionStartTick = currentTick;
            transitionTargetTick = targetTick;
            isTransitioning = true;
        }
    }
}