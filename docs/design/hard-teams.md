# Hard teams

How the Hard version of a gym leader, Elite Four member or Champion is built. Use it when you make a Hard team for a new region, or change an existing one. The review of the current teams against these rules is in [hard-teams-review.md](hard-teams-review.md).

Where the teams live:

- **Kanto and Sinnoh:** hand-written files in the built-in pack, `src/main/resources/resourcepacks/kanto_league/data/rctmod/trainers/<id>_hard.json`, next to the Normal `<id>.json`.
- **Galar:** the `HARD` table in `scripts/generate-galar-league.py`, which writes the same files. It checks every species, form, move, ability and item against the Cobblemon jar, and refuses a Hard team that leaves out a Normal Pokémon. Kanto's and Sinnoh's files have no such check, so check them by hand against these rules.

## Words used here

- **Ace:** the trainer's signature Pokémon, normally the last and highest-levelled on the Normal team (Brock's Onix, Cynthia's Garchomp). The fight is built around it.
- **BST (base stat total):** the sum of a species' six base stats. A rough measure of how strong a species is: Geodude 300, Alakazam 500, Garchomp 600.
- **STAB:** a move of the Pokémon's own type, which does 1.5× damage.
- **Coverage:** moves of other types, so the Pokémon can hit the things that resist its own type.
- **Weak spot:** a type that hits at least half of the team super-effectively. A Rock gym's weak spots are Water, Grass, Fighting, Ground and Steel.
- **Setup move:** a move that boosts the user's stats (Swords Dance, Calm Mind, Dragon Dance, Shell Smash, Quiver Dance, Nasty Plot).
- **Win condition:** the Pokémon that can beat the player's whole team if left unchecked, usually a strong attacker with a setup move.
- **Hazards:** moves that hurt the player's Pokémon each time one switches in (Stealth Rock, Spikes, Toxic Spikes, Sticky Web).
- **Pivot:** a move that attacks and then switches out (U-turn, Volt Switch, Parting Shot), to bring in a teammate safely.
- **Priority:** a move that goes first whatever the speed (Quick Attack, Bullet Punch, Sucker Punch, Ice Shard).

## The fixed part

Every Hard team has these, whatever the trainer:

| | Hard |
|---|---|
| Team | The Normal team, plus one Pokémon (two when the Normal team is tiny, like Misty's). The Elite Four, the Champion Cup and Champions have 6. |
| Levels | Every Pokémon at the Normal ace's level. |
| IVs | 31 in every stat. |
| EVs | One of six spreads, by the Pokémon's role (below). |
| Nature | One that boosts the stat the role uses and lowers one it doesn't (Adamant for a physical attacker, Timid for a fast special one, Bold for a physical wall). |
| Ability | Picked for the set, from the species' own abilities (hidden ones allowed). |
| Held item | Fitting the point in the journey: berries, Eviolite and type boosters early; Leftovers, Life Orb, Focus Sash and Choice items later. |
| Moves | Four, built as a set (below). Every move must be in the species' Cobblemon learnset. |
| Bag | 2 Super Potions, Hyper Potions or Full Restores, by the point in the journey; 2 uses per battle. |
| AI | Sharper than Normal (`maxSelectMargin` 0.05 instead of 0.15). |
| Galar | Dynamaxes the same Pokémon as the Normal team. |

The EV spreads (`EV_SPREADS` in the Galar script, and the same numbers in the Kanto and Sinnoh files):

| Spread | Stats | For |
|---|---|---|
| `atk` | Attack 252, Speed 252 | Fast physical attackers |
| `spa` | Sp. Atk 252, Speed 252 | Fast special attackers |
| `hpatk` | HP 252, Attack 252 | Slow physical attackers |
| `hpspa` | HP 252, Sp. Atk 252 | Slow special attackers |
| `def` | HP 252, Defense 252 | Walls against physical hits |
| `spd` | HP 252, Sp. Def 252 | Walls against special hits |

## Building the team

Go through these steps in order. Steps 3 to 6 are where most of the judgement is.

### 1. Start from the Normal team

Every Normal Pokémon stays on the Hard team, at the ace's level. Two exceptions:

- A Normal Pokémon may be **evolved** (Agatha's Golbat becomes Crobat, Bruno's Onix becomes Steelix), if the evolution is allowed in the region (step 2).
- A **duplicate** on the Normal team (Bruno's two Onix, Agatha's two Gengar) may be replaced by a different Pokémon, so the Hard team doesn't repeat itself.

Leaving out a Normal Pokémon any other way breaks the Hard version's promise: "the same fight, harder". It also tends to remove the one off-type Pokémon the original game gave the leader.

### 2. Check availability

The added Pokémon must belong to the region:

- **Kanto:** a Gen 1 species, or one of a Gen 1 species' family added later (Crobat, Steelix, Kingdra, Bellossom, Hitmontop, Scizor). A species from an unrelated later family is allowed only if the character uses it, or its family, in an official game (FireRed/LeafGreen's Elite Four rematch gives Lorelei Piloswine and Agatha Misdreavus, so Mamoswine and Mismagius are fine for them).
- **Sinnoh:** any Gen 1 to 4 species.
- **Galar:** any species in Sword and Shield's Galar Pokédex, including the Isle of Armor and Crown Tundra.

