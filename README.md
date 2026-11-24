# Cobblemon Journey Progression

*Formerly Ultimate Cobblemon Progression (UCP).*

A Fabric side-mod for **Cobblemon** that turns the game into a region-by-region Pokémon journey. Start in a region, find its gyms in villages, beat its leaders, and unlock its Pokémon tier by tier: what spawns, how strong it is, and how far your team can level all grow with your progress.

**Requires Cobblemon 1.8** (Minecraft 1.21.1, Fabric). Latest release: [2.5.0](https://github.com/Lucalaure/ultimate-cobblemon-progression/releases/latest).

## Documentation

- **[How CJP Works](docs/how-it-works.md)**: the full guide to regions, gyms, the Trainer Card, tiers, rarity, spawning, wild levels, the level cap, configuration, datapacks and commands.
- **[Species by Tier and Rarity](docs/species-by-tier.md)**: every naturally spawning Pokémon, its tier, and how rare Cobblemon makes it.
- **[Changelog](CHANGELOG.md)**
- **[Roadmap](docs/roadmap.md)**: planned features.

---

## Installing

Required: **Cobblemon 1.8**, **Fabric API**, **Fabric Language Kotlin** and **Cardinal Components API**.

For the region journey (gyms and the built-in Kanto, Sinnoh and Galar leagues), also install:

- **[Radical Cobblemon Trainers](https://modrinth.com/mod/rctmod)** (RCT): the trainers, their teams and their battles.
- **[CobbleFurnies](https://modrinth.com/mod/cobblefurnies)**: furniture used inside the gym buildings.
- Optional: **[Cobblemon: Mega Showdown](https://modrinth.com/mod/cobblemon-mega-showdown)**, for Dynamax and Gigantamax. Galar's leaders then Dynamax their ace as in Sword and Shield, and every Gym Challenge block is a Power Spot, so you can Dynamax there too with a Dynamax Band.
- Recommended, client-side: the **[RCT Trainer Textures Plus](https://modrinth.com/resourcepack/rct-trainer-textures-plus)** resource pack, for gym leaders that look like themselves.

Don't install Radical Gyms & Structures alongside CJP: CJP already ships its gym buildings, and RGS would add more of them outside villages.

Without RCT, CJP still works, using its original trainer-level ladder (see [Classic mode](#classic-mode-trainer-levels)).

---

## How it plays

1. **Pick where your journey starts.** Every player gets a **Trainer Card** on joining. The first time you open it, it asks which region to start in: Kanto, Sinnoh or Galar. That region opens, and you join its trainer series.
2. **Find a gym.** New villages can have a **gym** at their centre, one at most per village. Using its **Gym Challenge** block summons your next leader into the arena; sneaking while using it lets you rematch any leader you've already beaten. One leader stands in a gym at a time.
3. **Beat the leader to unlock the next tier.** Each badge opens more of the region's Pokémon, stronger wild levels and a higher level cap. After the eight gyms come the Elite Four and the Champion, and beating the Champion completes the region.
4. **Choose where your journey goes next.** Once you're Champion, your Trainer Card asks which league to travel to next. Your journey continues there from its first gym, with a fresh level cap. The regions you've finished stay open: their Pokémon keep spawning, mostly at levels that fit your new team.
5. **Track it all on your Trainer Card**: your region, tier, level cap and next challenge, the region's trainer order, every region's progress, and the wild Pokémon you can meet, with Cobblemon's caught/seen markers. Every badge, Elite Four win and Champion title is also an advancement in the **Pokémon Journey** tab.

You can also pick one **extra region** to explore from the start, on the card's Regions tab. Its Pokémon open alongside your main region.

### The built-in leagues

With RCT installed, CJP turns on a built-in datapack, **CJP Kanto, Sinnoh & Galar Leagues**: a 10-tier Kanto with its 8 gym leaders, the Elite Four and the Champion (FireRed/LeafGreen-style teams), a 10-tier Sinnoh with RCT's Brilliant Diamond/Shining Pearl leaders, Elite Four and Champion Cynthia, a 10-tier Galar with its 8 Gym Leaders, the Champion Cup and Champion Leon (Sword teams, Normal difficulty only for now), plus every other region from Johto to Paldea as an optional extra region that grows with any league's badges. It's an ordinary datapack, so you can turn it off, or override it with your own region data. See [Region mode](docs/how-it-works.md#9-region-mode).

---

## What changes for wild Pokémon

Whether you follow regions or the classic ladder, your progress controls:

- **Which species can spawn** for you: Pokémon above your tier (or from regions you haven't reached) are blocked.
- **How strong wild Pokémon are**: levels are rerolled based on the Pokémon's tier and yours.
- **Which region's Pokémon you meet most**: in region mode, the region you're working through is favored over the ones you've finished (see [spawn pools](docs/how-it-works.md#wild-pokémon-in-a-new-region-spawn-pools)). Otherwise, unlocking a tier only adds Pokémon.

**Rarity is still Cobblemon's.** Whether a Pokémon is common or ultra-rare comes from Cobblemon's spawn data, and CJP doesn't change those odds. Tiers measure strength, not rarity, so a Pokémon can be low-tier and ultra-rare (Dratini) or high-tier and common (Slaking). See [Rarity](docs/how-it-works.md#3-rarity-whats-rare-and-whats-not).

---

## Classic mode: trainer levels

Without region data (for example without RCT, or with the built-in leagues turned off), CJP gives every player a **Trainer Level** from 1 to 7. Each level is a tier, and you level up automatically by seeing and catching enough different species in your Pokédex. Your Pokémon can't gain XP past your tier's level cap.

| Tier | Base Stat Total | Level cap | To reach it (seen / caught) |
|------|-----------------|-----------|------|
| 1 | 175 – 305 | 15 | start |
| 2 | 306 – 354 | 27 | 30 / 10 |
| 3 | 355 – 440 | 40 | 80 / 20 |
| 4 | 441 – 475 | 54 | 140 / 35 |
| 5 | 476 – 500 | 69 | 210 / 55 |
| 6 | 501 – 570 | 85 | 290 / 80 |
| 7 | 571 and up | 100 | 380 / 110 |

The default tiers also cover species from Myths and Legends, Complete Cobblemon Collection, Mega Showdown and MysticMons, but CJP doesn't require them. Tiers are datapack-driven, so you can replace them: see [Custom tiers](docs/how-it-works.md#7-custom-tiers-datapacks).

---

## Commands

```
/trainerlevel get                          your progress (everyone)
/trainerlevel card                         a new Trainer Card, if you lost yours (everyone)
/trainerlevel region next <region>         travel to your next region, once yours is complete (everyone)
/trainerlevel region choose <region>       pick your extra starting region (everyone)
/trainerlevel region reset <player>        clear a player's extra region choice (op level 2)
/trainerlevel difficulty [normal|hard]     show or change your gym difficulty (everyone)
/trainerlevel set <player> <level>         classic mode: set a trainer level (op level 2)
/trainerlevel promote <player>             classic mode: raise a player one level (op level 2)
```

## Configuration

Settings live in `config/ultimate-cobblemon-progression/`. Highlights from `ucp-common.json`:

- `gymVillageChance` (default `0.3`): the share of new villages whose centre is a gym. `0` turns village gyms off.
- `giveTrainerCard` (default `true`): give every player a Trainer Card on their first join.
- `gymDifficulty` (default `"player_choice"`): each player picks Normal or Hard gym leaders on their Trainer Card and can switch any time; `"normal"` or `"hard"` sets it for everyone. See [Gym difficulty](docs/how-it-works.md#gym-difficulty).
- `allowTwoLeagues` (default `true`): players may pick a second league, with its own gym leaders, and work through both at once. Gyms ask which league's leader to battle, and the level cap is the average of the two (or the lower of the two, with `twoLeagueCap` set to `"lowest"`). See [Two leagues at once](docs/how-it-works.md#two-leagues-at-once).
- `catchWholeTier` (default `false`): difficulty option, catch every Pokémon of your tier before you can face the next gym leader (your extra region also waits until its tier is caught).

And from `ucp-spawn.json`:

- `regionPoolWeight`, `pastInCapPoolWeight`, `pastFullPoolWeight` (defaults `1.0`, `0.5`, `0.15`): how often you meet the region you're working through, finished regions' Pokémon at your new team's levels, and finished regions' Pokémon at full strength. See [spawn pools](docs/how-it-works.md#wild-pokémon-in-a-new-region-spawn-pools).
- `doWeightScaling` (default `false`): favor Pokémon closer to your tier within each rarity. Off, unlocking a tier only adds Pokémon.

The full list is in [Configuration](docs/how-it-works.md#6-configuration).

---

## Credits

- Based on **[Ultimate Cobblemon Progression](https://github.com/ppVon/ultimate-cobblemon-progression)** by ppVon (MIT), which this project continues independently.
- Gym buildings adapted from **[Radical Gyms & Structures](https://modrinth.com/datapack/radical-gyms-cobblemon)** by JayOnline (MIT). Its notice is in [`src/main/resources/licenses/`](src/main/resources/licenses/).
- Trainers and battles by **[Radical Cobblemon Trainers](https://modrinth.com/mod/rctmod)**. The Trainer Card uses Cobblemon's own Pokédex art, loaded from your Cobblemon install.

## Building

Requires JDK 21.

```
./gradlew build
```

The mod jar is written to `build/libs/`. `scripts/` has the tools that generate the default tiers, region tiers, and the gym buildings from RGS.
