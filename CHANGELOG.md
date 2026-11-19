# Unreleased

## Features
- **Choose your next region**: once every region on your journey is complete, your Trainer Card asks which league region to travel to next (or use `/trainerlevel region next <region>`). Picking one opens it at its first tier, joins its RCT series, and gyms summon its leaders. Once your journey has started, only its regions open: completing a region no longer opens the next one in order, and regions without a `series` stay locked unless picked as your extra starting region.
- **Sinnoh league** in the built-in datapack (now called UCP Kanto & Sinnoh Leagues; its id is still `kanto_league`): Sinnoh's 10 tiers are unlocked by RCT's Brilliant Diamond/Shining Pearl gym leaders, Elite Four and Champion Cynthia, in a new `sinnoh` series that only appears in gyms. You can start your journey in Sinnoh, or travel there after Kanto.
- **Spawn pools**: while you work through a region, wild Pokémon come from three pools: the new region (most common), finished regions' Pokémon within your tier at the new region's levels, and finished regions' Pokémon at their full levels (rarest). You can catch Pokémon for your new team from every region you've finished, and nothing you've unlocked stops spawning. Weights are `regionPoolWeight`, `pastInCapPoolWeight` and `pastFullPoolWeight` in `ucp-spawn.json`; `doRegionPools` turns it off.
- `scripts/generate-region-tiers.py` replaces the PowerShell version: it writes "any one of" requirements (`a|b`) and keeps each tier's gym leaders, level cap and requirements unless you pass new ones.
- Config files are rewritten on load, so options added in a new version show up in them (your own values are kept).
- `/trainerlevel get` marks your current region and says why each locked region is locked.
- Gyms tell you to pick your next region when you've completed your current one.
- **Pokémon Journey advancements**: UCP has its own advancement tab, with every league's badges, Elite Four and Champion (e.g. Boulder Badge with Brock's Hard Stone as its icon). The leagues' tiers unlock through them, and the extra regions through hidden badge milestones (your n-th badge in any league). They're earned with a new trigger, `ultimate-cobblemon-progression:defeat_trainer`, fired on every Radical Cobblemon Trainers win, and generated from the region data by `scripts/generate-league-advancements.py`.
- Beating a UCP league trainer no longer awards any of RCT's own advancements (its badge, "Defeat any trainer", its Champion counts); only UCP's.
- Tier requirements can list alternatives: an `advancements` entry can be a list, where completing any one counts. The built-in extra regions (Johto to Paldea) now grow with the matching Kanto **or** Sinnoh badge.
- Optional gym team limit (`gymTeamLimit` in `ucp-common.json`, off by default): players can bring no more Pokémon to a gym battle than the leader has. The leader says how many when it accepts the challenge, and a battle with a bigger party doesn't start.
- The Mod Menu config's General page now also has the gym team limit, the share of villages with a gym, and the first-join Trainer Card.

## Changes
- Kanto's 8th gym leader is now Giovanni (Earth Badge), with his FireRed/LeafGreen Viridian Gym team, in place of Clair, who goes back to RCT's Radical Red series.
- Weight scaling (`doWeightScaling`) is now off by default: unlocking a tier only adds species and no longer makes the ones you already had rarer. Existing `ucp-spawn.json` files keep their setting; set it to `false` to match.
- Sinnoh is no longer an extra starting region, since it's a league now. Players who had picked it get to pick again.
- Completing a region asks where your journey goes next, instead of opening the next region in order.
- The Trainer Card's Road counts the trainers of every unlocked tier as beaten, even if RCT reset its record of them when you switched series.

## Fixes
- The Trainer Card's confirmation text no longer has a doubled, smeared look (it was drawn with a drop shadow).
- `/trainerlevel get` and the Trainer Card no longer ask you to pick an extra starting region once every option is already open, and an open region can't be picked.
- Village gyms no longer have Radical Gyms & Structures' "use button to respawn leader" sign, which pointed at a button UCP removes.

# UCP 2.0.0