It must also be implemented in Cobblemon.

### 3. Find the team's weak spots

For each of the 18 types, count how many team members it hits super-effectively. Count abilities that change this: Levitate is immune to Ground, Flash Fire to Fire, Water Absorb and Storm Drain to Water, Volt Absorb, Lightning Rod and Motor Drive to Electric, and Thick Fat halves Fire and Ice.

A type that hits **half the team or more** is a weak spot. If a weak spot has **nothing on the team that resists it**, one Pokémon of that type can beat the whole fight alone. That is the main thing the added Pokémon should fix.

A gym of one type will always share that type's weaknesses: every Fire gym is weak to Water. That's fine and expected. The goal isn't to remove the weakness, it's to make sure the player can't win with one Pokémon and nothing else.

### 4. Pick the added Pokémon

In order of preference, it should:

1. **Patch the worst weak spot by its typing.** It resists or is immune to that type, and doesn't share the other weak spots. Example: Wake's team is weak to Electric, and Quagsire (Water/Ground) is immune to it.
2. If the gym's type makes that impossible (no Fire type resists Water), **carry moves that punish the Pokémon that exploit the weak spot.** Example: Blaine's Ninetales sets sun, which halves Water damage, and uses Solar Beam against the Water and Ground Pokémon that come in.
3. **Have a job that helps the ace.** Typical jobs:
   - setting the field: weather for the team's abilities, hazards, screens, Trick Room
   - wearing the player down: burn, paralysis, Toxic, hazards
   - taking out what threatens the ace, or soaking its weakness
   - pivoting, to bring the ace in safely

   Example: Nessa's Pelipper sets rain with Drizzle, which doubles the speed of her Swift Swim team, ace Drednaw included.
4. **Fit the character.** Same theme or a secondary type, or a Pokémon the character uses in another game (Roark is a fossil collector, so a fossil fits him).

Don't add a Pokémon that only adds another copy of the team's weak spots and has no job (another pure Rock type to a Rock gym).

### 5. Keep the ace the ace

The added Pokémon must not outshine the ace:

- Its BST is at or below the ace's, unless it's a wall or a support Pokémon with no real damage (Bruno's Steelix is 510 to Machamp's 505, but sets Stealth Rock and roars).
- It's not a stronger win condition than the ace. If the ace is the team's setup sweeper, the added Pokémon doesn't set up and sweep too.
- The ace keeps the best item for its job (Life Orb, Choice item or Focus Sash), and moves last.

The exception is an ace that is weak for its point in the journey. Then the added Pokémon can be stronger, as long as the fight still ends on the ace.

### 6. Build each set

For every Pokémon on the Hard team, not only the added one:

- **A role first:** attacker, wall, setter or pivot. The role picks the EV spread and the nature.
- **Four moves:** at least one STAB attack; coverage for the types that wall it; and one or two moves for the role (a setup move for an attacker, recovery and a status move for a wall, a hazard for a setter). The moves the original game gave it are a good start.
- **An item**, unless the set wants none: Acrobatics doubles its power without an item, and Pickpocket and Unburden need an empty hand. Items should fit the set (Eviolite only on a Pokémon that can still evolve, a type booster that matches its main attack).
- Natures, abilities and items can repeat on a team; there's no item clause.

### 7. Keep it fair

Hard is a harder fight, not a lucky one. Not allowed:

- One-hit KO moves (Fissure, Sheer Cold, Horn Drill, Guillotine).
- Evasion boosts (Double Team, Minimize) and accuracy drops as a plan (Smokescreen, Sand Attack, Mud-Slap spam).
- More than one move that puts the player to sleep per team, and sleep on the same Pokémon as a setup move (Lovely Kiss with Nasty Plot): the player can't answer it.
- Paraflinch: paralysis together with Serene Grace Air Slash or Iron Head on one team, which makes most of the player's turns a coin flip.
- Moody, and trapping abilities (Shadow Tag, Arena Trap) on a Pokémon that then sets up or stalls. Arena Trap on a fast attacker that hits hard is fine (Giovanni's Dugtrio).

Allowed and good: priority moves, hazards, weather, screens, Trick Room, Explosion, Destiny Bond, Focus Sash, status moves used once.

### 8. Check it

- Every species, form, move, ability and item exists in Cobblemon and the species can learn every move. The Galar script does this itself; for Kanto and Sinnoh, check against the Cobblemon jar's `data/cobblemon/species/` files.
- Every Normal Pokémon is on the Hard team (step 1), at the ace's level.
- Count the weak spots again with the finished team (step 3).
