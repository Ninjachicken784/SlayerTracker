package com.slayertracker.core;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Follows the slayer quest from chat messages and the sidebar. Has no Minecraft code, so it can be tested alone.
 * All times are in milliseconds.
 */
public final class SlayerLogic {
	/** Where total XP is kept (the mod saves it to a file). */
	public interface XpStore {
		/** Total XP, or -1 if not known yet. */
		long get(SlayerType type);

		void set(SlayerType type, long xp);
	}

	private enum State { NONE, FARMING, BOSS, DONE }

	static final long IDLE_LIMIT_MS = 120_000;
	static final long BOSS_IDLE_LIMIT_MS = 600_000;
	private static final long DUPLICATE_MS = 3000;
	private static final long STALE_MS = 2000;

	private static final Pattern LEVEL_LINE = Pattern.compile(
			"^(Zombie|Spider|Wolf|Enderman|Blaze|Vampire) Slayer LVL (\\d+) - Next LVL in ([\\d,]+) XP!$");

	private final XpStore xpStore;
	private final Map<SlayerType, SlayerStats> stats = new EnumMap<>(SlayerType.class);

	private State state = State.NONE;
	private SlayerType type;
	private int tier;
	private String lastProgress = "";

	private long questStart = -1; // -1: we did not see this quest start
	private long bossStart = -1; // -1: we did not see this boss spawn
	private boolean cycleComplete; // true if the time since the last kill/start was fully seen
	private long cycleMs; // active time not yet added to the stats (includes failed quests)
	private long idleMs; // time since the last activity, not yet counted
	private long lastTick = -1;
	private long lastKill = -1_000_000;
	private long lastXpSync = -1_000_000;
	private SlayerType lastXpSyncType;
	private long farmingWhileBossSince = -1;
	private double xpBuffPercent;

	public SlayerLogic(XpStore xpStore) {
		this.xpStore = xpStore;
	}

	public SlayerType currentType() {
		return type;
	}

	public void setCurrentType(SlayerType type) {
		this.type = type;
	}

	public void setXpBuffPercent(double percent) {
		this.xpBuffPercent = percent;
	}

	public SlayerStats stats(SlayerType type) {
		return stats.computeIfAbsent(type, t -> new SlayerStats());
	}

	public String describeState() {
		return "state=" + state + " type=" + type + " tier=" + tier;
	}

	/** Clears the timing stats. Total XP is not touched. */
	public void reset() {
		stats.clear();
		state = State.NONE;
		questStart = -1;
		bossStart = -1;
		cycleComplete = false;
		cycleMs = 0;
		idleMs = 0;
		farmingWhileBossSince = -1;
	}

	/** Call when you leave the server: the quest being watched can no longer be timed. */
	public void disconnect() {
		state = State.NONE;
		questStart = -1;
		bossStart = -1;
		cycleComplete = false;
		cycleMs = 0;
		idleMs = 0;
		lastTick = -1;
		farmingWhileBossSince = -1;
	}

	public void tick(long now) {
		if (lastTick >= 0 && (cycleComplete || state == State.FARMING || state == State.BOSS)) {
			idleMs += Math.max(0, Math.min(now - lastTick, 1000));
			long limit = state == State.BOSS ? BOSS_IDLE_LIMIT_MS : IDLE_LIMIT_MS;
			if (state == State.BOSS && idleMs <= limit) {
				activity(); // a boss fight counts as activity
			} else if (idleMs > limit) {
				idleMs = limit + 1; // long break: this stretch will be thrown away
			}
		}
		lastTick = now;
	}

	/** Moves the waiting time into the cycle, unless it was a long break. */
	private void activity() {
		if (idleMs <= IDLE_LIMIT_MS) cycleMs += idleMs;
		idleMs = 0;
	}

