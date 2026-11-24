# Design: two regions at once

Status: approved and built (unreleased). Roadmap item: *Two regions at once*.

## Goal

A player can work through **two league regions side by side** (Kanto and Sinnoh in the built-in data) to speed the game up:

- They can pick both when they start, or add the second one later.
- The second league progresses **exactly like the first**: its own gym leaders unlock its tiers, and on Catch Every Pokémon each league needs its own current tier caught before its next leader.
- When both leagues have a leader ready, the **Gym Challenge block asks which one to battle**.

## How it works today

- **Journey:** a player picks one **home** league on the Trainer Card. After finishing it, they pick the **next** league. Only one league is ever in progress (`RegionProgress.current()`).
- **Extra starting region:** one optional extra region, but only a **non-league** one (Johto, Hoenn, …). It has no leaders. Its tiers follow hidden badge milestones ("your 3rd badge in any league").
- **Gym Challenge block** (`GymLeaderPicker`): it walks the open regions in priority order and summons the first unbeaten trainer it finds. If the first region is waiting on catches, it stops there.
- **Radical Cobblemon Trainers** keeps **one series per player**. It uses that series for:
  - **the level cap:** `PlayerState.getLevelCap()`, the ace level of the next trainer in the series;
  - **leader battle rules:** a trainer of another series, or whose required trainers aren't beaten in this series, won't battle.

  Switching series wipes RCT's series progress. Defeat counts are kept for every trainer, whatever the series.
- **CJP's own records don't depend on RCT's series.** Tiers unlock from CJP's own advancements (`defeat_trainer`), and each league tier's `levelCap` is already the next leader's ace, the same number RCT uses.

So the blocker is RCT's single series, not CJP's region model.

## Design

### 1. A second league slot

