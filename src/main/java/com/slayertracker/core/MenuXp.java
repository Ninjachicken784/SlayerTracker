package com.slayertracker.core;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads total XP for every slayer from the "Slayer Leaderboards" item in Maddox's "Slayer" menu.
 * Its lines look like "Revenant Horror: 11,854,362 (Top 0.06%)" or "Sven Packmaster: 3,777,880 (#818)".
 */
public final class MenuXp {
	private static final String ITEM_NAME = "Slayer Leaderboards";
	private static final Pattern LINE = Pattern.compile("^([A-Za-z ]+): ([\\d,]+)(?: \\(.*\\))?$");

	private MenuXp() { }

	/** Lines are the item name followed by its description. Empty for any other item. */
	public static Map<SlayerType, Long> read(List<String> rawLines) {
		Map<SlayerType, Long> totals = new EnumMap<>(SlayerType.class);
		if (rawLines.isEmpty() || !QuestInfo.clean(rawLines.get(0)).equals(ITEM_NAME)) return totals;
		for (String rawLine : rawLines) {
			Matcher m = LINE.matcher(QuestInfo.clean(rawLine));
			if (!m.matches()) continue;
			SlayerType type = SlayerType.fromBossLine(m.group(1));
			if (type == null) continue;
			try {
				totals.put(type, Long.parseLong(m.group(2).replace(",", "")));
			} catch (NumberFormatException ignored) {
				// not a number we can use
			}
		}
		return totals;
	}

	/**
	 * Slayer XP only goes up, so a lower number is not allowed to replace the saved one.
	 * This protects the count in case the leaderboard is behind.
	 */
	public static void apply(Map<SlayerType, Long> totals, SlayerLogic.XpStore store) {
		totals.forEach((type, xp) -> {
			if (xp > store.get(type)) store.set(type, xp);
		});
	}
}
