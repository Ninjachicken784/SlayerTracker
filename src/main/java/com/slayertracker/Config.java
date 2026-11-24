package com.slayertracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.slayertracker.core.SlayerType;

import net.fabricmc.loader.api.FabricLoader;

/** Saved settings and total XP (config/slayertracker.json). */
public final class Config {
	public boolean showHud = true;
	public int hudX = 4;
	public int hudY = 40;
	public double xpBuffPercent = 0;
	public String lastSlayer = "";
	public Map<String, Long> totalXp = new HashMap<>();

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Config instance = new Config();

	public static Config get() {
		return instance;
	}

	public long xp(SlayerType type) {
		Long value = totalXp.get(type.name());
		return value == null ? -1 : value;
	}

	public void setXp(SlayerType type, long xp) {
		Long previous = totalXp.put(type.name(), xp);
		if (previous == null || previous != xp) save();
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("slayertracker.json");
	}

	public static void load() {
		Path path = file();
		if (!Files.exists(path)) return;
		try {
			Config loaded = GSON.fromJson(Files.readString(path), Config.class);
			if (loaded != null) instance = loaded;
		} catch (Exception e) {
			System.err.println("[slayertracker] Could not read config, using defaults: " + e);
		}
		if (instance.totalXp == null) instance.totalXp = new HashMap<>();
		if (instance.lastSlayer == null) instance.lastSlayer = "";
	}

	public static void save() {
		try {
			Files.writeString(file(), GSON.toJson(instance));
		} catch (IOException e) {
			System.err.println("[slayertracker] Could not save config: " + e);
		}
	}
}
