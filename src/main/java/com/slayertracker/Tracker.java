package com.slayertracker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import com.slayertracker.core.MenuXp;
import com.slayertracker.core.QuestInfo;
import com.slayertracker.core.SlayerLogic;
import com.slayertracker.core.SlayerType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/** Connects the game to the tracking logic. */
public final class Tracker {
	public static final SlayerLogic LOGIC = new SlayerLogic(new SlayerLogic.XpStore() {
		@Override
		public long get(SlayerType type) {
			return Config.get().xp(type);
		}

		@Override
		public void set(SlayerType type, long xp) {
			Config.get().setXp(type, xp);
		}
	});

	private static final int CHAT_KEPT = 40;
	private static final Deque<String> chatLog = new ArrayDeque<>();
	private static List<String> lastMenu = List.of();
	private static int ticks;
	private static boolean inWorld;

	private Tracker() { }

	public static void init() {
		LOGIC.setXpBuffPercent(Config.get().xpBuffPercent);
		for (SlayerType type : SlayerType.values()) {
			if (type.name().equals(Config.get().lastSlayer)) LOGIC.setCurrentType(type);
		}
	}

	public static void onChat(String message) {
		long now = System.currentTimeMillis();
		for (String line : message.split("\n")) {
			String clean = QuestInfo.clean(line);
			String lower = clean.toLowerCase(Locale.ROOT);
			// Only slayer-looking lines are kept for the debug copy.
			if (lower.contains("slayer") || lower.contains("boss")) {
				chatLog.addLast(clean);
				if (chatLog.size() > CHAT_KEPT) chatLog.removeFirst();
			}
			LOGIC.chat(clean, now);
		}
	}

	public static void onDisconnect() {
		LOGIC.disconnect();
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			if (inWorld) LOGIC.disconnect();
			inWorld = false;
			return;
		}
		inWorld = true;
		long now = System.currentTimeMillis();
		LOGIC.tick(now);

		QuestInfo quest = QuestInfo.parse(GameText.sidebar(mc));
		if (quest == null) quest = QuestInfo.parse(GameText.tabList(mc));
		if (quest != null) {
			LOGIC.quest(quest, now);
			if (!quest.type().name().equals(Config.get().lastSlayer)) {
				Config.get().lastSlayer = quest.type().name();
				Config.save();
			}
		}

		if (++ticks % 10 == 0 && mc.screen instanceof AbstractContainerScreen<?> screen) {
			readMenu(screen);
		}
	}

	private static void readMenu(AbstractContainerScreen<?> screen) {
		String title = QuestInfo.clean(screen.getTitle().getString());
		boolean slayerMenu = title.contains("Slayer") || SlayerType.fromBossLine(title) != null;
		if (!slayerMenu) return;
		List<String> copy = new ArrayList<>();
		copy.add("Menu: " + title);
		for (Slot slot : screen.getMenu().slots) {
			if (!slot.hasItem()) continue;
			List<String> lines = GameText.item(slot.getItem());
			if (lines.size() > 1) {
				copy.add("--- slot " + slot.index);
				for (String line : lines) copy.add(QuestInfo.clean(line));
			}
			MenuXp.Result result = MenuXp.read(lines);
			if (result != null) Config.get().setXp(result.type(), result.xp());
		}
		lastMenu = copy;
	}

	/** Everything needed to fix detection if the game's text differs from what the mod expects. */
	public static String debugText(Minecraft mc) {
		List<String> out = new ArrayList<>();
		out.add("== Slayer Tracker debug ==");
		out.add(LOGIC.describeState());
		out.add("== Sidebar ==");
		GameText.sidebar(mc).forEach(line -> out.add(QuestInfo.clean(line)));
		out.add("== Tab list (quest part) ==");
		List<String> tab = GameText.tabList(mc).stream().map(QuestInfo::clean).toList();
		for (int i = 0; i < tab.size(); i++) {
			if (SlayerType.fromBossLine(tab.get(i)) != null || tab.get(i).contains("Slayer")) {
				out.addAll(tab.subList(i, Math.min(tab.size(), i + 4)));
			}
		}
		out.add("== Recent slayer chat ==");
		out.addAll(chatLog);
		out.add("== Last slayer menu ==");
		out.addAll(lastMenu);
		return String.join("\n", out);
	}
}
