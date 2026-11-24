package com.slayertracker.core;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads total XP from a menu item. Deliberately strict: a wrong total is worse than none.
 * It accepts "Blaze Slayer XP: 1,234" or, on an item that names the slayer, "Total XP: 1,234".
 */
public final class MenuXp {
	public record Result(SlayerType type, long xp) { }

	private static final Pattern NAMED = Pattern.compile(
			"^(?:Total )?(Zombie|Spider|Wolf|Enderman|Blaze|Vampire) Slayer XP: ([\\d,]+)$");
	private static final Pattern TOTAL = Pattern.compile("^Total (?:Slayer )?XP: ([\\d,]+)$");
	private static final Pattern SLAYER = Pattern.compile("\\b(Zombie|Spider|Wolf|Enderman|Blaze|Vampire) Slayer\\b");

	private MenuXp() { }

	/** Lines are the item name followed by its description. Null if no total is found. */
	public static Result read(List<String> rawLines) {
		List<String> lines = rawLines.stream().map(QuestInfo::clean).toList();
		SlayerType context = null;
		int contexts = 0;
		for (String line : lines) {
			Matcher m = SLAYER.matcher(line);
			while (m.find()) {
				SlayerType found = SlayerType.fromName(m.group(1));
				if (found != context) {
					context = found;
					contexts++;
				}
			}
		}
		for (String line : lines) {
			Matcher named = NAMED.matcher(line);
			if (named.matches()) return result(SlayerType.fromName(named.group(1)), named.group(2));
			Matcher total = TOTAL.matcher(line);
			if (contexts == 1 && total.matches()) return result(context, total.group(1));
		}
		return null;
	}

	private static Result result(SlayerType type, String number) {
		try {
			return new Result(type, Long.parseLong(number.replace(",", "")));
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
