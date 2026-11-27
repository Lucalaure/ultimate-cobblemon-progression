# CJP 2.9.1

## Fixes
- **Catch Every Pokémon only asks for Cobblemon's own spawns.** A Pokémon that Cobblemon never spawns could still be needed to advance if another mod or datapack added a spawn for it, so Galar tier 4 could ask for a Kubfu you can only get from Legendary Monuments' Mustard. Only the wild spawns that come with Cobblemon now count, plus what they evolve into.

# CJP 2.9.0

## Changes
- **Gym leaders give real rewards.** Gym leaders, the Elite Four, the Champion Cup and Champions used to drop the same random loot as any trainer of their level, often just an X Attack. Each now gives a reward once, the first time you beat them, straight into your inventory: the TM it gives in the games (Rock Tomb from Brock, Stealth Rock from Roark, Magical Leaf from Milo) and a bundle that grows through the league, from Great Balls and Exp. Candy at the first gyms to Ultra Balls, Rare Candies, Nature Mints and items like an Ability Capsule or Lucky Egg later on. Champions give a Master Ball, an Ability Patch and a random Choice item, Leftovers, Life Orb, Exp. Share or Muscle Band.
- **Hard leaders give one more item.** Beating a leader's Hard version also gives a stone of its type (a Water Stone from Misty) or, for types without one, a type-boosting item (a Hard Stone from Brock). Champions give their held item: a Life Orb from Blue and Leon, a Choice Scarf from Cynthia. If you beat a leader on Normal first, a Hard rematch still earns it, once.
- **Rematches drop nothing**, so no reward can be farmed.
- Players who beat leaders before this update don't get their rewards automatically. An operator can hand them out with `/loot give` and `/advancement grant`; see "Leader rewards" in the docs for the commands.

# CJP 2.8.0

## Changes
- **Evolutions are allowed at gyms.** With `gymLegalityCheck` on, gym leaders now accept any Pokémon of a region you've opened, so the Pokémon you catch can evolve and still come along, even when the evolution doesn't spawn in the wild yet. A regional form counts as its own region's: a Kanto Pikachu evolved into an Alolan Raichu needs Alola. The Pokémon must also be within your level cap. The old rule, only Pokémon of tiers you've unlocked, is still there as `gymLegalityRule: "strict"` (Allowed Pokémon: Wild spawns on the Mod Menu General page).
- **Tier catches before their leaders.** In the Pokémon Journey tab, each league's tier catch line hung off its first badge, so "Kanto Tier 1 Caught" came after Brock, though Catch Every Pokémon makes you catch tier 1 before he'll battle you. The catch line now starts at the tab's root, as its own lane beside the league's badges: each tier sits in the same column as the leader it comes before (Tier 1 above the Boulder Badge). They're still hidden until earned. Progress isn't affected.

# CJP 2.7.1

## Fixes
- **The Road tab shows each leader's level.** The number next to each trainer on the Trainer Card's Road tab was the level cap you get after beating them, which read as the trainer's own level (Erika showed 36, Koga's level, though her team tops out at 29). It now shows the level of the trainer's strongest Pokémon on your difficulty, as "Tier 5 · Lv 29". Your current level cap is still on the Profile tab.
- **Tidier Pokémon Journey tab.** Each league's tier catch advancements ("Kanto Tier 2 Caught") branched off a different badge, and every league's Tier 1 off the tab's root, so the tree split at every step. They're now one line per league, under its badges: Tier 1 hangs off the first badge, and each tier off the one before. They're still hidden until earned, but earning one also shows the earlier tiers of its line. Progress isn't affected.

# CJP 2.7.0

