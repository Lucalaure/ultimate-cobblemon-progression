# How Cobblemon Journey Progression Works

This guide explains exactly what CJP changes in Cobblemon, using the mod's default settings and tier data. Every number here comes from the code and the shipped data files; the full species list is in [Species by Tier and Rarity](species-by-tier.md).

## The short version

- Every player has a **trainer level** from 1 to 7. Each level is a **tier**.
- Each Pokémon species belongs to one tier, based on its strength (Base Stat Total). **Wild Pokémon above your tier can't spawn for you.**
- **Rarity is Cobblemon's, not CJP's.** Whether a Pokémon is common or ultra-rare is decided by Cobblemon's spawn data. CJP never changes those odds. A Pokémon can be low-tier and ultra-rare (Dratini), or high-tier and common (Slaking).
- Wild Pokémon **levels** are rerolled based on the Pokémon's tier and yours.
- Your own Pokémon **can't gain XP past your tier's level cap**.
- You **level up automatically** by seeing and catching enough different species in your Pokédex.
- **Region mode** (optional) replaces the single ladder with one ladder per region, unlocked by gym badges. When you finish a region's league, you **choose which region your journey goes to next**, and wild Pokémon from the regions you've finished keep spawning alongside the new region's. See [section 9](#9-region-mode).
- **Gyms** appear in villages. Using a gym's Gym Challenge block summons your next gym leader. See [Gyms](#gyms).
- Every player starts with a **Trainer Card** that shows their progress. See [Trainer Card](#trainer-card).

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

Each time Cobblemon tries to spawn a wild Pokémon near you, CJP checks the species:

| Species tier compared to yours | Result |
|---|---|
| At or below your tier | Can spawn |
| Above your tier | Blocked |
| Not listed in any tier | Blocked by default (see `blockUnknownSpecies` in [Configuration](#6-configuration)) |

This applies to every kind of Cobblemon spawn: normal world spawns, fishing, PokeSnacks and bait.

**A Pokémon's forms share its tier**, except in region mode: there, Alolan, Galarian, Hisuian and Paldean forms belong to **their own region**, at their own tier (see [Regional forms](#regional-forms)).

**Which player counts.** Cobblemon runs spawning separately around each player. The check uses the player whose spawner made the attempt. If two players are together, Pokémon spawned by the higher-tier player's spawner can appear near both of them. If a spawn isn't tied to a player, the nearest player within 128 blocks counts; if nobody is that close, tiered species are blocked.

**Herds.** Many Pokémon spawn in herds, and some only in herds led by their evolution: Toxel's herds are led by Toxtricity, Riolu's by Lucario, Pichu's by Pikachu. Herd members you can't meet yet are left out, and the herd forms from the rest: until Toxtricity unlocks for you, a Toxel herd is all Toxel.

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

These are Cobblemon's defaults (server admins can change them in `config/cobblemon/spawning/best-spawner-config.json`). **CJP doesn't change them.**

### Tier is not rarity

CJP's tiers measure strength, not rarity. The two are completely independent, which leads to some patterns worth knowing:

- **Pseudo-legendary first stages are tier 1 but hard to find.** Dratini, Larvitar, Bagon, Beldum, Gible, Deino, Goomy, Jangmo-o and Dreepy can spawn from the start, but mostly as rare or ultra-rare (Dratini and Goomy also show up as common in a few spots). Their final forms are tier 7.
- **Starters are tier 2 and ultra-rare.** Every first-stage starter (Bulbasaur, Charmander, Squirtle and the rest), plus the fossils Cranidos and Shieldon, is in tier 2's ultra-rare bucket. The starters' final evolutions are tier 6, also ultra-rare.
- **Tier 7 has very few wild Pokémon.** Only Slaking and the pseudo-legendary final forms (Dragonite, Tyranitar, Salamence, Metagross, Garchomp, Hydreigon, Goodra, Kommo-o, Dragapult, Archaludon) spawn naturally, and most of those mainly as rare or ultra-rare.
- **Strong doesn't mean rare.** Gyarados, Machamp and Gardevoir are tier 6 but common.

For every species' tier and rarity, see [Species by Tier and Rarity](species-by-tier.md).

### How tiers and rarity interact

1. Cobblemon rolls a rarity using the odds above. CJP doesn't affect this roll.
2. Cobblemon collects the Pokémon in that rarity that fit the spot (biome, time, weather and so on). CJP removes the ones above your tier.
3. If nothing is left, **that spawn attempt produces nothing**. It doesn't fall back to a different rarity.
4. Otherwise one Pokémon is picked using Cobblemon's spawn weights. In region mode, the [spawn pools](#wild-pokémon-in-a-new-region-spawn-pools) adjust them; the optional weight scaling below can too.

Two consequences:

- At low tiers, a lot of rare and ultra-rare rolls come up empty, because most of what's in those buckets is still locked. Your rare odds get better as you unlock more, not because the odds change, but because more of each bucket becomes available.
- Overall spawn density can be a bit lower at low tiers in biomes where most local species are locked.

### Weight scaling: who's favored within a rarity

**Off by default** (`doWeightScaling` in [`ucp-spawn.json`](#ucp-spawnjson)): unlocking a tier only adds Pokémon, and the odds of the ones you already had stay as Cobblemon set them. When it's on, once a rarity is rolled, CJP nudges which Pokémon in it gets picked, favoring Pokémon in your current tier:

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

CJP replaces Cobblemon's level for every wild Pokémon it spawns (Cobblemon normally picks levels close to your strongest party member). The new level depends on the Pokémon's tier and the tier of the **nearest player within 128 blocks**:

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

CJP creates two files in `config/ultimate-cobblemon-progression/`. If you have [Mod Menu](https://modrinth.com/mod/modmenu), you can also edit most options in-game; the spawn pool options (`doRegionPools` and the three pool weights) are only in the file. CJP writes each file back when it loads it, so options added in a newer version appear with their defaults, and your own values are kept.

On a dedicated server, edit the **server's** files and restart it; the in-game screen only changes your own client's copy. In single player, changes from the in-game screen apply right away.

### `ucp-spawn.json`

| Option | Default | What it does |
|---|---|---|
| `doSpeciesBlocking` | `true` | Block wild Pokémon above the player's tier. |
| `blockUnknownSpecies` | `true` | Block species that aren't listed in any tier. |
| `blockUnlistedTrainers` | `true` | Region mode: Radical Cobblemon Trainers' wild trainers that no region's `wild_trainers.json` lists never spawn ([Wild trainers](#wild-trainers)). Off: they spawn for everyone, as without CJP. |
| `doLevelScaling` | `true` | Replace wild levels with CJP's tier-based levels ([section 4](#4-wild-pokémon-levels)). |
| `tierCapScaling` | `0.25` | How much of the gap between your cap and a lower-tier Pokémon's cap is added to its level. `0` = low-tier Pokémon never get stronger; `1` = they scale fully to your cap. |
| `minLevelScaling` | `0.45` | Lowest wild level, as a fraction of the boosted cap. |
| `avgLevelScaling` | `0.75` | Most likely wild level, as a fraction of the boosted cap. |
| `maxLevelScaling` | `1.1` | Highest wild level, as a fraction of the boosted cap (still never above 100). |
| `doWeightScaling` | `false` | Apply weight scaling within each rarity ([section 3](#weight-scaling-whos-favored-within-a-rarity)). Off, unlocking a tier only adds species. |
| `weightCurrentTierBuff` | `2.0` | Multiplier for Pokémon in your own tier. |
| `weightDecayPerTier` | `0.20` | How much the multiplier drops per tier of difference. |
| `weightMinFactor` | `0.15` | The multiplier never goes below this. |
| `doRegionPools` | `true` | Region mode: while you work through a region, spawn from the three [spawn pools](#wild-pokémon-in-a-new-region-spawn-pools). Off: every species uses its own region's levels and weights. |
| `regionPoolWeight` | `1.0` | Weight of the region in progress. |
| `pastInCapPoolWeight` | `0.5` | Weight of finished regions' species within your cap, at the current region's levels. |
| `pastFullPoolWeight` | `0.15` | Weight of finished regions' species at their full levels. |

### `ucp-common.json`

| Option | Default | What it does |
|---|---|---|
| `doLevelCap` | `true` | Stop your Pokémon gaining XP past your tier's level cap ([section 5](#5-level-cap-on-your-pokémon)). |
| `doDexProgression` | `true` | Level players up automatically from Pokédex progress. If off, trainer levels only change through commands. |
| `regionStarters` | `true` | Region mode: you pick your starter after choosing where your journey starts, and only from that region's starters (plus your second region's, if you picked it first). Choosing where your journey starts opens the starter choice. |
| `starterLevel` | `5` | The level starters come at, as in the original games, with the moves they know at that level. `0` keeps the level in Cobblemon's starter list (10). |
| `giveTrainerCard` | `true` | Give every player a [Trainer Card](#trainer-card) on their first join. |
| `gymVillageChance` | `0.3` | Share of newly generated villages (0 to 0.95) whose centre is a [gym](#gyms). `0` turns village gyms off. Takes effect the next time a world starts. |
| `gymTeamLimit` | `false` | Difficulty option: you can bring no more Pokémon to a [gym](#gyms) battle than the leader has. |
| `gymLegalityCheck` | `"off"` | Whether the Pokémon you bring to a [gym](#gyms) battle must be ones you could have by now (see `gymLegalityRule`). `"off"`: no check. `"warn"`: the leader names any that aren't when it accepts your challenge. `"block"`: the battle doesn't start while one is in your party. Your starter is always allowed. |
| `gymLegalityRule` | `"region"` | What `gymLegalityCheck` allows. `"region"`: any Pokémon of a region you've opened, so the evolutions of what you catch are fine; a regional form counts as its own region's (an Alolan Raichu needs Alola). `"strict"`: only Pokémon of tiers you've unlocked, the ones you can meet in the wild now. Both also need the Pokémon at or below your level cap. |
| `gymDifficulty` | `"player_choice"` | [Gym difficulty](#gym-difficulty): `"player_choice"` lets each player pick Normal or Hard leaders on their Trainer Card and change it any time; `"normal"` or `"hard"` sets it for everyone. |
| `allowTwoLeagues` | `true` | Region mode: players may pick another league, with its own gym leaders, as their second region and work through both side by side ([Two leagues at once](#two-leagues-at-once)). Off: the second region can only be one without gyms. Players who already picked a league keep it. |
| `twoLeagueCap` | `"average"` | Region mode, with [two leagues at once](#two-leagues-at-once): how the level cap follows both leagues. `"average"`: the average of the two leagues' caps, rounded up. `"lowest"`: the lower of the two, so you keep both leagues level. |
| `catchWholeTier` | `false` | Difficulty option (region mode): to reach a region's next tier, you also have to catch every obtainable Pokémon of your current tier there. Gyms won't summon a league's next leader until you have, and your extra starting region waits at its tier until it's caught. See [Catch every Pokémon](#catch-every-pokémon-optional). |

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
| `requirements.advancements` | No | Advancement ids that must all be completed to **reach** this tier, e.g. `["ultimate-cobblemon-progression:journey/kanto/boulder_badge"]`. An entry can also be a list of alternatives, where completing any one counts: `[["ultimate-cobblemon-progression:journey/kanto/boulder_badge", "ultimate-cobblemon-progression:journey/sinnoh/coal_badge"]]` needs the Boulder Badge **or** the Coal Badge. |
| `species` | No | Species ids in this tier. |

Rules and tips:

- **Number tiers 1, 2, 3, … with no gaps.** The highest number is the maximum trainer level.
- **Use species ids, not form ids.** `cobblemon:vulpix` covers every Vulpix form. Entries like `cobblemon:bergmite_hisui` don't match anything and are ignored.
- **List each species in only one tier.**
- **Advancement requirements** are re-checked the moment an advancement is earned, so tiers can be tied to anything that grants one: beating a trainer (CJP's `defeat_trainer` trigger, see [Advancements](#advancements-the-pokémon-journey-tab)), finishing a quest, or a custom datapack advancement.
- **A tier with no requirements stops automatic progression** at the tier before it. Players then need `/trainerlevel promote` or `/trainerlevel set` to go further.
- Species from add-ons only take effect when that add-on is installed; unknown ids are ignored.
- `/reload` reapplies tier files without a restart.

If you change the tier files, you can regenerate [Species by Tier and Rarity](species-by-tier.md) with `scripts/generate-species-reference.ps1` (see the script for usage).

---

## 8. Commands

| Command | Permission | What it does |
|---|---|---|
| `/trainerlevel get` | Everyone | Shows your trainer level and your Pokédex progress toward the next one. |
| `/trainerlevel card` | Everyone | Gives you a new [Trainer Card](#trainer-card) if you don't have one. |
| `/trainerlevel set <player> <level>` | Operator (level 2) | Sets a player's trainer level. Values are clamped between 1 and the highest tier. |
| `/trainerlevel promote <player>` | Operator (level 2) | Raises a player one level and shows them the normal level-up message. |

In region mode, `/trainerlevel get` instead lists every region: its tier and what's needed next, which one is your **current region**, and for a locked one why it's locked (e.g. "travel here after completing Kanto"). `set`/`promote` are disabled (use `/advancement grant` and `/advancement revoke`). Three more commands exist in region mode:

| Command | Permission | What it does |
|---|---|---|
| `/trainerlevel region next <region>` | Everyone | Picks the region your journey continues in, once your current region is complete. Same as picking it on the Trainer Card. |
| `/trainerlevel region choose <region>` | Everyone | Picks your extra starting region (once). Only regions that aren't open yet can be picked. |
| `/trainerlevel region reset <player>` | Operator (level 2) | Clears a player's extra starting region choice. It doesn't touch their home region or the regions they travelled to: those picks can't be undone by command. |
| `/trainerlevel difficulty [normal\|hard]` | Everyone | Shows your [gym difficulty](#gym-difficulty), or changes it (when the server lets players choose). Same as the Trainer Card's Road tab. |

---

## 9. Region mode

Region mode gives each region (Kanto, Johto, …) its **own ladder of tiers**, unlocked by advancements such as gym-leader wins. It turns on automatically when any region data is loaded; without region data, everything above works as described.

### How it plays

1. You pick the **region your journey starts in** (your home region), where only its weakest species can spawn.
2. Each **tier** of that region is unlocked by its requirements, typically beating the next gym leader. Earning the advancement unlocks the tier immediately and tells you in chat how many species it added.
3. When you earn the region's **completion advancement** (e.g. the Champion), the region is complete. Completed regions stay fully open.
4. You then **choose your next region** from the leagues you haven't visited yet, and your journey continues there from its first tier. See [Moving on to your next region](#moving-on-to-your-next-region).

Regions come in two kinds:

- **League regions** have a trainer `series` (Kanto, Sinnoh and Galar in the built-in data). They're the stops on your journey: one opens when you start in it or travel to it.
- **Other regions** (Johto, Hoenn, … in the built-in data) only add species. They don't open on their own once your journey has started: the only way to open one is to pick it as your [extra starting region](#picking-a-second-starting-region). Completing a region opens nothing but the region you choose next.

Species from regions you haven't reached, and species in no region at all, can't spawn (the second case follows `blockUnknownSpecies`). This only affects wild spawns: trades, eggs and evolutions aren't restricted.

### Regional forms

A regional form belongs to the region it comes from, at its own tier there, as if it were a species of that region. Its species' region doesn't matter:

- **Alolan Rattata spawns once Alola reaches its tier**, whether or not you've opened Kanto. Regular Rattata follows Kanto as usual. So in Alola without Kanto you only meet Alolan Rattata, and with both open you meet both, wherever Cobblemon spawns each.
- **Its levels and spawn weight come from its own region**, like any of that region's species.
- **It counts on its own** for [catching every Pokémon](#catch-every-pokémon-optional): Alolan Rattata is one of Alola's Pokémon to catch, and catching it doesn't count for Kanto's Rattata.

In the built-in data:

| Forms | Region |
|---|---|
| Alolan | Alola |
| Galarian | Galar |
| Hisuian | Sinnoh (its tiers include the Hisui dex) |
| Paldean (Wooper, Tauros) | Paldea |

Each form sits in the tier of its region that matches its own Base Stat Total, so Alolan Rattata is in Alola's tier 1 and Hisuian Typhlosion in Sinnoh's tier 8.

**Pokémon that always evolve into a regional form don't spawn in region mode.** Cobblemon spawns some Pikachu, Cubone, Cyndaquil, Goomy and others that look normal but evolve into a regional form wherever you are. Every regional form they lead to spawns in the wild itself, at about the same rarity, so you catch those instead. The normal Pikachu, Cubone and Cyndaquil still spawn.

**Evolving in the right biome still works.** A normal Pikachu or Exeggcute evolved in an Alola-style biome becomes the Alolan form, even before you've opened Alola. Evolutions are never restricted, so this is a way to reach a regional form early. It counts for that region once you open it.

Forms no region claims, like Cobblemon's Valencian forms, follow their species.

A region claims forms with `formAspects` in its `region.json`, and lists them in its tier files (see [Region files](#region-files)).

Progress is worked out from your advancements (and Pokédex, if a tier asks for it) each time it changes, so a trainer mod resetting its own series progress never re-locks a region. CJP only stores the regions you picked.

### Differences from the tier ladder

- **Wild levels** work as in [section 4](#4-wild-pokémon-levels), but each species is compared with your progress **in its own region**, using that region's tier caps. While you work through a region, species from the regions you've finished also spawn at the current region's levels, and the [spawn pools](#wild-pokémon-in-a-new-region-spawn-pools) decide how often each kind appears.
- **CJP's level cap is off.** Region mode is meant to be paired with a trainer mod that has its own cap, such as Radical Cobblemon Trainers.
- **Trainer levels and Pokédex auto-levelling aren't used.**

### Region files

Each region is a folder in `data/ultimate-cobblemon-progression/regions/<region>/`:

```
regions/kanto/region.json
regions/kanto/tier_1.json
regions/kanto/tier_2.json
...
```

`region.json`:

```json
{
  "order": 1,
  "title": "Kanto",
  "completedBy": "ultimate-cobblemon-progression:journey/kanto/champion",
  "series": "kanto"
}
```

| Field | Required | Meaning |
|---|---|---|
| `order` | No (default 0) | Unlock order. Before a player has a journey (or when no region has a `series`), the lowest order is open from the start and each later region opens when the one before it is complete. With a journey, only the regions on it (and the extra starting region) are open, and `order` just sorts the Trainer Card's lists. |
| `title` | No | Name shown in chat and `/trainerlevel get`. Defaults to the folder name. |
| `completedBy` | No | Advancement that completes the region. Without it, the region is complete once its last tier is unlocked. |
| `startChoice` | No (default false) | Lets players pick this region as their extra starting region (see below). Ignored on the first region, which is always open, and on regions with a `series`. |
| `series` | No | The Radical Cobblemon Trainers series for this region's trainers, e.g. `"kanto"`. A region with a series is a **league region**: it can be picked as where a player's journey starts, or where it goes next (see below). |
| `formAspects` | No | Cobblemon aspects that mark a Pokémon as this region's [regional form](#regional-forms), e.g. `["alolan", "region-bias-alola"]`. A form with one of them belongs to this region. A form the tier files don't list takes its species' tier number here (capped at the last tier). `region-bias-*` aspects mark Pokémon that always evolve into this region's forms, which never spawn in region mode. If two regions list the same aspect, the earlier one keeps it. |

A region can also have a `wild_trainers.json`, listing the Radical Cobblemon Trainers trainers that spawn on their own in that region (see [Wild trainers](#wild-trainers)). Each entry is one trainer: an RCT trainer id, or a list of ids for the versions of the same trainer (its rematches, or a rival's copies for each starter), which the Trainer Card counts once:

```json
{
  "trainers": [
    ["youngster_ben_0059", "youngster_ben_0065", "youngster_ben_01f2", "youngster_ben_01f3"],
    "youngster_calvin_005a"
  ]
}
```

### Wild trainers

Besides the leaders CJP's gyms summon, Radical Cobblemon Trainers spawns ordinary trainers around players. In region mode:

- **A wild trainer only spawns for you if its region is open for you**: your journey's regions and your extra starting region, finished ones included. None spawn before you pick where your journey starts.
- **Trainers no region lists never spawn** (the built-in data leaves out Pokémon Unbound's, from Borrius). Turn that off with `blockUnlistedTrainers` in `ucp-spawn.json`.
- RCT still decides everything else: how often trainers spawn, where, and which ones match your team's level.
- Trainers summoned by commands, gyms or spawner blocks aren't affected.

The built-in lists hold each original game's own trainers: **447 for Kanto**, FireRed's, and **483 for Sinnoh**, Brilliant Diamond / Shining Pearl's. Many trainers come in several versions: rematches at higher levels, from the games' Vs. Seeker, and a rival's three copies of each battle, one per starter. Every version still spawns, matched to your team's level like any trainer, so familiar trainers keep turning up as you get stronger, but each trainer counts once. Grunts are each their own battle.

RCT's Kanto trainers come from Radical Red, a FireRed hack that keeps FireRed's trainers but rebuilds their teams (harder, at higher levels, about a quarter of their Pokémon from later games). CJP gives them back **FireRed's own teams**, from [pret's FireRed decompilation](https://github.com/pret/pokefirered): FireRed's species, levels, IVs, held items and moves, including the moves FireRed works out from its own learnsets for Pokémon without custom ones, and FireRed's double battles. Sinnoh's teams are already Brilliant Diamond / Shining Pearl's. Left out:

- Radical Red's own additions, which aren't in FireRed: the Friendly Coaches and other tutorial trainers, its joke trainers, its post-game cameos like Brendan, May and Red, and the trainers it put in slots FireRed never uses (empty placeholders, and nameless leftovers from the first games: "Ace Trainer Janny" sits in one, with its three Magnemite). A few FireRed trainers that Radical Red gave made-up classes keep their FireRed name: "Dragon Tamer Ramiro" is Bird Keeper Ramiro again.
- Trainers that can never battle a CJP player: those that need other story trainers beaten first, like Radical Red's rival battles (RCT only counts those wins inside its own series).

**Galar** has **147 trainers** (221 battles) from Pokémon Sword & Shield, base game and both DLCs. RCT has none, so CJP brings its own, copied from [Serebii](https://serebii.net/pokearth/galar/): each Pokémon's species, form (Galarian forms included), level, moves and held item, and the items the trainer uses. Gender, ability and nature aren't listed there, so they come from Cobblemon's species data. They spawn once Galar is open for you, like any wild trainer.

- Left out: gym leaders, the Champion, Chairman Rose, Mustard and Honey, the Stadium battles and Max Raid partners. Rivals and story trainers stay: Hop (his 42 battles count as one trainer), Bede, Marnie, Klara, Avery, Sordward, Shielbert, Peony, Oleana and Team Yell.
- Each Galar trainer class uses the closest class RCT already has, which decides where it spawns, its loot and its skin: a Gym Trainer counts as an Ace Trainer, a Backpacker as a Hiker, Team Yell as Bikers. Skin packs for RCT, like RCT Trainer Textures Plus, cover them. No Galar-specific skins exist.
- Nobody Dynamaxes: in Sword & Shield that only happens at Power Spots, and route trainers never do. Hop's post-game Zacian and Zamazenta use their Hero form, since the Crowned form needs the Rusted Sword or Shield.
- Some classes only spawn in particular places: Macro Cosmos staff and Doctors count as Scientists, which RCT spawns around structures like end cities and ancient cities, so they're rare.

The **Trainers** tab of your [Trainer Card](#trainer-card) lists every wild trainer of your open regions, weakest first, and marks the ones you've beaten. CJP keeps that record itself, starting from the update that added the tab, so **wins from before it don't count**. RCT respawns ordinary trainers you've beaten, so you can meet them again and count them then. In a battle with several players on your side, the win counts for each of them. The built-in lists are generated by `scripts/generate-wild-trainers.ps1`.

### Choosing where your journey starts

The first time a player opens their [Trainer Card](#trainer-card), it asks **which region their journey starts in**, before showing anything else. Every region with a `series` is offered (Kanto, Sinnoh and Galar in the built-in data), with its number of trainers, species and first leader. Picking one (after confirming, since it's permanent):

- opens that region from the start, in place of the first region;
- puts the player into its Radical Cobblemon Trainers series, so its leaders will battle them (if they're already in that series, nothing is reset).

Until a player has picked, gyms ask them to open their card first. If no region has a `series`, there's nothing to pick and the first region is open from the start, as before.

### Two leagues at once

With `allowTwoLeagues` on, your second region can be another league, so you work through two leagues side by side (any two of Kanto, Sinnoh and Galar in the built-in data). The second league works exactly like your home one:

- **Its gym leaders unlock its tiers**, in its own order. With [Catch Every Pokémon](#catch-every-pokémon-optional) on, each league needs its own current tier caught before its next leader, and one league waiting never holds the other back.
- **Gyms let you choose.** When both leagues have a leader ready, using a Gym Challenge block opens a menu with each league's leader, the level of their strongest Pokémon, and the difficulty, and you pick who to battle. When only one is ready, it's summoned straight away, and the block tells you why the other isn't (e.g. Pokémon still to catch).
- **The two leagues are a pair.** If one is complete first, you carry on with the other. Once both are done, you choose where your journey goes next, as in [Moving on to your next region](#moving-on-to-your-next-region), and can pick a second league again. Your level cap then starts over with the new region, as it does for a single league, and the leagues you finished stay open as past regions.
- **Your Trainer Card** shows both: the Profile tab lists both regions and each league's next leader, and the Road tab has a button to switch between the two roads.

**Level cap.** Radical Cobblemon Trainers gives each league its own cap (the level of its next leader's strongest Pokémon), but a Pokémon can't belong to one league, so you get one cap: **the average of the two, rounded up**. It rises whichever league you play, and rises most when you advance both:

| Kanto / Sinnoh badges | Kanto's next leader | Sinnoh's next leader | Your cap |
|---|---|---|---|
| 0 / 0 | 14 | 14 | 14 |
| 2 / 2 | 24 | 27 | 26 |
| 4 / 4 | 36 | 36 | 36 |
| 6 / 0 | 47 | 14 | 31 |

So you can rush one league, but its leaders get ahead of your cap. Servers that want both leagues kept level can set `twoLeagueCap` to `"lowest"`: the cap is then the lower of the two, so you can't train past the league you're behind in. Once one league is complete, your cap is the other league's own, and once both are, it's 100 until you pick your next region.

**Starting a second league partway through never lowers your cap.** CJP remembers the cap you had with one league, and you keep it until the average catches up. After that it's the plain average, and the remembered cap is gone for good. It also covers a region you'd picked becoming a league in an update (Galar in this version).

CJP hands this cap to RCT, so RCT's screens, its experience limit and its wild trainer spawns all use it. Players with one league keep RCT's own cap. When you complete the league that isn't your RCT series, CJP also records it as a completed series in RCT, so it counts for RCT's loot luck bonus like your home league.

RCT keeps one trainer series per player: you stay in your home league's series. A leader a gym summons for you battles you whatever your series, since CJP has already checked the order. As with RCT's own rule, leaders won't battle you while a party Pokémon is above your level cap: the Gym Challenge block tells you which one.

### Moving on to your next region

When every league you're working through is complete (with [two leagues](#two-leagues-at-once), both of them) and there's a league region you haven't been to, your journey moves on: chat tells you to choose, and the next time you open your [Trainer Card](#trainer-card) it asks **where your journey goes next**, with each region's trainers, species and first leader. **Not now** shows the card first; it asks again next time. The same pick is also a command:

```
/trainerlevel region next <region>
```

Picking one (after confirming, since it's permanent):

- opens that region at its first tier, and its gyms summon its leaders from then on;
- puts you into its Radical Cobblemon Trainers series. RCT gives each series its own level cap, so your cap **starts over** with the new region's first leader (15 in the built-in leagues), and RCT's trainers won't battle you while a party Pokémon is above it (unless the server sets RCT's `allowOverLeveling`). Your old team is still yours, but a new region means training a new one, much like a new game.

Only the region you pick opens. Everything you'd already opened stays open, and its Pokémon keep spawning (see [spawn pools](#wild-pokémon-in-a-new-region-spawn-pools)). Until you pick, gyms and `/trainerlevel get` tell you to choose, and you're reminded when you join. When you've completed every league region, your journey is complete and chat congratulates you.

The pick is permanent and there's no command to undo it, since it also switches your Radical Cobblemon Trainers series. On the Trainer Card, the Road and trainer counts for a finished region still show its trainers as beaten after you switch series, because CJP counts every trainer of an unlocked tier as beaten.

Since CJP decides your series in region mode, RCT's **Trainer Association** NPC doesn't spawn while region mode is on: it would let you join another series, or switch to RCT's freeroam (level cap 100) after finishing a league, and skip past your journey. Without region mode, RCT's own `spawnTrainerAssociation` setting applies.

### Catch every Pokémon (optional)

With `catchWholeTier` on in `ucp-common.json` (or **Catch Every Pokémon to Advance** in the Mod Menu screen), moving on in a league takes two things: beating the tier's gym leader **and** having caught every Pokémon of the tier you're on. In Kanto, tier 2 needs Brock beaten and all 16 tier-1 Pokémon caught; tier 3 needs Misty and every tier-2 Pokémon; and so on.

- **Gyms wait for you.** Until the current tier is fully caught, a Gym Challenge block doesn't summon the next leader and tells you what's left ("Still to catch (3): Pidgey, Rattata and Zubat"), so your RCT level cap can't race ahead of your tier.
- **What counts as caught:** the Pokédex's caught mark. Catching, evolving and trading all count, and so do Pokémon you caught before turning the option on. [Regional forms](#regional-forms) count on their own: Alola needs an Alolan Rattata, and Kanto needs a regular one.
- **Only obtainable Pokémon count**: ones that spawn in the wild (including spawns added by datapacks) or evolve from one that does. Pokémon you can't get, like most legendaries, are skipped, so no tier can get stuck. In the built-in data that skips Rotom (Sinnoh tier 4), Phione (tier 5), Azelf, Mesprit, Cresselia and Enamorus (tier 9), and the legendaries of both leagues' tier 10.
- **The last tier isn't needed.** Becoming Champion only takes beating the Champion, since there's no tier after it.
- **Your extra starting region follows the same rule, without holding the league back.** Its next tier opens once you have the matching badge *and* have caught all of its current tier. Gyms never wait on it: you can keep beating leaders, and the extra region simply stays behind until you catch its Pokémon (then catches up at once if you already have the badges). The Trainer Card's Regions tab shows how many it still needs ("Tier 1 / 10 · 10 to catch").
- Your progress shows on the Trainer Card ("First catch every tier 1 Pokémon. 3 to go (16 in all): …"), in `/trainerlevel get` ("Next: catch Pidgey, Rattata and Zubat"), and on the card's Wild Pokémon tab, which marks what you've caught. Chat tells you when a tier is fully caught, in your league or your extra region.

**You never lose a tier you've reached.** CJP remembers the highest tier you've reached in each region, and the catch rule only holds back the tiers above it. Turning the option on in a world that's underway, or a datapack adding Pokémon to a tier you've passed, doesn't take a tier away. Badges and other requirements still apply as usual.

### Wild Pokémon in a new region: spawn pools

Unlocking a new region only ever adds Pokémon: the regions you've finished stay fully open. While you work through a region, wild Pokémon come from three **spawn pools**:

1. **The new region**: its species up to your tier there, at its levels ([section 4](#4-wild-pokémon-levels), measured against your progress in that region). The most common pool. Your [extra starting region](#picking-a-second-starting-region), if it isn't complete, counts as this pool too.
2. **Finished regions, within your cap**: species of completed regions whose tier is at or below your tier in the new region, at the **new region's** levels. A Kanto tier-2 Pokémon spawns at Sinnoh tier-2 levels, so you can catch Pokémon for your new team from every region you've finished.
3. **Finished regions, at full strength**: every species of completed regions, at their own fully unlocked levels. The rarest pool.

A species of a finished region within your cap is in pools 2 and 3: its weight is the sum of both, and each one that spawns gets its level from one of them, picked in proportion to their weights. Each pool's weight multiplies Cobblemon's spawn weight (and the tier weighting, if you turn [weight scaling](#weight-scaling-whos-favored-within-a-rarity) on); set them with `regionPoolWeight`, `pastInCapPoolWeight` and `pastFullPoolWeight` in [`ucp-spawn.json`](#ucp-spawnjson). Between regions (after a Champion, before you pick) and once every league is complete, there are no pools: everything spawns at its own region's levels and weights.

Right after a Kanto Champion travels to Sinnoh, with the built-in data and default settings:

| Wild Pokémon | Pools | Level range (typical) | Weight |
|---|---|---|---|
| Sinnoh tier 1 (Starly, Shinx, …) | new region | 6–16 (11) | × 1.0 |
| Sinnoh tier 2 and up | | can't spawn yet | |
| Kanto tier 1 (Pidgey, Rattata, …) | within cap (77%) / full (23%) | 6–16 (11) / 16–39 (27) | × 0.65 |
| Kanto tier 2 (Geodude, …) | full only, until Sinnoh tier 2 | 18–45 (31) | × 0.15 |
| Kanto tier 10 (Dragonite, Snorlax, …) | full only, until Sinnoh tier 10 | 45–100 (75) | × 0.15 |
| Johto to Paldea | | can't spawn (not on your journey) | |

After Roark (Sinnoh tier 2, cap 22), Sinnoh's tier 1 rises to 7–18 and its tier 2 opens; Kanto's tier 1 rises with it (7–18) and Kanto's tier 2 joins the within-cap pool at 9–24. Each badge after that does the same for the next tier, until Kanto's tier 10 joins the within-cap pool at Sinnoh tier 10. "Within your cap" compares tier numbers (Kanto tier 3 joins at Sinnoh tier 3), which lines up because both leagues have 10 tiers. Weights only compare species that can spawn at the same spot and rarity, so how often you meet each pool also depends on how many of its species live in that biome. Cobblemon still decides where each species spawns (biome, time, rarity), so Pokémon from every open region mix in the same world. Wild levels use the nearest player within 128 blocks.

### Picking a second starting region

Each player can open **one** second region alongside their home region: either a region marked `"startChoice": true` (Johto, Hoenn, Unova, Kalos, Alola, Galar and Paldea in the built-in data), or, with `allowTwoLeagues` on (the default), **another league** with its own gym leaders (see [Two leagues at once](#two-leagues-at-once)). That makes two regions at most. It's the only way to open a region without a `series`. Only regions that aren't open yet are offered, a league only once you've picked where your journey starts, and it can be picked at any time, not just at the start. The easiest way is the [Trainer Card](#trainer-card): its Regions tab shows a **Pick** button on every starting choice (the tab is marked **!** until you've picked), and asks you to confirm before anything changes. The same pick is also a command:

```
/trainerlevel region choose <region>
```

Players are reminded on joining and in `/trainerlevel get` until they pick. The choice is permanent; an operator can clear it with `/trainerlevel region reset <player>`.

Picking a region without gyms only opens it. Its tiers still unlock through their own requirements, so a chosen region grows with whatever its tier files ask for. The built-in data ties the other regions to the badges of whichever league you're in, through hidden [badge milestones](#badge-milestones): tier 2 needs your 1st badge in any league, tier 3 your 2nd, …, tier 10 becoming Champion of any league. While it's unfinished, the extra region's Pokémon spawn as part of the [new region pool](#wild-pokémon-in-a-new-region-spawn-pools).

`tier_N.json` files use the same format as [custom tiers](#7-custom-tiers-datapacks): `levelCap`, `species`, and `requirements` (usually `advancements`), plus an optional `trainers` list for [gyms](#gyms). Tiers must be numbered 1, 2, 3… with no gaps, and tier 1 normally has no requirements. A species belongs to the first region that lists it, and so do its forms, except [regional forms](#regional-forms). A regional form is listed as the species id and its aspect, e.g. `"cobblemon:rattata alolan"`, with more aspects when several forms share one (`"cobblemon:tauros paldean blaze-breed"`).

`scripts/generate-region-tiers.py` (Python 3) builds the tier files from Cobblemon's regional Pokédexes, splitting the species into equal-sized tiers by Base Stat Total, weakest first. It keeps each tier's `trainers` list, and its level cap and requirements unless you pass new ones, so re-sorting a region's species is one command:

```
python scripts/generate-region-tiers.py --dex cobblemon:johto --out-dir <path to regions/johto>
```

`scripts/add-regional-forms.py` then adds every regional form to its region's tier files, in the tier that matches its Base Stat Total, without moving any species. Run it after `generate-region-tiers.py`, which rebuilds the species lists from scratch.

The built-in regions' requirements are set by `scripts/generate-league-advancements.py` (see [Advancements](#advancements-the-pokémon-journey-tab)), so after re-sorting one you don't need to pass any. For a new region, pass `--level-caps` (one per tier) and `--advancements` (one entry per tier, separated by `;`, empty for no requirement, with `|` between alternatives). The built-in regions came from these dexes: each region's own (e.g. `cobblemon:kanto`), and `cobblemon:sinnoh,cobblemon:hisui` for Sinnoh. See the top of the script for every option.

### The built-in leagues

When Radical Cobblemon Trainers is installed, CJP turns on a built-in datapack, **CJP Kanto, Sinnoh & Galar Leagues**, in every world. It contains:

- a 10-tier **Kanto**, with each tier after the first unlocked by one of its gym leaders, and the Champion completing it;
- Kanto's RCT trainers: the 8 leaders (Brock, Misty, Lt. Surge, Erika, Koga, Sabrina, Blaine and Giovanni), the Elite Four (either Agatha counts) and the Champion, with FireRed/LeafGreen-style teams (Koga's are 7 levels lower, so the level cap rises in two steps between Erika and Sabrina instead of one), in a `kanto` series, and their natural spawning turned off so they only appear in gyms;
- a 10-tier **Sinnoh**, built the same way from RCT's own Brilliant Diamond/Shining Pearl trainers: Roark, Gardenia, Maylene, Wake, Fantina, Byron, Candice and Volkner, then Aaron, Bertha, Flint, Lucian and Champion Cynthia, in a `sinnoh` series. Their teams are RCT's (aces from level 14 to 66), except that Maylene's are 3 levels lower and Wake's 1 level higher, so the level cap doesn't stay flat between them, and each one requires the one before it, so RCT's level cap follows the league (15 at the start, then each next trainer's level). Each tier's cap is the level of the next trainer's strongest Pokémon, as in Kanto;
- a 10-tier **Galar**, with CJP's own trainers, since RCT has none from Sword and Shield: Gym Leaders Milo, Nessa, Kabu, Bea and Allister, Opal, Gordie and Melony, Piers and Raihan, then the Champion Cup (Marnie, Hop and Bede) and Champion Leon, in a `galar` series. Gyms 4 and 6 have a different leader in Sword and in Shield; Galar has both, so you beat Bea and then Allister to pass gym 4, and Gordie and then Melony to pass gym 6, each for their own badge (10 in all). Their teams, levels and moves are Sword's and Shield's gym and Champion Cup battles, except that Bea's and Allister's teams are 4 levels lower so the cap rises in steps from Kabu to Opal, with IVs, potions and the sharper AI as for Kanto's Normal leaders. Each also has a Hard version, built like Kanto's: one more Pokémon, all at the Normal ace's level, with competitive sets. RCT has no textures for them, so they look like RCT's default trainer unless a resource pack adds some. With [Mega Showdown](#dynamax-and-gigantamax-mega-showdown) installed, each Dynamaxes its ace as in the games (Leon's Charizard Gigantamaxes; Piers never Dynamaxes). `scripts/generate-galar-league.py` holds the teams and writes every file;
- every other region (Johto, Hoenn, Unova, Kalos, Alola, Paldea) as an optional extra region, unlocking with any league's badges;
- natural spawning turned off for every other leader, Elite Four member and Champion RCT has: the rest of the Radical Red series (such as Clair and its Elite Four), all of Unbound's, and the rematch and cameo versions that RCT spawns like ordinary trainers (Sinnoh's leader rematches, Radical Red's Johto leaders). No gym leader wanders the wild outside CJP's gyms;
- `wild_trainers.json` lists for Kanto (447 trainers, FireRed's, with FireRed's teams), Sinnoh (483, Brilliant Diamond/Shining Pearl's) and Galar (147, Sword & Shield's, which CJP adds to RCT), so their [wild trainers](#wild-trainers) only spawn once the region is open. `scripts/generate-wild-trainers.ps1` builds Kanto's and Sinnoh's lists, `scripts/generate-kanto-trainers.ps1` gives Kanto's trainers FireRed's teams from `scripts/data/kanto-trainers.json`, and `scripts/generate-galar-trainers.ps1` builds Galar's trainers from `scripts/data/galar-trainers.json`;
- reworked trainer loot (see [Trainer loot](#trainer-loot) below).

#### Trainer loot

Beaten RCT trainers drop items at their feet. The built-in datapack replaces RCT's loot tables, keeping each trainer class's kinds of items (fishers drop fishing gear, breeders evolution items, and so on). What a trainer drops depends on the level of its **strongest Pokémon**, the level shown next to its name:

| Trainer level | Items per win | Common | Uncommon | Rare | Epic | Legendary |
|---|---|---|---|---|---|---|
| 1–25 | 1–3 (avg 1.3) | 75% | 22% | 3% | — | — |
| 26–50 | 1–3 (avg 1.5) | 60% | 28% | 9% | 3% | — |
| 51–75 | 1–3 (avg 1.7) | 50% | 30% | 13% | 6% | 1% |
| 76–100 | 1–3 (avg 1.9) | 40% | 32% | 17% | 9% | 2% |

- Every win drops at least one item.
- Stronger items need stronger trainers: Fire, Water, Thunder, Leaf and Ice Stones and TMs from level 26; Moon, Sun, Dusk, Dawn and Shiny Stones, trade evolution items, Rare Candy, Ability Capsules and Patches and the Lucky Egg from level 40; Assault Vest from 51; Choice items, Life Orb, Leftovers and Exp. Share are legendary, so also 51.
- Luck still pushes drops toward rarer items. RCT raises it for every series a player completes.
- Filler like vanilla flowers, saplings, paintings, pottery sherds, armor trims and music discs is replaced by Poké Balls, apricorns and Exp. Candies.
- Radical Red's Team Rocket Boss Giovanni no longer drops a guaranteed Master Ball.

The tables are generated from RCT's own by `scripts/generate-trainer-loot.ps1`, which holds all the numbers above.

The two leagues' tiers:

| Tier | Kanto: unlocked by | Cap | Sinnoh: unlocked by | Cap |
|---|---|---|---|---|
| 1 | (start) | 15 | (start) | 15 |
| 2 | Leader Brock | 21 | Gym Leader Roark | 22 |
| 3 | Leader Misty | 24 | Gym Leader Gardenia | 27 |
| 4 | Leader Lt. Surge | 29 | Gym Leader Maylene | 31 |
| 5 | Leader Erika | 36 | Gym Leader Wake | 36 |
| 6 | Leader Koga | 43 | Gym Leader Fantina | 39 |
| 7 | Leader Sabrina | 47 | Gym Leader Byron | 42 |
| 8 | Leader Blaine | 50 | Gym Leader Candice | 49 |
| 9 | Leader Giovanni | 54 | Gym Leader Volkner | 57 |
| 10 | Elite Four Lorelei, Bruno, Agatha, Lance, then Champion Blue | 100 | Elite Four Aaron, Bertha, Flint, Lucian, then Champion Cynthia | 100 |

Galar's tiers:

| Tier | Unlocked by | Cap |
|---|---|---|
| 1 | (start) | 20 |
| 2 | Gym Leader Milo | 24 |
| 3 | Gym Leader Nessa | 27 |
| 4 | Gym Leader Kabu | 32 |
| 5 | Gym Leaders Bea, then Allister | 38 |
| 6 | Gym Leader Opal | 42 |
| 7 | Gym Leaders Gordie, then Melony | 46 |
| 8 | Gym Leader Piers | 48 |
| 9 | Gym Leader Raihan | 49 |
| 10 | Marnie, Hop, Bede, then Champion Leon | 100 |

Each region's species are split into its 10 tiers by Base Stat Total, weakest first; see the tier files for the lists.

Players can start in either league and travel to the other once they're Champion. Taking Sinnoh's trainers into the `sinnoh` series removes them from RCT's own `bdsp` series in that world, the same way Kanto's trainers leave `radicalred`.

It's an ordinary datapack, so it shows in `/datapack list` as `ultimate-cobblemon-progression:kanto_league` (the id is kept from before Sinnoh was added). Turn it off with `/datapack disable "ultimate-cobblemon-progression:kanto_league"`, or untick it when creating a world, to use your own region data or the trainer-level ladder instead. Your own datapacks load after it, so they can override any of its files. Without RCT installed it isn't offered at all.

For good-looking leaders, players can add the [RCT Trainer Textures Plus](https://modrinth.com/resourcepack/rct-trainer-textures-plus) resource pack, which replaces RCT's skins for every trainer in the league.

**With Legendary Monuments.** Four of Legendary Monuments' battle-victory scripts (Archie, Maxie, Mustard and Zinnia) treat every NPC you beat as a Cobblemon NPC. RCT's trainers aren't Cobblemon NPCs, so beating any of them, a gym leader included, crashed the game. When both mods are installed, CJP adds an always-on pack, `ultimate-cobblemon-progression:legendary_monuments_fix`, that replaces those four scripts with copies that skip other mods' trainers. Legendary Monuments' own trainers work as before.

### Advancements: the Pokémon Journey tab

CJP keeps its own advancements for the journey, in one advancement tab, **Pokémon Journey**. Each league is a branch of it: one advancement per badge (named after the badge, with the leader's signature item as its icon), one per Elite Four member, then the Champion. Tiers unlock through these advancements, so they're what `/trainerlevel get` and the Trainer Card name as your next step ("Next: Boulder Badge").

| Advancement | Earned by |
|---|---|
| `ultimate-cobblemon-progression:journey/kanto/boulder_badge` … `earth_badge` | Beating Kanto's 8 gym leaders |
| `…/journey/kanto/elite_four_lorelei`, `…_bruno`, `…_agatha`, `…_lance` | Beating each Elite Four member (either Agatha counts) |
| `…/journey/kanto/champion` | Beating Champion Blue: completes Kanto |
| `…/journey/sinnoh/coal_badge` … `beacon_badge`, `…/elite_four_aaron` … `…_lucian`, `…/sinnoh/champion` | The same for Sinnoh |

Operators can still adjust a player's progress with `/advancement grant` and `/advancement revoke` on these ids.

**How they're earned.** Every Radical Cobblemon Trainers win fires CJP's own advancement trigger, `ultimate-cobblemon-progression:defeat_trainer`:

```json
"criteria": {
  "defeat": {
    "trigger": "ultimate-cobblemon-progression:defeat_trainer",
    "conditions": { "trainers": ["leader_brock_019e"] }
  }
}
```

`trainers` lists RCT trainer ids, any one of which counts (leave it out to match any trainer). Your own datapacks can use the trigger too.

**RCT's advancements don't fire for league trainers.** Beating a trainer that's listed in any region's tiers (a CJP league trainer) awards **only** CJP's advancements: none of RCT's, not its badge for that trainer, not "Defeat any trainer", not its "all Champions" count. RCT's advancements keep working for every other RCT trainer. In RCT's own advancement tab, its entries for the league trainers simply stay unearned.

#### Badge milestones

Regions without a `series` unlock with **milestones**: hidden advancements (they don't show in the tab) earned by beating your *n*-th gym in **any** league (for a gym with two leaders, like Galar's 4th and 6th, the second of them), `…/journey/milestones/badge_1` to `badge_8`, and by becoming any league's Champion, `…/journey/milestones/champion`. Johto's tier 2 needs `badge_1`, so it grows the same way whether you're collecting Kanto or Sinnoh badges. A new league only has to be added to the milestones, not to every region's tier files.

**Generating them.** `scripts/generate-league-advancements.py` (Python 3) writes every journey advancement and milestone from the region data, using the badge and Champion names in `scripts/league-advancements.json` and the trainers' names and signature items from the pack and RCT. It also sets the tier files' `requirements` and each league's `completedBy` to match. Run it after changing a league's trainers or adding a league:

```
python scripts/generate-league-advancements.py
```

### Gyms

Gyms give each step of a region a place to go. A gym is a building with a **Gym Challenge** block inside. Using the block summons **your next gym leader** in front of it: the trainer your next locked tier asks for. Every gym works for every leader and every region, so after Kanto the same gyms summon the next region's leaders.

**Where they are.** Gyms only generate in villages. A gym can replace the centre of a new village (plains, desert, savanna, snowy and taiga), with the village bell just inside its door. A village has one centre, so it never gets more than one gym, and gyms never generate anywhere else. `gymVillageChance` in `ucp-common.json` sets how many villages get one. Only newly generated villages can have gyms. In creative, the block is in the Functional Blocks tab, so you can also build your own gyms. Outside creative it can't be broken.

**Dynamax and Gigantamax (Mega Showdown).** <a id="dynamax-and-gigantamax-mega-showdown"></a>With [Cobblemon: Mega Showdown](https://modrinth.com/mod/cobblemon-mega-showdown) installed, battles can use Dynamax, and Galar's leaders Dynamax their ace as in Sword and Shield: Milo's Eldegoss and Hop's Inteleon Dynamax, and the others' aces Gigantamax (Nessa's Drednaw, Kabu's Centiskorch, Bea's Machamp, Allister's Gengar, Opal's Alcremie, Gordie's Coalossal, Melony's Lapras, Raihan's Duraludon, Marnie's Grimmsnarl, Bede's Hatterene, Leon's Charizard). Piers never Dynamaxes. Mega Showdown only allows Dynamax near a Power Spot, so every Gym Challenge block counts as one (within Mega Showdown's `powerSpotRange`, 20 blocks by default): wear a Dynamax Band and you can Dynamax in the gym too. The leaders' Dynamax is set with RCT API's `gimmicks` field in their team files; without Mega Showdown it does nothing.

**The buildings** are the 8 Kanto gyms from [Radical Gyms & Structures](https://modrinth.com/datapack/radical-gyms-cobblemon) by JayOnline (MIT), picked at random for each gym village. Their themes (Pewter, Cerulean, …) are just looks: any of them summons whichever leader you need. The gyms use [CobbleFurnies](https://modrinth.com/mod/cobblefurnies) furniture, so village gyms only generate when CobbleFurnies is installed. Don't install RGS alongside CJP's gyms, or its own copies will also generate outside villages.

**Who gets summoned.** CJP looks at your journey's current region first, then your other regions in order, and takes the first open, unfinished one whose next tier lists `trainers`. It summons the first trainer in that list you haven't beaten. With [two leagues at once](#two-leagues-at-once), both leagues can have a leader ready, and the block asks which one to battle:

```json
{
  "levelCap": 100,
  "trainers": ["elite_four_lorelei_004e", "elite_four_bruno_0051",
               ["elite_four_agatha_0053", "elite_four_agatha_0054"],
               "elite_four_lance_0057", "champion_terry_01b8"],
  "requirements": { "advancements": ["ultimate-cobblemon-progression:journey/kanto/champion"] }
}
```

Each entry is a Radical Cobblemon Trainers trainer id. A nested list means "any one of these": beating either Agatha counts, and the first one is summoned. `trainers` only picks who appears. The tier still unlocks through its `requirements`, as before. Gyms need Radical Cobblemon Trainers installed.

**One leader at a time.** A gym holds one leader for one challenger. While it's taken, other players are told who is battling and must wait. The leader leaves, and the gym is free again, when:

- the challenger beats it,
- the challenger logs out, leaves the dimension or goes more than 32 blocks away,
- or 3 minutes pass without a battle.

**Rematches.** Sneak and use a Gym Challenge block with an empty hand to open the rematch menu: every leader, Elite Four member and Champion you've beaten, from every region on your journey, each with a **Normal** button and (when it has one) a **Hard** button, whatever your [gym difficulty](#gym-difficulty). The leader is summoned like any other and leaves the same way. A rematch changes nothing: no badges, tiers or level cap. Radical Cobblemon Trainers' progression rules are skipped for it, so you can rematch an earlier region's leaders after travelling on, and your level cap doesn't stop the battle. The leader drops its loot again, as RCT trainers always do.

**Team limit (optional).** With `gymTeamLimit` on in `ucp-common.json`, you can bring no more Pokémon to a gym battle than the leader has: Brock has 2, so you can bring at most 2. The leader says so when it accepts your challenge. If your party is bigger, the battle doesn't start, and you're told how many to put in the PC. Every Pokémon in your party counts, fainted ones included. It applies to every trainer a Gym Challenge block summons, the Elite Four and Champion included.

**Unlocked Pokémon only (optional).** Eggs, trades, fossils and other mods can give you a Pokémon from a region, or a tier, you haven't unlocked yet. With `gymLegalityCheck` set to `"block"` in `ucp-common.json`, a gym leader won't battle you while one is in your party: it names them when it accepts your challenge, and the battle doesn't start until they're in the PC. `"warn"` only names them. What's allowed depends on `gymLegalityRule`:
- `"region"` (the default): any Pokémon of a region you've opened (your home region, the regions you've travelled to and your second region), so you can evolve what you catch and bring it along. A regional form counts as its own region's: a Kanto Pikachu evolved into an Alolan Raichu needs Alola. Without region mode, a Pokémon is allowed when it, or a Pokémon it evolved from, is from a tier you've unlocked.
- `"strict"`: only Pokémon of tiers you've unlocked, the ones you can meet in the wild now. Evolving a Pokémon into one of a later tier keeps it out until you unlock that tier.

Under both, the Pokémon must also be at or below your level cap. Your starter is always allowed, from whichever region you picked it, and so are Pokémon without a region or tier. Rematches aren't checked, since they don't change your progress.

### Gym difficulty

Every leader, Elite Four member and Champion in the built-in leagues comes in two versions:

| | Normal | Hard |
|---|---|---|
| Team | As in the original games | The same, plus 1 to 2 Pokémon for most leaders. The Elite Four have 6. |
| Levels | As in the original games | Every Pokémon at the level of the trainer's ace: Brock's whole team is level 14, Cynthia's level 66 |
| IVs | All six equal, rising through the journey: 6 for the first gym, 24 for the eighth, 28 for the Elite Four, 31 for the Champion | 31 |
| EVs | None | Trained for each Pokémon's role |
| Held items | As in the original games (none in Kanto) | Fitting the point in the journey: berries and type boosters early, Leftovers, Life Orb and Focus Sash later |
| Moves | As in the original games | Built as a set: setup moves, coverage, hazards |
| AI | Sharper than RCT's default | Sharper still |
| Bag | As in the original games, 2 uses per battle | 2 Super Potions, Hyper Potions or Full Restores, 2 uses per battle |

**Choosing.** After you pick where your journey starts, your Trainer Card asks **Choose your challenge**: Normal or Hard. You can switch at any time on the card's Road tab, or with `/trainerlevel difficulty <normal|hard>`. The change applies from the next leader a gym summons (a leader already waiting keeps its team), and badges you already have stay as they are. The Road tab marks the leaders you beat on Hard. A server can set `gymDifficulty` in `ucp-common.json` to `"normal"` or `"hard"` for everyone; then the card doesn't ask.

**Either version counts.** Beating the Hard leader earns the same badge and opens the same tier. Radical Cobblemon Trainers' level cap and trainer order follow the Normal leaders, so CJP also marks the Normal leader as beaten in RCT when you beat the Hard one. Your level cap rises the same way whichever you fought. The Hard leader looks and talks like the Normal one, so trainer texture packs cover both.

**For datapacks.** A Hard leader is an RCT trainer whose id is the Normal one's plus `_hard` (`leader_brock_019e_hard`), listed after it in the tier file's slot: `["leader_brock_019e", "leader_brock_019e_hard"]`. Its RCT trainer data (`mobs/trainers/single/`) should be a copy of the Normal one's with `"optional": true`, so RCT's series ignores it. Slots without a `_hard` entry summon the same trainer on both difficulties.

`scripts/convert-rgs-gyms.py` builds the gym files from RGS: it removes RGS's command blocks and adds the Gym Challenge block, the bell and the jigsaw blocks that connect a gym to its village.

---

A region whose leaders have no Hard versions (a datapack's own region, say) always summons them at Normal. Its Road tab shows **Normal** without the switch.

## Trainer Card

Every player gets a **Trainer Card** the first time they join (turn this off with `giveTrainerCard`). Using it opens a Pokédex-style screen, worked out fresh by the server each time. The first time, it asks where your journey starts ([see above](#choosing-where-your-journey-starts)), and each time you complete a region it asks where your journey goes next ([see above](#moving-on-to-your-next-region)); **Not now** skips that question until the card is opened again. Every pick asks you to confirm first. Once your journey has started, it also asks you to [choose your challenge](#gym-difficulty) once: Normal or Hard gym leaders. Otherwise the card has five tabs (four without Radical Cobblemon Trainers):

- **Profile:** your current region (the one your journey is in) and tier, your level cap (Radical Cobblemon Trainers' cap when it's installed, otherwise the tier's), trainers beaten, Pokédex counts, and your next challenge. Between regions, it reminds you to choose where your journey goes next.
- **Road:** your gym difficulty, with a button to switch it, then every trainer the region's tiers list, in order, marked ✔ beaten, ▶ next or • later, with the tier each one unlocks and the level of its strongest Pokémon (on your difficulty). Trainers of tiers you've unlocked always count as beaten, and the ones you beat on Hard are tagged **HARD**.
- **Trainers:** the [wild trainers](#wild-trainers) of each open region, weakest first, marked ✔ beaten or • not yet, with the level of each one's strongest Pokémon (a range, like Lv 12–48, for a trainer with rematches) and how many of the region's you've beaten. A trainer counts once, whichever of its versions you beat. **Hide beaten** leaves only the ones still to beat, and clicking a region's header folds its list away. Only wins since this tab was added count.
- **Regions:** where you stand in every region: complete, a progress bar for the tier you're on, or locked. Until you've picked your [extra starting region](#picking-a-second-starting-region), this tab also has a **Pick** button on each one that isn't open yet (and the tab is marked **!**).
- **Wild Pokémon:** every species that can currently spawn for you, per open region, with Cobblemon's caught/seen marker from your Pokédex and how many the next tier adds. Hover a species for its number and types.

Without region data, the card shows Profile (trainer level, level cap, what you need for the next level) and Wild Pokémon only. The screen uses Cobblemon's own Pokédex art, loaded from the installed Cobblemon.

Lost your card? `/trainerlevel card` gives you a new one if you don't have one. It's also in the creative Tools & Utilities tab.