	/** One line of chat with no color codes. */
	public void chat(String rawLine, long now) {
		String line = QuestInfo.clean(rawLine);
		if (line.equals("SLAYER QUEST STARTED!")) {
			start(now, true);
		} else if (line.equals("SLAYER QUEST COMPLETE!") || line.equals("NICE! SLAYER BOSS SLAIN!")) {
			kill(now);
		} else if (line.equals("SLAYER QUEST FAILED!")) {
			if (state == State.FARMING || state == State.BOSS) {
				activity();
				state = State.DONE;
				bossStart = -1;
				questStart = -1;
			}
		} else {
			Matcher m = LEVEL_LINE.matcher(line);
			if (m.matches()) {
				SlayerType slayer = SlayerType.fromName(m.group(1));
				try {
					long total = slayer.totalFromNextLevel(Integer.parseInt(m.group(2)),
							Long.parseLong(m.group(3).replace(",", "")));
					if (total >= 0) {
						xpStore.set(slayer, total);
						lastXpSync = now;
						lastXpSyncType = slayer;
					}
				} catch (NumberFormatException ignored) {
					// not a number we can use
				}
			}
		}
	}

	/** The quest on the sidebar right now. Call only when there is one. */
	public void quest(QuestInfo info, long now) {
		if (info.type() != type) {
			boolean switched = type != null;
			type = info.type();
			if (switched) {
				// Time spent on another slayer must not count for this one.
				cycleMs = 0;
				idleMs = 0;
				if (state != State.FARMING || now - questStart > 5000) {
					state = State.NONE;
					questStart = -1;
					bossStart = -1;
					cycleComplete = false;
				}
			}
		}
		tier = info.tier();
		boolean changed = !info.progress().equals(lastProgress);
		lastProgress = info.progress();
		if (info.phase() != QuestInfo.Phase.FARMING) farmingWhileBossSince = -1;

		switch (info.phase()) {
			case FARMING -> {
				if (state == State.DONE) {
					// A new quest whose chat message we missed. Wait a moment so an old sidebar is not read as new.
					if (now - lastKill > STALE_MS) start(now, true);
				} else if (state == State.NONE) {
					start(now, false);
				} else if (state == State.BOSS) {
					if (farmingWhileBossSince < 0) {
						farmingWhileBossSince = now;
					} else if (now - farmingWhileBossSince > STALE_MS) {
						start(now, false); // the old boss is gone and we do not know how
					}
				} else if (changed) {
					activity();
				}
			}
			case BOSS -> {
				if (state == State.FARMING && (questStart < 0 || now - questStart > 1000)) {
					activity();
					if (questStart >= 0) {
						SlayerStats s = stats(type);
						s.spawnCount++;
						s.spawnMs += now - questStart;
						s.lastSpawnMs = now - questStart;
					}
					bossStart = now;
					state = State.BOSS;
				} else if (state == State.NONE) {
					bossStart = -1; // joined in the middle of a fight
					state = State.BOSS;
				}
			}
			case SLAIN -> {
				if (state == State.BOSS) kill(now);
			}
		}
	}

	private void start(long now, boolean sawStart) {
		if (state == State.FARMING && sawStart && questStart >= 0 && now - questStart < DUPLICATE_MS) return;
		activity();
		state = State.FARMING;
		questStart = sawStart ? now : -1;
		bossStart = -1;
		farmingWhileBossSince = -1;
		if (!sawStart) {
			cycleComplete = false;
		} else if (!cycleComplete) {
			cycleComplete = true;
			cycleMs = 0;
			idleMs = 0;
		}
	}

	private void kill(long now) {
		// Several messages can report the same kill, so only the first one counts.
		if (state == State.DONE || now - lastKill < DUPLICATE_MS) return;
		lastKill = now;
		activity();
		if (type != null) {
			SlayerStats s = stats(type);
			if (state == State.BOSS && bossStart >= 0) {
				s.bossCount++;
				s.bossMs += now - bossStart;
				s.lastBossMs = now - bossStart;
			}
			if (cycleComplete && cycleMs > 0) {
				s.ratedKills++;
				s.ratedMs += cycleMs;
			}
			addXp(now);
		}
		state = State.DONE;
		questStart = -1;
		bossStart = -1;
		cycleMs = 0;
		idleMs = 0;
		cycleComplete = true; // from here on we see the whole next cycle
	}

	private void addXp(long now) {
		long total = xpStore.get(type);
		// If the game just told us the exact total, it already includes this boss.
		boolean justSynced = lastXpSyncType == type && now - lastXpSync < DUPLICATE_MS;
		if (total < 0 || justSynced) return;
		long gain = Math.round(type.xpForTier(tier) * (1 + xpBuffPercent / 100.0));
		if (gain > 0) xpStore.set(type, total + gain);
	}
}