## Features
- **Your starter comes from your region.** In region mode you now pick your starter after choosing where your journey starts, and only from that region's starters (and your second region's, if you picked it first). Choosing where your journey starts opens the starter choice right away; before that, the starter screen tells you to open your Trainer Card. Players who already have a starter keep it. Turn it off with `regionStarters` in `ucp-common.json` (Starters From Your Region on the Mod Menu General page).
- **Starters at level 5**, as in the original games, with the moves they know at level 5, instead of Cobblemon's level 10. Change it with `starterLevel` (Starter Level on the Mod Menu General page); `0` keeps Cobblemon's level. If another mod handles starters, CJP leaves them to it.
- **Unlocked Pokémon only at gyms (optional).** With `gymLegalityCheck` in `ucp-common.json` (or Unlocked Pokémon Only on the Mod Menu General page), gym leaders check that the Pokémon you bring are from tiers you've unlocked, since eggs, trades and fossils can bring others. `"warn"` names them when the leader accepts your challenge; `"block"` also stops the battle until they're in the PC. Your starter is always allowed. Off by default.

## Fixes
- The Trainer Card's first region choice was titled "Choose your second region", and the Regions tab's second-region pick "Choose your starting region". They're the right way round again.
- **Wild levels follow the right player.** A wild Pokémon's level came from the progress of whoever stood closest when it spawned, while whether it could spawn at all came from the player it spawned for. On a server, a Pokémon allowed by one player's progress could get its level from another's. Both now use the same player.
- The server log now warns when Radical Cobblemon Trainers isn't a version CJP was tested with (0.19.x), since CJP's hooks into it stop working quietly rather than crashing.
- CJP now requires Cobblemon 1.8.x, so a Cobblemon version it can't run on shows Fabric's "incompatible mods" screen instead of crashing.

# CJP 2.6.0

