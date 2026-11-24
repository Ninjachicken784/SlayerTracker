package com.slayertracker;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.slayertracker.core.SlayerType;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class SlayerMod implements ClientModInitializer {
	private static boolean openMoveScreen;

	@Override
	public void onInitializeClient() {
		Config.load();
		Tracker.init();
		Hud.register();

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) Tracker.onChat(message.getString());
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> Tracker.onDisconnect());
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Tracker.tick(client);
			// Opened on the next tick, otherwise closing the chat would close it too.
			if (openMoveScreen) {
				openMoveScreen = false;
				client.setScreen(new MoveScreen());
			}
		});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommands(dispatcher));
	}

	// Not "/slayer", so that a server command with that name keeps working.
	private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommands.literal("slayertracker")
				.executes(context -> {
					say(context.getSource(), "Commands: move, toggle, reset, setxp <total>, xpbuff <percent>, debug");
					return 1;
				})
				.then(ClientCommands.literal("move").executes(context -> {
					openMoveScreen = true;
					return 1;
				}))
				.then(ClientCommands.literal("toggle").executes(context -> {
					Config.get().showHud = !Config.get().showHud;
					Config.save();
					say(context.getSource(), Config.get().showHud ? "GUI on." : "GUI off.");
					return 1;
				}))
				.then(ClientCommands.literal("reset").executes(context -> {
					Tracker.LOGIC.reset();
					say(context.getSource(), "Timing stats reset. Total XP kept.");
					return 1;
				}))
				.then(ClientCommands.literal("setxp")
						.then(ClientCommands.argument("total", LongArgumentType.longArg(0)).executes(context -> {
							SlayerType type = Tracker.LOGIC.currentType();
							if (type == null) {
								say(context.getSource(), "Start a slayer quest first, so I know which slayer this is.");
								return 0;
							}
							long total = LongArgumentType.getLong(context, "total");
							Config.get().setXp(type, total);
							say(context.getSource(), type.display + " total XP set to " + total + ".");
							return 1;
						})))
				.then(ClientCommands.literal("xpbuff")
						.then(ClientCommands.argument("percent", DoubleArgumentType.doubleArg(0, 500)).executes(context -> {
							double percent = DoubleArgumentType.getDouble(context, "percent");
							Config.get().xpBuffPercent = percent;
							Config.save();
							Tracker.LOGIC.setXpBuffPercent(percent);
							say(context.getSource(), "Slayer XP buff set to " + percent + "%.");
							return 1;
						})))
				.then(ClientCommands.literal("debug").executes(context -> {
					Minecraft mc = Minecraft.getInstance();
					mc.keyboardHandler.setClipboard(Tracker.debugText(mc));
					say(context.getSource(), "Debug text copied. Paste it where you need it.");
					return 1;
				})));
	}

	private static void say(FabricClientCommandSource source, String text) {
		source.sendFeedback(Component.literal("[Slayer Tracker] " + text));
	}
}
