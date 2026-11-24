package com.slayertracker;

import java.util.Locale;

import com.slayertracker.core.SlayerStats;
import com.slayertracker.core.SlayerType;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

/** The small text box. */
public final class Hud {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GOLD = 0xFFFFAA00;

	private Hud() { }

	public static void register() {
		HudElementRegistry.attachElementAfter(
				VanillaHudElements.SUBTITLES,
				Identifier.fromNamespaceAndPath("slayertracker", "hud"),
				Hud::render);
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.screen instanceof MoveScreen) return;
		if (!Config.get().showHud || Tracker.LOGIC.currentType() == null) return;
		draw(graphics, mc, x(mc, graphics.guiWidth()), y(mc, graphics.guiHeight()));
	}

	public static String[] lines() {
		SlayerType type = Tracker.LOGIC.currentType() == null ? SlayerType.BLAZE : Tracker.LOGIC.currentType();
		SlayerStats stats = Tracker.LOGIC.stats(type);
		long xp = Config.get().xp(type);
		return new String[] {
			type.display + ": " + (xp < 0 ? "? (/slayertracker setxp)" : String.format(Locale.US, "%,d", xp)),
			"Boss/h: " + (stats.bossesPerHour() < 0 ? "--" : Long.toString(Math.round(stats.bossesPerHour()))),
			"Avg Spawn: " + seconds(stats.averageSpawnSeconds()),
			"Avg Boss: " + seconds(stats.averageBossSeconds()),
			"Last Spawn: " + seconds(stats.lastSpawnSeconds()),
			"Last Boss: " + seconds(stats.lastBossSeconds())
		};
	}

	private static String seconds(double value) {
		return value < 0 ? "--" : String.format(Locale.US, "%.1fs", value);
	}

	public static int width(Minecraft mc) {
		int widest = 0;
		for (String line : lines()) widest = Math.max(widest, mc.font.width(line));
		return widest;
	}

	public static int height(Minecraft mc) {
		return lines().length * (mc.font.lineHeight + 1);
	}

	/** Saved position, kept inside the screen. */
	public static int x(Minecraft mc, int screenWidth) {
		return Math.max(0, Math.min(Config.get().hudX, screenWidth - width(mc)));
	}

	public static int y(Minecraft mc, int screenHeight) {
		return Math.max(0, Math.min(Config.get().hudY, screenHeight - height(mc)));
	}

	public static void draw(GuiGraphicsExtractor graphics, Minecraft mc, int x, int y) {
		String[] lines = lines();
		for (int i = 0; i < lines.length; i++) {
			graphics.text(mc.font, lines[i], x, y + i * (mc.font.lineHeight + 1), i == 0 ? GOLD : WHITE);
		}
	}
}