## Features
- **PokeBadges support.** With the PokeBadges mod installed, the 26 Kanto, Sinnoh and Galar badge advancements use PokeBadges' badge items as their icons, and earning one also gives you that badge for your Badge Box. Players who earned badges before PokeBadges was added get them when they next join, unless they've owned them before. PokeBadges is optional and not included: without it, nothing changes.
- **Tier catch advancements.** Each tier of a league now has a hidden advancement in the Pokémon Journey tab for catching every Pokémon of that tier, regional forms included ("Kanto Tier 2 Caught"). It hangs off the gym leader whose badge opens the tier (Kanto's tier 2 under Brock's Boulder Badge, tier 1 under the tab's root) and stays hidden until you earn it. The last tier's is a challenge, with a Master Ball icon. They count the same Pokémon as the Catch Every Pokémon option, but work whether it's on or off, and players who already caught a tier get its advancement when they next join or catch something. Regions without gyms don't have them.
- Datapacks: `scripts/generate-league-advancements.py` writes them, and the new trigger `ultimate-cobblemon-progression:catch_tier` (conditions `region` and `tier`) can be used in any advancement.

## Fixes
- **Cobblenav's Fishingnav lists only Pokémon you can fish.** The Fishingnav from Cobblemon PokeNav worked out what can bite with its own fishing rules, so it listed species from regions and tiers you haven't unlocked. It now shows the same Pokémon fishing actually gives you.

# CJP 2.5.2

## Fixes
- **Fishing follows your regions.** Pokémon fished up with a Poké Rod ignored region mode: any species could bite, whatever you had unlocked. Fishing now works like wild spawns: only Pokémon of your open regions and reached tiers bite, regional forms follow their own region, and fished Pokémon get the same spawn weights and levels as wild ones. If every Pokémon a cast could catch is still locked, nothing bites.
- **Mod Menu config pages no longer overlap.** Settings ran into each other (Give Trainer Card on First Join over Allow Two Leagues) and, in smaller windows or at larger GUI scales, into the Save and Cancel buttons or off the screen. Both pages now list one setting per row and scroll when the window is too short. The two-league level cap is now its own button, Two-League Level Cap.
- **Trainer Card buttons fit their labels.** The Road tab's Switch to Normal/Hard button was too narrow for its bold label, which spilled past its edges; the card's other buttons are now sized for their labels too, and a long league name on the Road tab's Show button is shortened instead of pushing the title aside.
- The gym choice and rematch menus keep a row's colours when a long name is cut short.

# CJP 2.5.1

## Features
- **Galar on Hard.** Galar's Gym Leaders, Champion Cup and Champion Leon now have Hard versions, so Galar's Road tab shows the difficulty switch like Kanto's and Sinnoh's. As for the other Hard leaders, each team is the Normal team plus one more Pokémon (six for Marnie, Hop, Bede and Leon), all at the Normal ace's level, with perfect IVs, EV spreads, held items, picked abilities and natures, and stronger moves: Nessa sets rain with Pelipper for her Swift Swim team, Kabu sun with Torkoal, Melony snow with Abomasnow and Raihan sand with Gigalith. On Hard you still beat both Bea and Allister at gym 4, and both Gordie and Melony at gym 6. Each Dynamaxes the same Pokémon as on Normal. Level caps and loot don't change.

# CJP 2.5.0

## Features
- **The Galar league.** Galar is now a league like Kanto and Sinnoh: pick it as where your journey starts, as your next region, or as your second league. Its Gym Leaders (Milo, Nessa, Kabu, Bea and Allister, Opal, Gordie and Melony, Piers and Raihan) unlock its tiers: gyms 4 and 6, which have a different leader in Sword and in Shield, have both, beaten one after the other for a badge each (10 badges in all), then the Champion Cup (Marnie, Hop and Bede) and Champion Leon complete it, each with an advancement in the Pokémon Journey tab. Teams, levels and moves are Sword's and Shield's, on Normal; Galar has no Hard versions yet, so on Hard its leaders are summoned at Normal, and its Road tab shows Normal without the difficulty switch. Bea's and Allister's teams are 4 levels below the games' (Machamp and Gengar at 32), so the level cap rises in steps from Kabu (27) to Opal (38) instead of jumping 9 levels. Radical Cobblemon Trainers has no Sword/Shield trainers, so CJP adds them, and they use RCT's default look. Galar's badges also count for the badge milestones the regions without gyms follow. With the Cobblemon: Mega Showdown mod, Galar's leaders Dynamax or Gigantamax their ace as in the games, and every Gym Challenge block is a Power Spot, so you can Dynamax in the gym too.
- **If you picked Galar as your second region**, it becomes your second league: you now work through its gyms from Milo, and its tiers open with its own badges instead of your other league's, so Galar's higher-tier Pokémon stop spawning until you earn them. Your level cap doesn't drop: starting a second league keeps the cap you had (see below).
- **Two leagues at once.** Your second region can now be another league with its own gym leaders, so you can work through Kanto and Sinnoh side by side. Pick it on the Trainer Card's Regions tab after choosing where your journey starts, or later while your second region is still free. It works exactly like your home league: its leaders unlock its tiers, and on Catch Every Pokémon each league needs its own tier caught. When both have a leader ready, a Gym Challenge block asks which one to battle. Your level cap is the average of the two leagues' caps, rounded up, so it rises whichever you play and most when you advance both. The two leagues are a pair: if one finishes first you carry on with the other, and once both are done you choose where your journey goes next (and can pick a second league again), with your level cap starting over as it does for a single league. You can have two regions at most: your home league and one second region, a league or a region without gyms. Starting a second league partway through never lowers your cap: you keep the one you had until the average catches up. As RCT does for everyone else, gym leaders won't battle you while a party Pokémon is above your cap. Completing your second league also counts as a completed series in RCT, for its loot luck bonus. Servers can make the cap the lower of the two leagues' instead, with `twoLeagueCap` set to `"lowest"`, or turn two leagues off with `allowTwoLeagues`, both in `ucp-common.json` (and on the Mod Menu General page).
- **Galar's wild trainers.** 147 trainers (221 battles) from Pokémon Sword & Shield and both DLCs, with their real teams: species, Galarian forms, levels, moves and held items. They spawn once Galar is open for you (as your home league, your next region or your second region) and fill a Galar list on the Trainer Card's Trainers tab. Rivals and story trainers are in (Hop, Bede, Marnie, Klara, Avery, Sordward, Shielbert, Peony, Oleana, Team Yell); gym leaders, the Champion and the Stadium battles aren't. Each class uses the closest RCT class for where it spawns, its loot and its skin, so RCT skin packs cover them. Nobody Dynamaxes, as on Sword & Shield's routes.
- Datapacks: `scripts/generate-galar-trainers.ps1` builds them from `scripts/data/galar-trainers.json`.

## Changes
- **Regional forms belong to their own region.** In region mode, Alolan, Galarian, Hisuian and Paldean forms are now Pokémon of their own region, at their own tier, instead of following their species. Alolan Rattata spawns once Alola reaches its tier, even if Kanto was never opened, and a player in Alola without Kanto only meets the Alolan form. Its levels and spawn weight come from its region. The built-in leagues datapack places each form in the tier that matches its Base Stat Total (Alolan Rattata in Alola's tier 1, Hisuian Typhlosion in Sinnoh's tier 8).
- **Catch Every Pokémon counts regional forms on their own.** Alolan Rattata is one of Alola's Pokémon to catch, and only a regular Rattata counts for Kanto's. The Trainer Card's Wild Pokémon tab lists each region's forms with their own caught marks and types.
- **Pokémon that always evolve into a regional form no longer spawn in region mode** (the Pikachu, Cubone, Cyndaquil, Goomy and others Cobblemon spawns with a region bias). Every regional form they lead to spawns in the wild itself. Evolving a normal Pikachu or Exeggcute in an Alola-style biome still gives the Alolan form.
- **A smoother level curve in Kanto.** FireRed/LeafGreen jump from Erika (level 29) straight to Koga and Sabrina (both 43), so the level cap rose 14 levels on one badge and then not at all on the next. Koga's teams are now 7 levels lower on both difficulties (his Weezing is level 36), and the level cap after Erika's badge is 36 instead of 43. His Pokémon and their moves are unchanged. With his strongest Pokémon now at level 36, his loot no longer includes the items that need a level-40 trainer (trade evolution items, Rare Candy, Ability Capsules and Patches, the Lucky Egg). Sinnoh had the same flat step (Maylene and Wake both at 30), so Maylene's teams are 3 levels lower (Lucario at 27) and Wake's 1 level higher (Floatzel at 31), with level caps of 27 and 31 after Gardenia's and Maylene's badges, for steps of 5, 4 and 5 levels from Gardenia to Fantina. Existing worlds get the new levels from the next leader a gym summons.
- **Hard leaders are a bit tougher.** On Hard, every Pokémon on a gym leader's, Elite Four member's or Champion's team is now at the level of their ace, instead of a few levels below it: Brock's whole team is level 14, Giovanni's level 50, Cynthia's level 66. Each trainer's highest level, which decides their loot, doesn't change, and Normal is untouched. Existing worlds get them from the next leader a gym summons.
- **The `lockRegionalForms` option is gone**, since regional forms now always follow their own region. Configs that still set it are ignored.
- **The Trainers tab counts each trainer once.** Trainers you can rematch at higher levels, and rivals with a copy of each battle for every starter, used to be listed once per version: Youngster Ben four times, Rival Terry twelve. They're now one row each, with their level range (Lv 12–48), beaten when you beat any version. Every version still spawns in the wild, so trainers keep turning up as your team gets stronger. Grunts are still one row per battle.
- **Kanto's wild trainers are FireRed's own, with FireRed's teams.** RCT's Kanto trainers come from Radical Red, a FireRed hack that rebuilds every team (harder, at higher levels, with about a quarter of the Pokémon from later games: Youngster Ben's Ekans was a Stunky, Bug Catcher Rick's Weedle a Wurmple). They now have FireRed's original teams, from pret's FireRed decompilation: species, levels, IVs, held items and moves, and FireRed's double battles. Radical Red's own additions no longer spawn in region mode: the Friendly Coaches and other tutorial trainers, its joke trainers, its post-game cameos like Brendan, May and Red, and the trainers it put in FireRed's unused slots ("Ace Trainer Janny"). A few FireRed trainers it gave made-up classes get their FireRed names back ("Dragon Tamer Ramiro" is Bird Keeper Ramiro). Kanto now lists 447 trainers and Sinnoh 483.
- Datapacks: an entry in `wild_trainers.json` can be a list of ids, the versions of one trainer.

## Fixes
- **Catch Every Pokémon can no longer take away a tier you've reached.** CJP now remembers the highest tier each player has reached in each region, and the catch rule only holds back tiers above it. Before, turning the option on in a world underway, or a datapack adding Pokémon to an earlier tier, could drop players back a tier. Existing worlds keep the tiers players had.
- Sinnoh's first-route trainers (with teams like Starly, Shinx and Pachirisu) were listed under Kanto, so they spawned for Kanto players and not for Sinnoh players. They're Sinnoh's now.

## Datapacks
- A tier file can list a regional form as a species id and its aspect, e.g. `"cobblemon:rattata alolan"`, with more aspects when several forms share one (`"cobblemon:tauros paldean blaze-breed"`). A form claimed by a region's `formAspects` that no tier file lists takes its species' tier number in that region.
- `scripts/add-regional-forms.py` adds every regional form to its region's tier files by Base Stat Total.

# CJP 2.4.0

## Features
- **Trainer levels on name tags.** Radical Cobblemon Trainers' trainers show their level next to their name, e.g. "Ace Trainer Kendra Lv 22": the level of their strongest Pokémon, which is what decides their loot. Only the name over their head changes, not chat or battle messages.
- **Reworked trainer loot** in the built-in leagues datapack. Every win now drops at least one item (early trainers used to drop nothing about 61% of the time), and rarer items get more common as trainers get stronger, instead of having the same odds at every level. Strong items need stronger trainers: evolution stones and TMs from level 26, trade evolution items, Rare Candy, Ability Capsules and Patches and the Lucky Egg from level 40, and Choice items, Life Orb and Leftovers from level 51. Vanilla filler like flowers, saplings, paintings, pottery sherds and armor trims is replaced by Poké Balls, apricorns and Exp. Candies. Radical Red's Team Rocket Boss Giovanni no longer drops a guaranteed Master Ball. Each trainer class keeps its own kinds of items. Datapacks: the tables are generated by `scripts/generate-trainer-loot.ps1`.
- **Wild trainers belong to regions.** In region mode, Radical Cobblemon Trainers' wild trainers only spawn for players who have their region open: Radical Red's trainers with Kanto, Brilliant Diamond/Shining Pearl's with Sinnoh. None spawn before a player picks where their journey starts, and Pokémon Unbound's never do (it has no CJP region). Turn the last part off with `blockUnlistedTrainers` in `ucp-spawn.json` (also on the Mod Menu spawn page).
- **Trainers tab on the Trainer Card**: every wild trainer of your open regions, weakest first, with its level and whether you've beaten it, and a count per region. **Hide beaten** shows only the ones left, and a region's header folds its list. CJP keeps this record itself from now on, so wins from before this update don't count; RCT respawns trainers you've beaten, so you can meet them again. Trainers that can never battle a CJP player (ones that need Radical Red's story battles first) aren't listed, so every region can be completed.
- Datapacks: a region can list its wild trainers in `wild_trainers.json`. The built-in lists are generated by `scripts/generate-wild-trainers.ps1`.

## Fixes
- **Baby Pokémon no longer go missing.** Toxel, Riolu, Pichu, Cleffa, Happiny, Smoochum, Igglybuff and others only spawn in herds led by their evolution (Toxel's by Toxtricity). While the evolution was still locked, CJP cancelled the leader and most of the herd after Cobblemon had picked them, so these Pokémon were very rare, and with Catch Every Pokémon on they could hold a tier back for a long time. Locked members are now left out before the herd is picked, so until Toxtricity unlocks, a Toxel herd is all Toxel.
- **No more gym leaders in the wild.** Radical Cobblemon Trainers' other leaders, Elite Four members and Champions still spawned naturally and could battle you: the rest of Radical Red (like Clair and its Elite Four), all of Unbound's, and the rematch and cameo versions RCT spawns like ordinary trainers (Sinnoh's leader rematches after you beat each leader, Radical Red's Johto leaders like Bugsy and Whitney). The built-in leagues datapack now turns their natural spawning off, as it already did for CJP's own leagues. Ordinary trainers spawn as before.
- **The Trainer Association no longer spawns in region mode.** RCT's Trainer Association NPC let players join another series, or switch to freeroam (level cap 100) after finishing a league, getting around the journey and its level cap. While region mode is on, CJP keeps it from spawning; without region mode, RCT's `spawnTrainerAssociation` setting applies as before.

# CJP 2.3.0

## Features
- **Gym difficulty: Normal or Hard.** Every leader, Elite Four member and Champion in the Kanto and Sinnoh leagues now has a Hard version at the same levels: an extra Pokémon or two for most leaders (6 for the Elite Four), perfect IVs, trained EVs, held items, movesets built as sets, a sharper AI and a 2-use bag. After picking where your journey starts, your Trainer Card asks you to choose; switch any time on its Road tab or with `/trainerlevel difficulty <normal|hard>`. It applies from the next leader a gym summons, either version earns the badge, and the Road tab marks leaders beaten on Hard. Servers can set it for everyone with `gymDifficulty` in `ucp-common.json` (also on the Mod Menu General page).
- **Gym rematches.** Sneak and use a Gym Challenge block with an empty hand to rematch any leader, Elite Four member or Champion you've beaten, on Normal or Hard, from any region on your journey. Rematches change no progress, and Radical Cobblemon Trainers' series, trainer order and level cap don't block them.
- Datapacks: a tier slot can list a Hard version of its trainer as `<id>_hard`. Beating it also marks the Normal trainer as beaten in Radical Cobblemon Trainers, so RCT's level cap and trainer order carry on, and it uses the Normal trainer's look and dialog.

## Changes
- **Normal leaders are a little stronger, as in the original games.** Their Pokémon used to have 0 IVs. They now have equal IVs that rise through the journey (6 at the first gym up to 24 at the eighth, 28 for the Elite Four and 31 for the Champion), still with no EVs. Kanto's leaders also use the sharper AI setting and 2-use bag limit Sinnoh's already had. Existing worlds get them from the next leader a gym summons.
- **Regional forms need their own region open.** In region mode, Alolan, Galarian, Hisuian and Paldean forms no longer spawn just because their species can: Alolan Rattata also needs Alola open, Galarian forms need Galar, Hisuian forms Sinnoh, and Paldean forms Paldea. The same goes for Pokémon that Cobblemon spawns ready to evolve into a regional form (like Pikachu that become Alolan Raichu). This also stops a Galarian Farfetch'd caught early from evolving into Sirfetch'd before Galar opens. Forms keep their species' tier and levels, and still count as their species for the Pokédex and for catching every Pokémon. Turn it off with `lockRegionalForms` in `ucp-spawn.json` or on the Mod Menu spawn page.
- Datapacks: a region's `region.json` can claim forms with `formAspects` (e.g. `["alolan", "region-bias-alola"]`).

# CJP 2.2.0

- **Renamed to Cobblemon Journey Progression (CJP)**, with a new icon. Only the name changed: the mod id (`ultimate-cobblemon-progression`), config files, datapack namespace and API stay the same, so existing worlds, configs and datapacks keep working. The jar is now `cjp-fabric-<version>+mc1.21.1.jar`.

## Fixes
- With Legendary Monuments installed, beating a Radical Cobblemon Trainers trainer (gym leaders included) no longer crashes the game. Four of its victory scripts treated every trainer as a Cobblemon NPC; CJP now replaces them with copies that skip other mods' trainers.
- The Trainer Card's Pokémon lists showed seen Pokémon with the caught ball and caught ones with the seen outline. They're now the right way round.
- Long text on the Trainer Card (the next-step hint, region and trainer names, tab labels, confirmations) now shrinks to fit, and wraps inside its box, instead of running off the edge or being cut off.

# UCP 2.1.0

## Features
- **Sinnoh league** in the built-in datapack (now called UCP Kanto & Sinnoh Leagues; its id is still `kanto_league`): Sinnoh's 10 tiers are unlocked by RCT's Brilliant Diamond/Shining Pearl gym leaders, Elite Four and Champion Cynthia, in a new `sinnoh` series that only appears in gyms. You can start your journey in Sinnoh, or travel there after Kanto.
- **Choose your next region**: once every region on your journey is complete, your Trainer Card asks which league region to travel to next (or use `/trainerlevel region next <region>`). Picking one opens it at its first tier, joins its RCT series, and gyms summon its leaders. Once your journey has started, only its regions open: completing a region no longer opens the next one in order, and regions without a `series` stay locked unless picked as your extra starting region.
- **Pokémon Journey advancements**: UCP has its own advancement tab, with every league's badges, Elite Four and Champion (e.g. Boulder Badge with Brock's Hard Stone as its icon). The leagues' tiers unlock through them, and the extra regions through hidden badge milestones (your n-th badge in any league). They're earned with a new trigger, `ultimate-cobblemon-progression:defeat_trainer`, fired on every Radical Cobblemon Trainers win, and generated from the region data by `scripts/generate-league-advancements.py`.
- Beating a UCP league trainer no longer awards any of RCT's own advancements (its badge, "Defeat any trainer", its Champion counts); only UCP's.
- **Spawn pools**: while you work through a region, wild Pokémon come from three pools: the new region (most common), finished regions' Pokémon within your tier at the new region's levels, and finished regions' Pokémon at their full levels (rarest). You can catch Pokémon for your new team from every region you've finished, and nothing you've unlocked stops spawning. Weights are `regionPoolWeight`, `pastInCapPoolWeight` and `pastFullPoolWeight` in `ucp-spawn.json`; `doRegionPools` turns it off.
- **Catch every Pokémon** (`catchWholeTier` in `ucp-common.json`, off by default): to reach a region's next tier, you also have to catch every obtainable Pokémon of your current tier there. Gyms won't summon a league's next leader until you have; your extra starting region waits at its tier until it's caught, without holding the league back. Pokémon that can't be obtained (no wild spawn and nothing evolves into them, like most legendaries) are skipped.
- Optional gym team limit (`gymTeamLimit` in `ucp-common.json`, off by default): players can bring no more Pokémon to a gym battle than the leader has. The leader says how many when it accepts the challenge, and a battle with a bigger party doesn't start.
- The Mod Menu config's General page now also has the gym team limit, the share of villages with a gym, and the first-join Trainer Card.
- `/trainerlevel get` marks your current region and says why each locked region is locked.
- Gyms tell you to pick your next region when you've completed your current one.
- Tier requirements can list alternatives: an `advancements` entry can be a list, where completing any one counts.
- `scripts/generate-region-tiers.py` replaces the PowerShell version: it writes "any one of" requirements (`a|b`) and keeps each tier's gym leaders, level cap and requirements unless you pass new ones.
- Config files are rewritten on load, so options added in a new version show up in them (your own values are kept).

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
