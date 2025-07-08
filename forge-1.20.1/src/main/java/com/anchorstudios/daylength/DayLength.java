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
import java.util.HashMap;
import java.util.Map;

@Mod(DayLength.MODID)
public class DayLength {
    public static final String MODID = "daylength";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long VANILLA_DAY_LENGTH = 24000L;
    private static final long VANILLA_DAY_DURATION = 1200L;
    private static final Map<ServerLevel, Double> timeAccumulator = new HashMap<>();
    private static boolean isSleeping = false;

    // Transition state
    private static class TransitionState {
        long startTime;
        long startTick;
        long targetTick;
        boolean active;
    }
    private static final TransitionState transitionState = new TransitionState();

    // Custom game rules
    public static final GameRules.Key<GameRules.IntegerValue> RULE_CUSTOMDAYLENGTH = GameRules.register(
            "customDayLength",
            GameRules.Category.UPDATES,
            GameRules.IntegerValue.create(20)
    );

    public static final GameRules.Key<GameRules.BooleanValue> RULE_REALTIMESYNC = GameRules.register(
            "realTimeSync",
            GameRules.Category.UPDATES,
            GameRules.BooleanValue.create(false)
    );

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Disable vanilla daylight cycle when our mod is active
        MinecraftServer server = event.getServer();
        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
    }

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

            // Check if players are sleeping
            boolean allPlayersSleeping = server.getPlayerList().getPlayers().stream()
                    .allMatch(player -> player.isSleeping() || !player.level().dimensionType().natural());
            isSleeping = allPlayersSleeping && !server.getPlayerList().getPlayers().isEmpty();

            for (ServerLevel level : server.getAllLevels()) {
                long newTime = calculateTime(level, server);

                // Only set time if not in realtime sync mode or if sleeping
                if (!level.getGameRules().getBoolean(RULE_REALTIMESYNC) || isSleeping) {
                    level.setDayTime(newTime);
                }
            }
        }

        private static long calculateTime(ServerLevel level, MinecraftServer server) {
            GameRules rules = level.getGameRules();

            // Handle sleep time acceleration
            if (isSleeping) {
                return level.getDayTime() + (VANILLA_DAY_LENGTH / 100); // Fast-forward time during sleep
            }

            // Real time sync takes priority
            if (rules.getBoolean(RULE_REALTIMESYNC)) {
                return calculateRealTime(level, server);
            }

            // Custom day length handling
            int customDayLength = rules.getInt(RULE_CUSTOMDAYLENGTH);

            // If day length is 0, time is frozen
            if (customDayLength == 0) {
                return level.getDayTime();
            }

            // Vanilla day length behavior
            if (customDayLength == 20) {
                return level.getDayTime() + 1L;
            }

            // Custom day length progression
            double speedFactor = (20.0 * 60.0) / (customDayLength * 60.0); // ticks per tick
            double accumulated = timeAccumulator.getOrDefault(level, 0.0);
            accumulated += speedFactor;

            long ticksToAdd = (long) accumulated;
            accumulated -= ticksToAdd;

            timeAccumulator.put(level, accumulated);
            return level.getDayTime() + ticksToAdd;
        }

        private static long calculateRealTime(ServerLevel level, MinecraftServer server) {
            GameRules rules = level.getGameRules();
            Config.Common config = Config.COMMON;

            ZonedDateTime now = getCurrentTime(config);
            LocalTime localTime = now.toLocalTime();
            int totalSeconds = localTime.toSecondOfDay();
            long targetTick = (long)((totalSeconds / 86400.0) * VANILLA_DAY_LENGTH);

            if (config.smoothTimeTransition.get() && transitionState.active) {
                long currentTime = server.getTickCount() * 50;
                long transitionDuration = config.smoothTransitionDuration.get() * 1000L;

                if (currentTime < transitionState.startTime + transitionDuration) {
                    double factor = (double)(currentTime - transitionState.startTime) / transitionDuration;
                    factor = factor * factor * (3 - 2 * factor);
                    return (long)(transitionState.startTick + (transitionState.targetTick - transitionState.startTick) * factor);
                } else {
                    transitionState.active = false;
                }
            }

            if (config.smoothTimeTransition.get() &&
                    Math.abs(targetTick - level.getDayTime()) > 100) {
                transitionState.startTime = server.getTickCount() * 50;
                transitionState.startTick = level.getDayTime();
                transitionState.targetTick = targetTick;
                transitionState.active = true;
            }

            return transitionState.active ? level.getDayTime() : targetTick;
        }

        private static ZonedDateTime getCurrentTime(Config.Common config) {
            if (config.useServerTime.get()) {
                return ZonedDateTime.now(ZoneId.of("UTC"));
            } else {
                int utcOffset = config.manualUtcOffset.get();
                return ZonedDateTime.now(ZoneId.of(utcOffset >= 0 ?
                        String.format("UTC+%d", utcOffset) :
                        String.format("UTC%d", utcOffset)));
            }
        }
    }
}