package com.slayertracker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/** Reads the text the game is showing: sidebar, tab list and menu items. */
public final class GameText {
	private GameText() { }

	/** Sidebar lines from top to bottom. */
	public static List<String> sidebar(Minecraft mc) {
		List<String> lines = new ArrayList<>();
		if (mc.level == null) return lines;
		Scoreboard scoreboard = mc.level.getScoreboard();
		Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		if (objective == null) return lines;
		List<PlayerScoreEntry> entries = new ArrayList<>(scoreboard.listPlayerScores(objective));
		entries.removeIf(PlayerScoreEntry::isHidden);
		entries.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed()
				.thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER));
		for (PlayerScoreEntry entry : entries) {
			PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
			lines.add(PlayerTeam.formatNameForTeam(team, entry.ownerName()).getString());
		}
		return lines;
	}

	/** Tab list lines, ordered by team name (the order Hypixel uses for its info columns). */
	public static List<String> tabList(Minecraft mc) {
		List<String> lines = new ArrayList<>();
		if (mc.getConnection() == null) return lines;
		List<PlayerInfo> players = new ArrayList<>(mc.getConnection().getListedOnlinePlayers());
		players.sort(Comparator.comparing((PlayerInfo info) -> info.getTeam() == null ? "" : info.getTeam().getName())
				.thenComparing(info -> info.getProfile().name()));
		for (PlayerInfo info : players) {
			Component name = info.getTabListDisplayName();
			if (name != null) lines.add(name.getString());
		}
		return lines;
	}

	/** The item name followed by its description lines. */
	public static List<String> item(ItemStack stack) {
		List<String> lines = new ArrayList<>();
		lines.add(stack.getHoverName().getString());
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null) {
			for (Component line : lore.lines()) {
				lines.add(line.getString());
			}
		}
		return lines;
	}
}
