# EvilSMPAbilities Reborn
New replacement plugin; remove the old EvilSMPAbilities.jar before installing.

## Build
Upload the CONTENTS of this folder to the ROOT of a GitHub repository (pom.xml must be at repository root). GitHub Actions > successful Build Plugin run > download EvilSMPAbilitiesReborn-jar artifact > extract ZIP > put EvilSMPAbilitiesReborn.jar into your Paper server plugins folder and restart.

Requires a compatible Paper 1.21.x API and Java 21+. **Not yet compiled or tested with your exact server or DragonEggBuffs.**

## Admin commands
/evilsmpitems give <player> tickle
/evilsmpitems give <player> nullifier
/evilsmpitems give <player> armourbreaker
/evilsmpitems give <player> restrictor

## Abilities
- Tickle Feather: hit applies Slow Falling 3 minutes, 5 minute item cooldown.
- Nullifier (enchanted obsidian): hit suppresses positive potion effects, applies Weakness II and Mining Fatigue I, and attempts to limit effective max health to 10 hearts for 5 minutes; 5 minute cooldown. Re-checks every 2 ticks to counter buff reapplication.
- Armour Breaker (enchanted netherite axe): shift-left click to enable for 5 seconds, then successful hits apply 50 direct durability damage to each equipped armour piece; 2 minute cooldown. Ignores Unbreaking on the extra durability damage.
- Restrictor (enchanted comparator): hit disables gliding, spear interactions and XP bottle throws for 5 minutes; 5 minute cooldown.

## Known limitations
- Native Bukkit cooldown overlay is shared by all items of the same material, including normal obsidian, feather, comparator and netherite axe. Cooldowns are per-player, in-memory, and reset on server restart.
- DragonEggBuffs compatibility depends on how that plugin changes max health and re-applies effects. This implementation attempts to counter modifiers, but should be tested; it may not override every implementation. It may also suppress beneficial effects from other sources.
- Spear detection uses material names ending in _SPEAR; verify on your server version.
- Nullifier status is in-memory and resets on restart.