- The **extra starting region** may now also be a **league** region. `RegionRegistry.startChoices()` stops filtering leagues out. The Trainer Card's start screen offers it right after the home pick: "Add a second league", or "Add a region without gyms", or skip.
- The player has up to **two active regions**: their current journey region and their second region. `RegionProgress` gets `active()` (one or two regions) in place of `current()`. `current()` stays for the code that needs a single region, and means the first active league (journey first).
- **The two leagues are a pair.** If one is completed first, the player carries on with the other. Once both are done, they pick the next region (the Trainer Card's existing "where next?" screen) and may pick a second league again: the finished second league joins the journey's history, so it stays open, and the second region is free. Their level cap starts over with the new region, as for a single league, so a new region never starts overlevelled, which is what the spawn pools are built around. (Replacing one league while the other carried on was the first plan; it left the new league starting at the old league's levels, so it was dropped.)
- A **non-league** second region keeps working exactly as today, through badge milestones. Players and worlds that already have one are unchanged.

### 2. The Gym Challenge block offers a choice

- `GymLeaderPicker.pick` returns **every** active league's next leader, each as ready or blocked (with the reason: catches missing, other requirements).
- **One ready:** it's summoned, as today.
- **Two ready:** a small screen lists both, with region, leader, ace level and difficulty, and the player picks one. It's built like the existing rematch screen (`RematchScreen`, `RematchPayloads`).
- **None ready:** the message lists why for each league ("Kanto: catch 3 more tier 4 Pokémon. Sinnoh: beat Gym Leader Wake's requirements…").
- Sneak-use for rematches is unchanged.

### 3. CJP takes over RCT's league rules

RCT stays in one series, the primary league's, so its wild trainers and other behaviour are untouched. For league trainers, CJP replaces the two series-based rules:

- **Battle rules:** a leader summoned by a Gym Challenge block (already tagged `LEADER_TAG`) battles whatever the player's RCT series and series progress. CJP has already checked the order before summoning. This uses the same `canBattleAgainst` hook as `RctRematchBattleMixin`, and keeps its other checks (cooldown, not already in battle, a Pokémon able to fight).
- **Level cap:** a mixin on `PlayerState.getLevelCap()` returns CJP's cap, the average of the two leagues' caps, while the player has two leagues active (see below). With one league, RCT's own value is used, so nothing changes for single-league players.
- **Hard leaders:** marking the Normal version as beaten (`RctBridge.addProgress`) only makes sense in the player's RCT series. For the other league it's skipped, since CJP's records already count either version.

### 4. Spawns, catches and the Trainer Card

- **Spawns:** both leagues are already "in progress", so their Pokémon come from the **region pool**. The **past-in-cap pool** (finished regions at your current levels) uses the first active league.
- **Catch Every Pokémon:** already per region, so each league is checked on its own. Only `GymLeaderPicker`'s early return changes.
- **Trainer Card:** the header and hint show both leagues ("Kanto · tier 4" and "Sinnoh · tier 3"). The Road tab gets a toggle between the two leagues. The Regions tab already lists every region.

### 5. Level caps when the two leagues differ

This is the open question. Each league has its own next-leader level, but a Pokémon can't belong to one league, so the player needs **one** cap.

Both built-in curves are close, so the gap only grows if a player rushes one league:

| Badges | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|---|
| Kanto next ace | 14 | 21 | 24 | 29 | 36 | 43 | 47 | 50 | 54 (E4) |
| Sinnoh next ace | 14 | 22 | 27 | 31 | 36 | 39 | 42 | 49 | 57 (E4) |

| Option | Cap is | Good | Bad |
|---|---|---|---|
| **A. Highest** | the higher of the two next-leader levels | Never blocks you, fits the "speed up" goal, simplest | Rush Kanto to 6 badges (cap 47) and Sinnoh's 3rd gym (Maylene, ace 27) is a walkover |
| **B. Lowest** | the lower of the two | Both leagues stay challenging | Rushing one league stops all XP, so you have to alternate. That slows the game, the opposite of the goal |
| **C. Highest, with catch-up leaders** | like A, but the lagging league's leaders are raised to near your cap (e.g. no lower than cap − 5) | Fast and both leagues stay a fight | Most work: leader levels set when the leader is summoned, and it overrides the hand-tuned levels |

| **D. Average** | the average of the two next-leader levels, rounded up | Rises whichever league you play, but rises most when you advance both, so it rewards alternating without forcing it | Rushing one league leaves you below its next leader: at 6 Kanto badges and 0 Sinnoh badges the cap is 31, facing Blaine's 47 |

**Decision: D, the average.** It keeps both leagues moving without blocking a player who prefers one.

| Kanto / Sinnoh badges | Kanto next ace | Sinnoh next ace | Cap |
|---|---|---|---|
| 0 / 0 | 14 | 14 | 14 |
| 2 / 2 | 24 | 27 | 26 |
| 4 / 4 | 36 | 36 | 36 |
| 6 / 0 | 47 | 14 | 31 |
| 6 / 6 | 47 | 42 | 45 |

Rules that make it work in every case:

- **A league's cap** is the level of its next unbeaten trainer's strongest Pokémon (what RCT uses), never below its current tier's `levelCap`.
- **With one league in progress** (the other finished first, or a player with a single league), the cap is that league's own. A finished league isn't averaged in, so finishing one never makes the cap jump.
- **With both complete,** 100, until the player picks their next region. Then the cap starts over, as for a single league.
- **A one-off floor:** when a second league starts partway through a journey (or a picked region becomes a league in an update), the cap doesn't drop below the single-league cap the player had. Once the average reaches it, the floor is dropped for good, and it's cleared when the next round starts.
- **Rounded up**, so 30.5 is 31.

## Existing worlds

- Players with one league and an optional non-league region are unchanged.
- A player whose extra region slot is still free can add a second league later from the Trainer Card. This only works while the slot is free, so it never replaces a region they already picked.

## Decisions

1. **Level cap:** the average of the two leagues' caps, rounded up (see section 5).
2. **Two regions at most**, with or without gyms: a home league plus one second region, which is either a league or a region without gyms. Never three.
3. **Server option:** `allowTwoLeagues` in `ucp-common.json` (and on the Mod Menu General page), on by default. Off, the second region can only be one without gyms, as today.
