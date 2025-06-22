package com.anchorstudios.daylength;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(DayLength.MODID)
public class DayLength {
    public static final String MODID = "daylength";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long VANILLA_DAY_LENGTH = 24000L;
    private static final long VANILLA_DAY_DURATION = 1200L;
    private static final Map<ServerLevel, Double> timeAccumulator = new HashMap<>();

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

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Day Length mod initialized!");
    }

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public DayLength(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::commonSetup);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Disable vanilla daylight cycle when our mod is active
        MinecraftServer server = event.getServer();
        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Ensure daylight cycle is always disabled
        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);

        for (ServerLevel level : server.getAllLevels()) {
            long newTime = calculateTime(level, server);
            level.setDayTime(newTime);
        }
    }

    private long calculateTime(ServerLevel level, MinecraftServer server) {
        GameRules rules = level.getGameRules();

        // Real time sync takes priority
        if (rules.getBoolean(DayLength.RULE_REALTIMESYNC)) {
            return calculateRealTime(level, server);
        }

        // Custom day length handling
        int customDayLength = rules.getInt(DayLength.RULE_CUSTOMDAYLENGTH);

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

    private long calculateRealTime(ServerLevel level, MinecraftServer server) {
        ZonedDateTime now = getCurrentTime();
        LocalTime localTime = now.toLocalTime();
        int totalSeconds = localTime.toSecondOfDay();
        long targetTick = (long)((totalSeconds / 86400.0) * DayLength.VANILLA_DAY_LENGTH);

        if (Config.SMOOTH_TIME_TRANSITION.get() && transitionState.active) {
            long currentTime = server.getTickCount() * 50;
            long transitionDuration = Config.SMOOTH_TIME_TRANSITION_DURATION.get() * 1000L;

            if (currentTime < transitionState.startTime + transitionDuration) {
                double factor = (double)(currentTime - transitionState.startTime) / transitionDuration;
                factor = factor * factor * (3 - 2 * factor);
                return (long)(transitionState.startTick + (transitionState.targetTick - transitionState.startTick) * factor);
            } else {
                transitionState.active = false;
            }
        }

        if (Config.SMOOTH_TIME_TRANSITION.get() &&
                Math.abs(targetTick - level.getDayTime()) > 100) {
            transitionState.startTime = server.getTickCount() * 50;
            transitionState.startTick = level.getDayTime();
            transitionState.targetTick = targetTick;
            transitionState.active = true;
        }

        return transitionState.active ? level.getDayTime() : targetTick;
    }

    private static ZonedDateTime getCurrentTime() {
        if (Config.USE_SERVER_TIME.get()) {
            return ZonedDateTime.now(ZoneId.of("UTC"));
        } else {
            int utcOffset = Config.MANUAL_UTC_OFFSET.get();
            return ZonedDateTime.now(ZoneId.of(utcOffset >= 0 ?
                    String.format("UTC+%d", utcOffset) :
                    String.format("UTC%d", utcOffset)));
        }
    }
}