## Features
- **Region mode**: datapacks can define regions (Kanto, Johto, ...), each with its own ladder of tiers unlocked by advancements such as gym badges. Completing a region opens the next, and players can pick one extra region to explore from the start. Spawns, weights and wild levels follow each species' own region.
- Gyms: a Gym Challenge block that summons your next gym leader (Radical Cobblemon Trainers) for your current region step. One leader per gym at a time.
- Gyms can replace the centre of new villages (at most one per village, never outside villages, `gymVillageChance` in `ucp-common.json`). The buildings are the 8 Kanto gyms from Radical Gyms & Structures (MIT), without its command blocks. They need CobbleFurnies installed.
- Region tier files take an optional `trainers` list naming the leaders for that tier, and `region.json` an optional RCT `series`.
- Built-in **UCP Kanto League** datapack, on by default when Radical Cobblemon Trainers is installed: Kanto's tiers, gym leaders, Elite Four and Champion, plus every other region as a second-region choice. No separate datapack needed.
- The first time the Trainer Card opens, it asks which region your journey starts in (any region with a `series`) and joins its trainer series.
- Trainer Card: given on first join (`giveTrainerCard`). Opens a Pokédex-style screen with your region, tier, level cap and next challenge, the region's trainer order, every region's progress, and the wild Pokémon you can meet. Its Regions tab is also where you pick your second starting region. `/trainerlevel card` replaces a lost one.

# UCP 1.4.0

Supports (and requires) Cobblemon 1.8!

The rules and spawning effects from this mod still affect pokemon spawned via bait, PokeSnack, or fishing

# UCP 1.3.2

## Fixes
-  `/trainerlevel promote @p` no longer displays dex progression if dex progression is turned off in the config

# UCP 1.3.1

## Features

- Changed `/trainerlevel set` to include a player argument
  - It is now `/trainerlevel set <player> <new_level>`
- Added `/trainerlevel promote <player>` which simulates an actual level up and will display the appropriate message to the target

# UCP 1.3.0

Supports (and requires) Cobblemon 1.7!

The rules and spawning effects from this mod still affect pokemon spawned via bait or PokeSnack

# UCP 1.2.2

## Fixes
- Fixed a crash caused by a deprecated GSON function

# UCP 1.2.1

## Fixes
- Tier 7 can now be reached naturally via dex progression as intended.  It was bugged before.  It also displays a message and /trainerlevel get will just show that you are max level.
- Tier up message now shows an accurate new species number based on species that are actually registered with cobblemon

# UCP 1.2.0

## Fixes
Performance has been improved by a large factor.  UCP now adds a field to the species class using mixins to store the tier on the species directly, instead of maintaining a hash table of species:tier.

# UCP 1.1.1

## Features
- /trainerlevel get now shows the requirements to level up

## Fixes
- Removed debug log on dex data changed
- Removed runtime dependency of modmenu to make UCP server-side safe
- /trainerlevel set now requires op level 2

# UCP 1.1.0

## Features
- Implemented party level cap (i forgor 💀)
- Trainer level can now be increased naturally via dex seen/caught counts defined per-tier
- Trainer level increases are announced to the player but not the server (yet)

## Config
- Configs no longer use Moonlight so it is no longer a dependency, but modmenu is still supported
- Config files now live in /config/ultimate-cobblemon-progression
- New Options:
  - doLevelCap: Whether to enforce party level caps based on trainer level
  - doDexProgression: Whether to do natural trainer level progression based on dex entries
  - doSpeciesBlocking: Whether to enforce tiers' species lists
  - doLevelScaling: Whether to scale mon spawn levels to their tier's level cap
  - doWeightScaling: Whether to affect the weight of spawned mons to the player's current tier

## Datapacks
- `level-cap` is now `levelCap`
- Dex requirements to reach a tier (other than tier_1) can be defined as:

```json
"requirements": {
  "dex": {
    "seen": 10,
    "caught": 20    
  }
}
```
- `seen` and `caught` are both optional on a per-tier basis
