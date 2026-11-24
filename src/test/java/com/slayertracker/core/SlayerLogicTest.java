package com.slayertracker.core;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Plain Java test with made-up game text. Run by the GitHub build. */
public class SlayerLogicTest {
	static final Map<SlayerType, Long> xp = new EnumMap<>(SlayerType.class);
	static SlayerLogic logic;
	static long now;
	static QuestInfo shown;

	public static void main(String[] args) {
		parsing();
		autoSlayer();
		lastTimes();
		duplicateMessages();
		failureAndBreak();
		joinMidQuest();
		xpSync();
		System.out.println("All tests passed");
	}

	static void fresh() {
		xp.clear();
		logic = new SlayerLogic(new SlayerLogic.XpStore() {
			public long get(SlayerType t) { return xp.getOrDefault(t, -1L); }
			public void set(SlayerType t, long v) { xp.put(t, v); }
		});
		now = 10_000;
		shown = null;
		logic.tick(now);
	}

	static void run(long ms) {
		for (long end = now + ms; now < end;) {
			now += 50;
			logic.tick(now);
			if (shown != null) logic.quest(shown, now);
		}
	}

	static QuestInfo quest(String status) {
		return QuestInfo.parse(List.of("Slayer Quest", "Inferno Demonlord IV", status));
	}

	/** Farm for farmMs, then fight for fightMs. */
	static void spawnAndFight(long farmMs, long fightMs) {
		shown = quest("(0/100) Combat XP");
		run(farmMs / 2);
		shown = quest("(50/100) Combat XP");
		run(farmMs - farmMs / 2);
		shown = quest("Slay the boss!");
		run(fightMs);
	}

	static void parsing() {
		QuestInfo q = QuestInfo.parse(List.of("Slayer Quest", "§cInferno Demonlord 🍫§cIV", "§7(§e1,234§7/§c5,000§7) §7Combat XP"));
		check(q != null && q.type() == SlayerType.BLAZE && q.tier() == 4 && q.phase() == QuestInfo.Phase.FARMING, "parse farming");
		check(quest("  Slay the boss! ").phase() == QuestInfo.Phase.BOSS, "parse boss");
		check(QuestInfo.parse(List.of("Purse: 5")) == null, "no quest");
		leaderboard();
	}

	/** Text copied from a screenshot of the real menu. */
	static void leaderboard() {
		List<String> item = List.of("§bSlayer Leaderboards", "Ultimate bragging rights", "",
				"Revenant Horror: §a11,854,362 §7(Top §a0.06%§7)",
				"Tarantula Broodfather: 6,504,568 (Top 0.08%)",
				"Sven Packmaster: 3,777,880 (#818)",
				"Voidgloom Seraph: 9,925,366 (#169)",
				"Riftstalker Bloodfiend: 2,535 (Top 40.89%)",
				"Inferno Demonlord: 3,921,497 (#847)");
		Map<SlayerType, Long> totals = MenuXp.read(item);
		check(totals.size() == 6, "all six slayers");
		check(totals.get(SlayerType.ZOMBIE) == 11_854_362L && totals.get(SlayerType.SPIDER) == 6_504_568L
				&& totals.get(SlayerType.WOLF) == 3_777_880L && totals.get(SlayerType.ENDERMAN) == 9_925_366L
				&& totals.get(SlayerType.VAMPIRE) == 2_535L && totals.get(SlayerType.BLAZE) == 3_921_497L, "totals");
		check(MenuXp.read(List.of("Slayer Leaderboards", "Revenant Horror: N/A")).isEmpty(), "no kills yet");
		check(MenuXp.read(List.of("Other item", "Revenant Horror: 5")).isEmpty(), "other items ignored");

		fresh();
		SlayerLogic.XpStore store = new SlayerLogic.XpStore() {
			public long get(SlayerType t) { return xp.getOrDefault(t, -1L); }
			public void set(SlayerType t, long v) { xp.put(t, v); }
		};
		xp.put(SlayerType.ZOMBIE, 11_878_362L);
		MenuXp.apply(totals, store);
		check(xp.get(SlayerType.ZOMBIE) == 11_878_362L, "lower number does not replace");
		check(xp.get(SlayerType.BLAZE) == 3_921_497L, "unknown total is filled in");
	}

