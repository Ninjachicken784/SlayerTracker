package com.slayertracker.core;

/** Timing stats for one slayer type. Kept until reset or until the game closes. */
public final class SlayerStats {
	long ratedKills;
	long ratedMs;
	long spawnCount;
	long spawnMs;
	long bossCount;
	long bossMs;
	long lastSpawnMs = -1;
	long lastBossMs = -1;

	/** Bosses per hour, or -1 before there is enough data. */
	public double bossesPerHour() {
		return ratedKills == 0 || ratedMs <= 0 ? -1 : ratedKills * 3_600_000.0 / ratedMs;
	}

	/** Latest spawn time in seconds, or -1. */
	public double lastSpawnSeconds() {
		return lastSpawnMs < 0 ? -1 : lastSpawnMs / 1000.0;
	}

	/** Latest boss time in seconds, or -1. */
	public double lastBossSeconds() {
		return lastBossMs < 0 ? -1 : lastBossMs / 1000.0;
	}

	/** Average spawn time in seconds, or -1. */
	public double averageSpawnSeconds() {
		return spawnCount == 0 ? -1 : spawnMs / 1000.0 / spawnCount;
	}

	/** Average boss time in seconds, or -1. */
	public double averageBossSeconds() {
		return bossCount == 0 ? -1 : bossMs / 1000.0 / bossCount;
	}
}
