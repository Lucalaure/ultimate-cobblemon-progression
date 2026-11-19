# How Ultimate Cobblemon Progression Works

This guide explains exactly what UCP changes in Cobblemon, using the mod's default settings and tier data. Every number here comes from the code and the shipped data files; the full species list is in [Species by Tier and Rarity](species-by-tier.md).

## The short version

- Every player has a **trainer level** from 1 to 7. Each level is a **tier**.
- Each Pokémon species belongs to one tier, based on its strength (Base Stat Total). **Wild Pokémon above your tier can't spawn for you.**
- **Rarity is Cobblemon's, not UCP's.** Whether a Pokémon is common or ultra-rare is decided by Cobblemon's spawn data. UCP never changes those odds. A Pokémon can be low-tier and ultra-rare (Dratini), or high-tier and common (Slaking).
- Wild Pokémon **levels** are rerolled based on the Pokémon's tier and yours.
- Your own Pokémon **can't gain XP past your tier's level cap**.
- You **level up automatically** by seeing and catching enough different species in your Pokédex.
- **Region mode** (optional) replaces the single ladder with one ladder per region, unlocked by gym badges. When you finish a region's league, you **choose which region your journey goes to next**. See [section 9](#9-region-mode).
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
| `giveTrainerCard` | `true` | Give every player a [Trainer Card](#trainer-card) on their first join. |
| `gymVillageChance` | `0.3` | Share of newly generated villages (0 to 0.95) whose centre is a [gym](#gyms). `0` turns village gyms off. Takes effect the next time a world starts. |
| `gymTeamLimit` | `false` | Difficulty option: you can bring no more Pokémon to a [gym](#gyms) battle than the leader has. |

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
| `requirements.advancements` | No | Advancement ids that must all be completed to **reach** this tier, e.g. `["rctmod:trainers/defeat_leader_brock"]`. An entry can also be a list of alternatives, where completing any one counts: `[["rctmod:trainers/defeat_leader_brock", "rctmod:trainers/defeat_gym_leader_roark_1"]]` needs Brock **or** Roark. |
| `species` | No | Species ids in this tier. |

Rules and tips:

- **Number tiers 1, 2, 3, … with no gaps.** The highest number is the maximum trainer level.
- **Use species ids, not form ids.** `cobblemon:vulpix` covers every Vulpix form. Entries like `cobblemon:bergmite_hisui` don't match anything and are ignored.
- **List each species in only one tier.**
- **Advancement requirements** are re-checked the moment an advancement is earned, so tiers can be tied to anything that grants one: beating a trainer, finishing a quest, or a custom datapack advancement.
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

In region mode, `/trainerlevel get` shows each region's tier and what's needed next instead, and `set`/`promote` are disabled (use `/advancement grant` and `/advancement revoke`). Three more commands exist in region mode:

| Command | Permission | What it does |
|---|---|---|
| `/trainerlevel region next <region>` | Everyone | Picks the region your journey continues in, once your current region is complete. Same as picking it on the Trainer Card. |
| `/trainerlevel region choose <region>` | Everyone | Picks your extra starting region (once). |
| `/trainerlevel region reset <player>` | Operator (level 2) | Clears a player's starting region choice. |

---

## 9. Region mode

Region mode gives each region (Kanto, Johto, …) its **own ladder of tiers**, unlocked by advancements such as gym-leader wins. It turns on automatically when any region data is loaded; without region data, everything above works as described.

### How it plays

1. You pick the **region your journey starts in** (your home region), where only its weakest species can spawn.
2. Each **tier** of that region is unlocked by its requirements, typically beating the next gym leader. Earning the advancement unlocks the tier immediately and tells you in chat how many species it added.
3. When you earn the region's **completion advancement** (e.g. the Champion), the region is complete. Completed regions stay fully open.
4. You then **choose your next region** from the leagues you haven't visited yet, and your journey continues there from its first tier. See [Moving on to your next region](#moving-on-to-your-next-region).

Regions come in two kinds:

- **League regions** have a trainer `series` (Kanto and Sinnoh in the built-in data). They're the stops on your journey: one opens when you start in it or travel to it.
- **Other regions** (Johto, Hoenn, … in the built-in data) only add species. They don't open on their own once your journey has started: the only way to open one is to pick it as your [extra starting region](#picking-a-second-starting-region). Completing a region opens nothing but the region you choose next.

Species from regions you haven't reached, and species in no region at all, can't spawn (the second case follows `blockUnknownSpecies`). This only affects wild spawns: trades, eggs and evolutions aren't restricted.

Progress is worked out from your advancements (and Pokédex, if a tier asks for it) each time it changes, so a trainer mod resetting its own series progress never re-locks a region. UCP only stores the regions you picked.

### Differences from the tier ladder

- **Wild levels and weights** work exactly as in [sections 3](#weight-scaling-whos-favored-within-a-rarity) and [4](#4-wild-pokémon-levels), but each species is compared with your progress **in its own region**, using that region's tier caps. See [Wild Pokémon in a new region](#wild-pokémon-in-a-new-region).
- **UCP's level cap is off.** Region mode is meant to be paired with a trainer mod that has its own cap, such as Radical Cobblemon Trainers.
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
  "completedBy": "rctmod:trainers/defeat_champion_terry"
}
```

| Field | Required | Meaning |
|---|---|---|
| `order` | No (default 0) | Unlock order. Before a player has a journey (or when no region has a `series`), the lowest order is open from the start and each later region opens when the one before it is complete. With a journey, only the regions on it (and the extra starting region) are open, and `order` just sorts the Trainer Card's lists. |
| `title` | No | Name shown in chat and `/trainerlevel get`. Defaults to the folder name. |
| `completedBy` | No | Advancement that completes the region. Without it, the region is complete once its last tier is unlocked. |
| `startChoice` | No (default false) | Lets players pick this region as their extra starting region (see below). Ignored on the first region, which is always open, and on regions with a `series`. |
| `series` | No | The Radical Cobblemon Trainers series for this region's trainers, e.g. `"kanto"`. A region with a series is a **league region**: it can be picked as where a player's journey starts, or where it goes next (see below). |

### Choosing where your journey starts

The first time a player opens their [Trainer Card](#trainer-card), it asks **which region their journey starts in**, before showing anything else. Every region with a `series` is offered, with its number of trainers, species and first leader. Picking one (after confirming, since it's permanent):

- opens that region from the start, in place of the first region;
- puts the player into its Radical Cobblemon Trainers series, so its leaders will battle them (if they're already in that series, nothing is reset).

Until a player has picked, gyms ask them to open their card first. If no region has a `series`, there's nothing to pick and the first region is open from the start, as before.

### Moving on to your next region

When every region on your journey is complete and there's a league region you haven't been to, your journey moves on: chat tells you to choose, and the next time you open your [Trainer Card](#trainer-card) it asks **where your journey goes next**, with each region's trainers, species and first leader. **Not now** shows the card first; it asks again next time. The same pick is also a command:

```
/trainerlevel region next <region>
```

Picking one (after confirming, since it's permanent):

- opens that region at its first tier, and its gyms summon its leaders from then on;
- puts you into its Radical Cobblemon Trainers series. RCT gives each series its own level cap, so your cap **starts over** with the new region's first leader (15 in the built-in leagues), and RCT's trainers won't battle you while a party Pokémon is above it (unless the server sets RCT's `allowOverLeveling`). Your old team is still yours, but a new region means training a new one, much like a new game.

Only the region you pick opens. Everything you'd already opened stays open. Until you pick, gyms tell you to open your card. When you've completed every league region, your journey is complete.

### Wild Pokémon in a new region

Each species is compared with your progress in **its own region** (sections [3](#weight-scaling-whos-favored-within-a-rarity) and [4](#4-wild-pokémon-levels)), so a new region starts low while the regions you've finished stay strong. Right after a Kanto Champion travels to Sinnoh, with the built-in data and default settings:

| Wild Pokémon | Its tier cap | Your cap in its region | Level range (typical) | Weight |
|---|---|---|---|---|
| Sinnoh tier 1 (Bidoof, Starly, …) | 15 | 15 (Sinnoh tier 1) | 6–16 (11) | × 2.0 |
| Sinnoh tier 2 and up | | | can't spawn yet | |
| Kanto tier 1 (Pidgey, Rattata, …) | 15 | 100 (Kanto complete) | 16–39 (27) | × 0.2 |
| Kanto tier 10 (Dragonite, Snorlax, …) | 100 | 100 | 45–100 (75) | × 2.0 |
| Johto to Paldea | | | can't spawn (not on your journey) | |

Each Sinnoh badge then opens Sinnoh's next tier and raises your Sinnoh cap, which pushes Sinnoh's lower tiers up with it. Cobblemon still decides where each species spawns (biome, time, rarity), so Sinnoh and Kanto Pokémon mix in the same world. Wild levels use the nearest player within 128 blocks, so they follow whoever is closest.

### Picking a second starting region

If any region has `"startChoice": true`, each player can open **one** of them from the very start, alongside the first region. The easiest way is the [Trainer Card](#trainer-card): its Regions tab shows a **Pick** button on every starting choice (the tab is marked **!** until you've picked), and asks you to confirm before anything changes. The same pick is also a command:

```
/trainerlevel region choose <region>
```

Players are reminded on joining and in `/trainerlevel get` until they pick. The choice is permanent; an operator can clear it with `/trainerlevel region reset <player>`.

Picking a region only opens it. Its tiers still unlock through their own requirements, so a chosen region grows with whatever its tier files ask for. The built-in data ties the other regions to the badges of whichever league you're in: each tier needs the matching Kanto badge **or** the matching Sinnoh badge.

`tier_N.json` files use the same format as [custom tiers](#7-custom-tiers-datapacks): `levelCap`, `species`, and `requirements` (usually `advancements`), plus an optional `trainers` list for [gyms](#gyms). Tiers must be numbered 1, 2, 3… with no gaps, and tier 1 normally has no requirements. A species belongs to the first region that lists it; its forms come with it.

`scripts/generate-region-tiers.ps1` builds the tier files from a Cobblemon regional Pokédex, splitting its species into equal-sized tiers by Base Stat Total. See the script for usage.

### The built-in leagues

When Radical Cobblemon Trainers is installed, UCP turns on a built-in datapack, **UCP Kanto & Sinnoh Leagues**, in every world. It contains:

- a 10-tier **Kanto**, with each tier after the first unlocked by one of its gym leaders, and the Champion completing it;
- Kanto's RCT trainers: the 8 leaders, the Elite Four (either Agatha counts) and the Champion, with FireRed/LeafGreen-style teams, in a `kanto` series, and their natural spawning turned off so they only appear in gyms;
- a 10-tier **Sinnoh**, built the same way from RCT's own Brilliant Diamond/Shining Pearl trainers: Roark, Gardenia, Maylene, Crasher Wake, Fantina, Byron, Candice and Volkner, then Aaron, Bertha, Flint, Lucian and Champion Cynthia, in a `sinnoh` series. Their teams are RCT's (aces from level 14 to 66), and each one requires the one before it, so RCT's level cap follows the league (15 at the start, then each next trainer's level). Each tier's cap is the level of the next trainer's strongest Pokémon, as in Kanto;
- every other region (Johto, Hoenn, Unova, Kalos, Alola, Galar, Paldea) as an optional extra region, unlocking with either league's badges.

Players can start in either league and travel to the other once they're Champion. Taking Sinnoh's trainers into the `sinnoh` series removes them from RCT's own `bdsp` series in that world, the same way Kanto's trainers leave `radicalred`.

It's an ordinary datapack, so it shows in `/datapack list` as `ultimate-cobblemon-progression:kanto_league` (the id is kept from before Sinnoh was added). Turn it off with `/datapack disable "ultimate-cobblemon-progression:kanto_league"`, or untick it when creating a world, to use your own region data or the trainer-level ladder instead. Your own datapacks load after it, so they can override any of its files. Without RCT installed it isn't offered at all.

For good-looking leaders, players can add the [RCT Trainer Textures Plus](https://modrinth.com/resourcepack/rct-trainer-textures-plus) resource pack, which replaces RCT's skins for every trainer in the league.

### Gyms

Gyms give each step of a region a place to go. A gym is a building with a **Gym Challenge** block inside. Using the block summons **your next gym leader** in front of it: the trainer your next locked tier asks for. Every gym works for every leader and every region, so after Kanto the same gyms summon the next region's leaders.

**Where they are.** Gyms only generate in villages. A gym can replace the centre of a new village (plains, desert, savanna, snowy and taiga), with the village bell just inside its door. A village has one centre, so it never gets more than one gym, and gyms never generate anywhere else. `gymVillageChance` in `ucp-common.json` sets how many villages get one. Only newly generated villages can have gyms. In creative, the block is in the Functional Blocks tab, so you can also build your own gyms. Outside creative it can't be broken.

**The buildings** are the 8 Kanto gyms from [Radical Gyms & Structures](https://modrinth.com/datapack/radical-gyms-cobblemon) by JayOnline (MIT), picked at random for each gym village. Their themes (Pewter, Cerulean, …) are just looks: any of them summons whichever leader you need. The gyms use [CobbleFurnies](https://modrinth.com/mod/cobblefurnies) furniture, so village gyms only generate when CobbleFurnies is installed. Don't install RGS alongside UCP's gyms, or its own copies will also generate outside villages.

**Who gets summoned.** UCP looks at your journey's current region first, then your other regions in order, and takes the first open, unfinished one whose next tier lists `trainers`. It summons the first trainer in that list you haven't beaten:

```json
{
  "levelCap": 100,
  "trainers": ["elite_four_lorelei_004e", "elite_four_bruno_0051",
               ["elite_four_agatha_0053", "elite_four_agatha_0054"],
               "elite_four_lance_0057", "champion_terry_01b8"],
  "requirements": { "advancements": ["rctmod:trainers/defeat_champion_terry"] }
}
```

Each entry is a Radical Cobblemon Trainers trainer id. A nested list means "any one of these": beating either Agatha counts, and the first one is summoned. `trainers` only picks who appears. The tier still unlocks through its `requirements`, as before. Gyms need Radical Cobblemon Trainers installed.

**One leader at a time.** A gym holds one leader for one challenger. While it's taken, other players are told who is battling and must wait. The leader leaves, and the gym is free again, when:

- the challenger beats it,
- the challenger logs out, leaves the dimension or goes more than 32 blocks away,
- or 3 minutes pass without a battle.

**Team limit (optional).** With `gymTeamLimit` on in `ucp-common.json`, you can bring no more Pokémon to a gym battle than the leader has: Brock has 2, so you can bring at most 2. The leader says so when it accepts your challenge. If your party is bigger, the battle doesn't start, and you're told how many to put in the PC. Every Pokémon in your party counts, fainted ones included. It applies to every trainer a Gym Challenge block summons, the Elite Four and Champion included.

`scripts/convert-rgs-gyms.py` builds the gym files from RGS: it removes RGS's command blocks and adds the Gym Challenge block, the bell and the jigsaw blocks that connect a gym to its village.

---

## Trainer Card

Every player gets a **Trainer Card** the first time they join (turn this off with `giveTrainerCard`). Using it opens a Pokédex-style screen, worked out fresh by the server each time. The first time, it asks where your journey starts ([see above](#choosing-where-your-journey-starts)), and each time you complete a region it asks where your journey goes next ([see above](#moving-on-to-your-next-region)); otherwise it has four tabs:

- **Profile:** your current region (the one your journey is in) and tier, your level cap (Radical Cobblemon Trainers' cap when it's installed, otherwise the tier's), trainers beaten, Pokédex counts, and your next challenge.
- **Road:** every trainer the region's tiers list, in order, marked ✔ beaten, ▶ next or • later, with each one's tier and level cap.
- **Regions:** where you stand in every region: complete, a progress bar for the tier you're on, or locked. Until you've picked your [starting region](#picking-a-second-starting-region), this tab also lets you pick it.
- **Wild Pokémon:** every species that can currently spawn for you, per open region, with Cobblemon's caught/seen marker from your Pokédex and how many the next tier adds. Hover a species for its number and types.

Without region data, the card shows Profile (trainer level, level cap, what you need for the next level) and Wild Pokémon only. The screen uses Cobblemon's own Pokédex art, loaded from the installed Cobblemon.

Lost your card? `/trainerlevel card` gives you a new one if you don't have one. It's also in the creative Tools & Utilities tab.

