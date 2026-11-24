# Slayer Tracker

Fabric mod for Minecraft 26.1.2 (Hypixel SkyBlock). Needs Fabric API.

```
Blaze: 5,244,000
Boss/h: 99
Avg Spawn: 21.4s
Avg Boss: 12.8s
```

## Get the jar
GitHub: **Actions** tab, open the newest green **Build** run, download `slayertracker-jar`, unzip it,
and put `slayertracker-1.0.0.jar` (not the `-sources` one) in your `mods` folder.

## Commands
| Command | What it does |
|---|---|
| `/slayertracker move` | Drag the GUI |
| `/slayertracker toggle` | Show or hide the GUI |
| `/slayertracker reset` | Reset timing stats (total XP is kept) |
| `/slayertracker setxp <total>` | Set the current slayer's total XP |
| `/slayertracker xpbuff <percent>` | Extra slayer XP, for example 25 during a +25% buff (0 turns it off) |
| `/slayertracker debug` | Copy quest, chat and slayer menu text to the clipboard |

## How it counts
- Spawn time: quest start (chat or sidebar) to `Slay the boss!` on the sidebar (tab list as backup).
- Boss time: boss spawn to the kill message (`SLAYER QUEST COMPLETE!` or `NICE! SLAYER BOSS SLAIN!`) or `Boss slain!`.
- Boss/h: includes the time between quests and time lost to failed quests. Breaks over 2 minutes are left out.
- A quest the mod did not see from the start is not used for Boss/h or spawn time.
- Timing stats are per slayer and reset when you close the game. Total XP is saved (one set, not per profile).

## Total XP
Set by `/slayertracker setxp`, or read from the game if its text matches what the mod expects.
After that, each kill adds the XP for the boss tier.

The game text used for detection has not been tested in game yet.
