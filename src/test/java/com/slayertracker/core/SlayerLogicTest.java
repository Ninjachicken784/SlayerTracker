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
		check(MenuXp.read(List.of("Blaze Slayer", "Total XP: 5,244,000")).xp() == 5_244_000, "menu total");
		check(MenuXp.read(List.of("Blaze Slayer", "XP: 50/100")) == null, "menu ignores unclear text");
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
