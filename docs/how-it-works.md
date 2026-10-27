# How Ultimate Cobblemon Progression Works

This guide explains exactly what UCP changes in Cobblemon, using the mod's default settings and tier data. Every number here comes from the code and the shipped data files; the full species list is in [Species by Tier and Rarity](species-by-tier.md).

## The short version

- Every player has a **trainer level** from 1 to 7. Each level is a **tier**.
- Each Pokémon species belongs to one tier, based on its strength (Base Stat Total). **Wild Pokémon above your tier can't spawn for you.**
- **Rarity is Cobblemon's, not UCP's.** Whether a Pokémon is common or ultra-rare is decided by Cobblemon's spawn data. UCP never changes those odds. A Pokémon can be low-tier and ultra-rare (Dratini), or high-tier and common (Slaking).
- Wild Pokémon **levels** are rerolled based on the Pokémon's tier and yours.
- Your own Pokémon **can't gain XP past your tier's level cap**.
- You **level up automatically** by seeing and catching enough different species in your Pokédex.

---

## 1. Trainer level and tiers

New players start at trainer level 1. Your trainer level is your tier: it decides which species can spawn for you and your Pokémon's level cap.

| Tier | Level cap | To reach it: species seen / caught | Pokémon in the tier (BST range) |
|---|---|---|---|
| 1 | 15 | You start here | 175 – 305 |
| 2 | 27 | 30 / 10 | 306 – 354 |
| 3 | 40 | 80 / 20 | 355 – 440 |
| 4 | 54 | 140 / 35 | 441 – 475 |
| 5 | 69 | 210 / 55 | 476 – 500 |
| 6 | 85 | 290 / 80 | 501 – 570 |
| 7 | 100 | 380 / 110 | 571 and up |

Tiers stack: at tier 4 you can meet Pokémon from tiers 1, 2, 3 and 4.

### How you level up

- **Seen** is the number of different species registered in your Pokédex (caught species count as seen too).
- **Caught** is the number of different species you've owned.
- Forms don't count separately: Alolan Vulpix and Vulpix are one species.

The check runs every time your Pokédex changes. When you meet **both** numbers for the next tier, you go up one level and get a chat message with your new level cap, how many species you unlocked, and what you need for the next level.

You only go up one level per Pokédex update. If you somehow already meet the requirements for several tiers (for example, after an admin changes the tier files), you'll climb one level each time you register a new Pokémon.

Use `/trainerlevel get` at any time to see your progress toward the next level.

---

## 2. Which Pokémon can spawn for you

Each time Cobblemon tries to spawn a wild Pokémon near you, UCP checks the species:

| Species tier compared to yours | Result |
|---|---|
| At or below your tier | Can spawn |
| Above your tier | Blocked |
| Not listed in any tier | Blocked by default (see `blockUnknownSpecies` in [Configuration](#6-configuration)) |

This applies to every kind of Cobblemon spawn: normal world spawns, fishing, PokeSnacks and bait.

**A Pokémon's forms always share its tier.** UCP assigns tiers to species, so Alolan, Galarian, Hisuian and Paldean forms use the base species' tier.

**Which player counts.** Cobblemon runs spawning separately around each player. The check uses the player whose spawner made the attempt. If two players are together, Pokémon spawned by the higher-tier player's spawner can appear near both of them. If a spawn isn't tied to a player, the nearest player within 128 blocks counts; if nobody is that close, tiered species are blocked.

**Legendaries.** Base Cobblemon doesn't spawn legendaries, mythicals, Ultra Beasts or Paradox Pokémon in the wild at all. Most of them sit in tiers 6 and 7 (a few weak ones, like Cosmog, Meltan, Kubfu and Poipole, are lower) so that anything else that spawns them (other mods, datapacks) follows the same rules.

---

## 3. Rarity: what's rare and what's not

### Cobblemon decides rarity

In Cobblemon, every wild spawn entry is in a **rarity bucket**. Each spawn attempt first rolls a bucket, then picks a Pokémon from that bucket that can spawn at that spot:

| Rarity | World spawns | Fishing / PokeSnack |
|---|---|---|
| Common | 94% | 83.25% |
| Uncommon | 5% | 11.25% |
| Rare | 0.5% | 4.125% |
| Ultra-rare | 0.2% | 1.375% |
| Boss | 0.3% | – |

These are Cobblemon's defaults (server admins can change them in `config/cobblemon/spawning/best-spawner-config.json`). **UCP doesn't change them.**

### Tier is not rarity

UCP's tiers measure strength, not rarity. The two are completely independent, which leads to some patterns worth knowing:

- **Pseudo-legendary first stages are tier 1 but hard to find.** Dratini, Larvitar, Bagon, Beldum, Gible, Deino, Goomy, Jangmo-o and Dreepy can spawn from the start, but mostly as rare or ultra-rare (Dratini and Goomy also show up as common in a few spots). Their final forms are tier 7.
- **Starters are tier 2 and ultra-rare.** Every first-stage starter (Bulbasaur, Charmander, Squirtle and the rest), plus the fossils Cranidos and Shieldon, is in tier 2's ultra-rare bucket. The starters' final evolutions are tier 6, also ultra-rare.
- **Tier 7 has very few wild Pokémon.** Only Slaking and the pseudo-legendary final forms (Dragonite, Tyranitar, Salamence, Metagross, Garchomp, Hydreigon, Goodra, Kommo-o, Dragapult, Archaludon) spawn naturally, and most of those mainly as rare or ultra-rare.
- **Strong doesn't mean rare.** Gyarados, Machamp and Gardevoir are tier 6 but common.

For every species' tier and rarity, see [Species by Tier and Rarity](species-by-tier.md).

### How tiers and rarity interact

1. Cobblemon rolls a rarity using the odds above. UCP doesn't affect this roll.
2. Cobblemon collects the Pokémon in that rarity that fit the spot (biome, time, weather and so on). UCP removes the ones above your tier.
3. If nothing is left, **that spawn attempt produces nothing**. It doesn't fall back to a different rarity.
4. Otherwise one Pokémon is picked, using Cobblemon's spawn weights adjusted by UCP's weight scaling (below).

Two consequences:

- At low tiers, a lot of rare and ultra-rare rolls come up empty, because most of what's in those buckets is still locked. Your rare odds get better as you unlock more, not because the odds change, but because more of each bucket becomes available.
- Overall spawn density can be a bit lower at low tiers in biomes where most local species are locked.

### Weight scaling: who's favored within a rarity

Once a rarity is rolled, UCP nudges which Pokémon in it gets picked, favoring Pokémon in your current tier:

```
weight multiplier = 2.0 − 0.2 × (tiers between the Pokémon and you), never below 0.15
```

| Pokémon's tier compared to yours | Multiplier |
|---|---|
| Same tier | × 2.0 |
| 1 below | × 1.8 |
| 2 below | × 1.6 |
| 3 below | × 1.4 |
| 4 below | × 1.2 |
| 5 below | × 1.0 |
| 6 below | × 0.8 |

So at tier 7, a tier-7 Pokémon is 2.5 times as likely to be picked as a tier-1 Pokémon with the same base weight (× 2.0 against × 0.8). With the default 7 tiers, the 0.15 minimum is never reached.

This only reorders Pokémon *within* the rolled rarity. It never makes a rare Pokémon common.

---

## 4. Wild Pokémon levels

UCP replaces Cobblemon's level for every wild Pokémon it spawns (Cobblemon normally picks levels close to your strongest party member). The new level depends on the Pokémon's tier and the tier of the **nearest player within 128 blocks**:

```
boosted cap = Pokémon's tier cap + 0.25 × (your tier cap − Pokémon's tier cap)
lowest      = 45% of boosted cap
typical     = 75% of boosted cap
highest     = 110% of boosted cap (never above 100)
```

Levels follow a triangle distribution: most land near the typical level and get less likely toward either end. Low-tier Pokémon get stronger as you progress, but always stay weaker than Pokémon from your own tier.

Level ranges with default settings (typical level in brackets):

| Your tier ↓ / Pokémon's tier → | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
|---|---|---|---|---|---|---|---|
| **1** | 6–16 (11) | | | | | | |
| **2** | 8–19 (14) | 12–29 (20) | | | | | |
| **3** | 9–23 (16) | 13–33 (23) | 18–44 (30) | | | | |
| **4** | 11–27 (19) | 15–37 (26) | 19–48 (33) | 24–59 (41) | | | |
| **5** | 13–31 (22) | 17–41 (29) | 21–51 (35) | 26–63 (44) | 31–75 (52) | | |
| **6** | 14–36 (25) | 18–46 (32) | 22–56 (38) | 27–68 (47) | 32–80 (55) | 38–93 (64) | |
| **7** | 16–39 (27) | 20–49 (34) | 24–60 (41) | 29–72 (50) | 34–84 (58) | 40–97 (67) | 45–100 (75) |

Wild Pokémon in your own tier can be up to 10% above your level cap. You can still catch them, but they won't gain XP until your cap rises.

---

## 5. Level cap on your Pokémon

Your Pokémon can't gain experience past your current tier's level cap:

- **At or above the cap:** XP from battles, candies and every other source is blocked. You'll see a short message on your action bar.
- **Close to the cap:** XP that would overshoot is trimmed so the Pokémon stops exactly at the cap.
- **Candies:** using an XP candy or Rare Candy on a Pokémon at the cap is blocked, and the candy isn't used up. A candy that would overshoot the cap is used up but only gives XP up to the cap.
- **Pokémon already above the cap** (caught wild, or traded in) keep their level. They just can't gain more XP.

Because Pokémon can't level past the cap, **level-up evolutions above your cap have to wait** until you reach a higher tier. Evolutions by item, trade or friendship aren't affected.

---

## 6. Configuration

UCP creates two files in `config/ultimate-cobblemon-progression/`. If you have [Mod Menu](https://modrinth.com/mod/modmenu), you can also edit them in-game.

On a dedicated server, edit the **server's** files and restart it; the in-game screen only changes your own client's copy. In single player, changes from the in-game screen apply right away.

### `ucp-spawn.json`

| Option | Default | What it does |
|---|---|---|
| `doSpeciesBlocking` | `true` | Block wild Pokémon above the player's tier. |
| `blockUnknownSpecies` | `true` | Block species that aren't listed in any tier. |
| `doLevelScaling` | `true` | Replace wild levels with UCP's tier-based levels ([section 4](#4-wild-pokémon-levels)). |
| `tierCapScaling` | `0.25` | How much of the gap between your cap and a lower-tier Pokémon's cap is added to its level. `0` = low-tier Pokémon never get stronger; `1` = they scale fully to your cap. |
| `minLevelScaling` | `0.45` | Lowest wild level, as a fraction of the boosted cap. |
| `avgLevelScaling` | `0.75` | Most likely wild level, as a fraction of the boosted cap. |
| `maxLevelScaling` | `1.1` | Highest wild level, as a fraction of the boosted cap (still never above 100). |
| `doWeightScaling` | `true` | Apply weight scaling within each rarity ([section 3](#weight-scaling-whos-favored-within-a-rarity)). |
| `weightCurrentTierBuff` | `2.0` | Multiplier for Pokémon in your own tier. |
| `weightDecayPerTier` | `0.20` | How much the multiplier drops per tier of difference. |
| `weightMinFactor` | `0.15` | The multiplier never goes below this. |

### `ucp-common.json`

| Option | Default | What it does |
|---|---|---|
| `doLevelCap` | `true` | Stop your Pokémon gaining XP past your tier's level cap ([section 5](#5-level-cap-on-your-pokémon)). |
| `doDexProgression` | `true` | Level players up automatically from Pokédex progress. If off, trainer levels only change through commands. |

---

## 7. Custom tiers (datapacks)

Tiers are loaded from `data/ultimate-cobblemon-progression/tiers/tier_<number>.json`. To replace a default tier, put a file with the same path in a datapack; to add a tier, add the next number.

```json
{
  "levelCap": 40,
  "requirements": {
    "dex": {
      "seen": 80,
      "caught": 20
    }
  },
  "species": [
    "cobblemon:pidgeotto",
    "cobblemon:mankey"
  ]
}
```

| Field | Required | Meaning |
|---|---|---|
| `levelCap` | Yes | The level cap for players at this tier, and the base for wild levels of this tier's species. |
| `requirements.dex.seen` | No | Species seen needed to **reach** this tier. |
| `requirements.dex.caught` | No | Species caught needed to **reach** this tier. |
| `species` | No | Species ids in this tier. |

Rules and tips:

- **Number tiers 1, 2, 3, … with no gaps.** The highest number is the maximum trainer level.
- **Use species ids, not form ids.** `cobblemon:vulpix` covers every Vulpix form. Entries like `cobblemon:bergmite_hisui` don't match anything and are ignored.
- **List each species in only one tier.**
- **A tier with no requirements stops automatic progression** at the tier before it. Players then need `/trainerlevel promote` or `/trainerlevel set` to go further.
- Species from add-ons only take effect when that add-on is installed; unknown ids are ignored.
- `/reload` reapplies tier files without a restart.

If you change the tier files, you can regenerate [Species by Tier and Rarity](species-by-tier.md) with `scripts/generate-species-reference.ps1` (see the script for usage).

---

## 8. Commands

| Command | Permission | What it does |
|---|---|---|
| `/trainerlevel get` | Everyone | Shows your trainer level and your Pokédex progress toward the next one. |
| `/trainerlevel set <player> <level>` | Operator (level 2) | Sets a player's trainer level. Values are clamped between 1 and the highest tier. |
| `/trainerlevel promote <player>` | Operator (level 2) | Raises a player one level and shows them the normal level-up message. |
