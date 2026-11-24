package com.slayertracker.core;

/** The six slayers, with the boss names shown on the sidebar and the XP tables from the wiki. */
public enum SlayerType {
	ZOMBIE("Zombie", new String[] { "Revenant Horror", "Atoned Horror" }, new long[] { 5, 15, 200, 1000, 5000, 20000, 100000, 400000, 1000000 }),
	SPIDER("Spider", new String[] { "Tarantula Broodfather", "Conjoined Brood" }, new long[] { 5, 25, 200, 1000, 5000, 20000, 100000, 400000, 1000000 }),
	WOLF("Wolf", new String[] { "Sven Packmaster" }, new long[] { 10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000 }),
	ENDERMAN("Enderman", new String[] { "Voidgloom Seraph" }, new long[] { 10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000 }),
	BLAZE("Blaze", new String[] { "Inferno Demonlord" }, new long[] { 10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000 }),
	VAMPIRE("Vampire", new String[] { "Riftstalker Bloodfiend", "Bloodfiend" }, new long[] { 20, 75, 240, 840, 2400 });

	public final String display;
	private final String[] bossNames;
	private final long[] levels; // total XP needed for LVL 1, 2, ...

	SlayerType(String display, String[] bossNames, long[] levels) {
		this.display = display;
		this.bossNames = bossNames;
		this.levels = levels;
	}

	/** Slayer XP for one boss of this tier (1-5), or 0 if the tier is unknown. */
	public int xpForTier(int tier) {
		if (tier < 1 || tier > 5) return 0;
		int[] table = this == VAMPIRE
				? new int[] { 10, 25, 60, 120, 150 }
				: new int[] { 5, 25, 100, 500, 1500 };
		return table[tier - 1];
	}

	/** Total XP worked out from "LVL x - Next LVL in y XP", or -1 if it cannot be worked out. */
	public long totalFromNextLevel(int level, long remaining) {
		if (level < 0 || level >= levels.length) return -1;
		long total = levels[level] - remaining;
		return total >= 0 ? total : -1;
	}

	public static SlayerType fromBossLine(String line) {
		for (SlayerType type : values()) {
			for (String name : type.bossNames) {
				if (line.startsWith(name)) return type;
			}
		}
		return null;
	}

	public static SlayerType fromName(String name) {
		for (SlayerType type : values()) {
			if (type.display.equalsIgnoreCase(name)) return type;
		}
		return null;
	}
}