	static void autoSlayer() {
		fresh();
		xp.put(SlayerType.BLAZE, 5_244_000L);
		logic.chat("  SLAYER QUEST STARTED!", now);
		for (int i = 0; i < 3; i++) {
			spawnAndFight(20_000, 10_000);
			logic.chat("SLAYER QUEST COMPLETE!", now);
			logic.chat("SLAYER QUEST STARTED!", now);
		}
		SlayerStats s = logic.stats(SlayerType.BLAZE);
		near(s.averageSpawnSeconds(), 20, "spawn average");
		near(s.averageBossSeconds(), 10, "boss average");
		near(s.bossesPerHour(), 120, "boss/h");
		check(xp.get(SlayerType.BLAZE) == 5_245_500L, "XP added for three tier IV bosses");
		logic.reset();
		check(logic.stats(SlayerType.BLAZE).bossesPerHour() < 0 && xp.get(SlayerType.BLAZE) == 5_245_500L, "reset keeps XP");
	}

	static void lastTimes() {
		fresh();
		check(logic.stats(SlayerType.BLAZE).lastSpawnSeconds() < 0, "no last time at the start");
		logic.chat("SLAYER QUEST STARTED!", now);
		spawnAndFight(20_000, 30_000);
		logic.chat("SLAYER QUEST COMPLETE!", now);
		logic.chat("SLAYER QUEST STARTED!", now);
		spawnAndFight(11_000, 22_000);
		logic.chat("SLAYER QUEST COMPLETE!", now);
		SlayerStats s = logic.stats(SlayerType.BLAZE);
		near(s.lastSpawnSeconds(), 11, "last spawn");
		near(s.lastBossSeconds(), 22, "last boss");
		logic.reset();
		check(logic.stats(SlayerType.BLAZE).lastBossSeconds() < 0, "reset clears last times");
	}

	static void duplicateMessages() {
		fresh();
		xp.put(SlayerType.BLAZE, 0L);
		logic.chat("SLAYER QUEST STARTED!", now);
		spawnAndFight(20_000, 10_000);
		shown = quest("Boss slain!");
		run(50);
		logic.chat("NICE! SLAYER BOSS SLAIN!", now);
		logic.chat("SLAYER QUEST STARTED!", now);
		logic.chat("SLAYER QUEST COMPLETE!", now);
		check(xp.get(SlayerType.BLAZE) == 500L, "one kill counted once");
		fresh();
		xp.put(SlayerType.BLAZE, 0L);
		logic.chat("SLAYER QUEST STARTED!", now);
		spawnAndFight(20_000, 10_000);
		logic.chat("SLAYER QUEST COMPLETE!", now);
		run(1000); // sidebar still shows the old boss for a moment
		run(5000);
		logic.chat("SLAYER QUEST COMPLETE!", now);
		check(xp.get(SlayerType.BLAZE) == 500L, "late second message ignored");
		logic.chat("[MVP] someone: SLAYER QUEST STARTED!", now);
		check(logic.describeState().contains("DONE"), "player chat ignored");
	}

	static void failureAndBreak() {
		fresh();
		logic.chat("SLAYER QUEST STARTED!", now);
		spawnAndFight(20_000, 10_000);
		logic.chat("SLAYER QUEST FAILED!", now);
		shown = null;
		run(600_000); // long break
		logic.chat("SLAYER QUEST STARTED!", now);
		spawnAndFight(20_000, 10_000);
		logic.chat("SLAYER QUEST COMPLETE!", now);
		SlayerStats s = logic.stats(SlayerType.BLAZE);
		near(s.averageBossSeconds(), 10, "failed fight not in boss average");
		near(s.bossesPerHour(), 60, "failed time lowers boss/h, break not counted");
	}

	static void joinMidQuest() {
		fresh();
		spawnAndFight(5_000, 10_000);
		logic.chat("SLAYER QUEST COMPLETE!", now);
		SlayerStats s = logic.stats(SlayerType.BLAZE);
		check(s.bossesPerHour() < 0 && s.averageSpawnSeconds() < 0, "partial quest not used for rate or spawn");
	}

	static void xpSync() {
		fresh();
		logic.chat("SLAYER QUEST STARTED!", now);
		spawnAndFight(5_000, 5_000);
		logic.chat("SLAYER QUEST COMPLETE!", now);
		check(!xp.containsKey(SlayerType.BLAZE), "unknown total stays unknown");
		logic.chat("   Blaze Slayer LVL 6 - Next LVL in 1,000 XP!", now);
		check(xp.get(SlayerType.BLAZE) == 99_000L, "total from level line");
	}

	static void near(double actual, double expected, String name) {
		check(Math.abs(actual - expected) < expected * 0.02, name + ": " + actual);
	}

	static void check(boolean ok, String name) {
		if (!ok) throw new AssertionError(name);
	}
}
