package com.slayertracker.core;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The slayer quest as shown on the sidebar (or in the tab list). */
public record QuestInfo(SlayerType type, int tier, Phase phase, String progress) {
	public enum Phase { FARMING, BOSS, SLAIN }

	private static final Pattern TIER = Pattern.compile("\\s(IV|V|I{1,3})$");
	private static final Pattern PROGRESS = Pattern.compile("^\\(?[\\d.,]+[kKmM]?\\s*/\\s*[\\d.,]+[kKmM]?\\)?\\s*(Combat XP|Kills)$");
	private static final Pattern FORMATTING = Pattern.compile("§.");

	/** Removes color codes, the invisible symbols Hypixel puts inside sidebar lines, and extra spaces. */
	public static String clean(String line) {
		return FORMATTING.matcher(line).replaceAll("")
				.replaceAll("[^\\x20-\\x7E]", "")
				.replaceAll("\\s+", " ")
				.trim();
	}

	/** Finds the quest in a list of lines: a boss name, followed shortly by its status. Null if there is none. */
	public static QuestInfo parse(List<String> rawLines) {
		List<String> lines = rawLines.stream().map(QuestInfo::clean).toList();
		for (int i = 0; i < lines.size(); i++) {
			SlayerType type = SlayerType.fromBossLine(lines.get(i));
			if (type == null) continue;
			Matcher tierMatch = TIER.matcher(lines.get(i));
			int tier = tierMatch.find() ? roman(tierMatch.group(1)) : 0;
			for (int j = i + 1; j < Math.min(lines.size(), i + 4); j++) {
				String status = lines.get(j);
				if (status.equalsIgnoreCase("Slay the boss!")) return new QuestInfo(type, tier, Phase.BOSS, status);
				if (status.equalsIgnoreCase("Boss slain!")) return new QuestInfo(type, tier, Phase.SLAIN, status);
				if (PROGRESS.matcher(status).matches()) return new QuestInfo(type, tier, Phase.FARMING, status);
			}
		}
		return null;
	}

	private static int roman(String numeral) {
		return switch (numeral) {
			case "I" -> 1;
			case "II" -> 2;
			case "III" -> 3;
			case "IV" -> 4;
			case "V" -> 5;
			default -> 0;
		};
	}
}
