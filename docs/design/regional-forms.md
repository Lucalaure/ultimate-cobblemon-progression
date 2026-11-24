# Design: regional forms as their own entries

Status: approved and built (unreleased). Roadmap item: *Regional forms in spawns and the catch-all challenge*.

## Problem

Today a regional form is judged as its base species, plus a lock:

- Alolan Rattata is a **Kanto tier 1** species, because Kanto's tier files list `cobblemon:rattata` and a species belongs to the first region that lists it (`RegionSpecies`).
- With `lockRegionalForms` on, it also needs Alola open (`RegionalForms.isAllowed`).
- Spawning needs **both**, so a player who starts in Alola without Kanto never meets Alolan Rattata.
- Its levels and spawn weight come from Kanto's pool (`TierStanding`).
- The catch-all challenge (`TierCatches`) counts whole species. Catching Alolan Rattata ticks off Kanto's Rattata, and Alola's tiers can never ask for it.

## Goal

A regional form behaves like a Pokémon of its form's region:

- It spawns when **its form region** has reached its tier, whether or not the base species' region is open.
- The base form spawns only when **its own region** allows it. A player in Alola without Kanto meets only Alolan Rattata. A player with both meets both, wherever Cobblemon's spawn data puts each.
- Its levels and pool weight come from its form region.
- On the catch-all challenge, it is a catch target in its form region's tier. The base form stays a target in its own region, and catching one form doesn't tick off the other.

## Design

### 1. Form entries in tier files

A region's tier files may list a form as well as a species, with Cobblemon's property syntax:

```json
{
  "levelCap": 15,
  "species": [
    "cobblemon:yungoos",
    "cobblemon:rattata alolan",
    "cobblemon:meowth alolan"
  ]
}
```

- The loader splits an entry into species id + aspect. A plain id is the base form, as today.
- A form entry belongs to the region that lists it, at that tier. The existing "first region wins" rule applies per (species, aspect) pair, not per species, so Kanto's `rattata` and Alola's `rattata alolan` don't conflict.
- `formAspects` in `region.json` stays, as the fallback for a form that no tier file lists: that form uses its base species' tier number in the form region. This keeps third-party datapacks working without listing every form.
- The built-in league datapack gets explicit form entries for every Alolan, Galarian, Hisuian (Sinnoh) and Paldean form, placed by the form's own Base Stat Total, like the species.

### 2. Region-bias spawns are blocked; biome evolutions stay

Pokémon such as `pikachu region_bias=alola` or `cyndaquil region_bias=hisui` look like the normal form but always evolve into a regional form.

- **Their wild spawns are always blocked.** Every regional evolution they lead to (Alolan Raichu, Marowak and Exeggutor; Galarian Weezing and Mr. Mime; Hisuian Typhlosion, Samurott, Decidueye, Lilligant, Goodra, Sliggoo, Avalugg and Braviary) has its own wild spawn in Cobblemon 1.8.4, at about the same rarity. Nothing becomes unobtainable, and these Pokémon need no tier.
- The normal forms (Pikachu, Cubone, Cyndaquil…) are separate spawn entries and keep spawning by their own region's rules.
- **Biome evolutions are kept.** Cobblemon lets a normal Pikachu or Exeggcute evolve into its Alolan form when evolved in an Alola-style biome. CJP doesn't block evolutions, so a Kanto player can get Alolan Raichu this way before opening Alola. On the catch-all challenge it then counts for Alola once Alola is open, since catch checks read the Pokédex.
- `region-bias-*` aspects stay in `formAspects`; they're how these spawns are recognised and blocked.

### 3. One lookup for "what is this Pokémon?"

Add `RegionEntry resolve(Species, Collection<String> aspects)`, returning region, tier and whether it is a form entry. Every place that now uses `RegionSpecies.region(species)` + `RegionalForms.isAllowed` for a *Pokémon* (not a species) goes through it:

- `TrainerLevelInfluence.affectSpawnable`, `isHerdMemberAllowed`, `onEntitySpawn`, `affectWeight`, `affectSpawn` (level). `affectWeight` and `affectSpawn` currently look only at the species, so a form's weight and level come from the base species' region. Both need the spawn detail's or entity's aspects.
- `TierStanding.of(player, species)` gets an overload taking the resolved entry, so pools and level caps use the form region.

`RegionalForms.isAllowed` and the `lockRegionalForms` option become redundant. The proposal is to remove the option: with the form judged by its own region, there's no unlocked case left to opt into. A config that still has it set is just ignored.

### 4. Catch-all challenge

- `RegionDef.species(tier)` becomes a list of entries (species + optional form name). `targets` and `missing` work on entries.
- **Caught check per form.** Cobblemon 1.8 keeps a `FormDexRecord` per form (`SpeciesDexRecord.getFormRecord(formName).getKnowledge()`). A form entry checks its form record ("Alola", "Galar", "Hisui", "Paldea"). A base entry checks the base form's record ("Normal"), so catching Alolan Rattata no longer counts for Kanto's Rattata. The aspect-to-form-name mapping comes from the species' `FormData` (aspects → name), not a hardcoded table.
- **Obtainable per form.** `TierCatches.compute()` collects (species, aspects) from spawn details, and follows evolutions using each result's aspects. A region-bias Pikachu then makes Alolan Raichu obtainable. A form with no wild spawn and no evolution path is skipped, as species are today.
- The Trainer Card's "still to catch" list and `/region` command show form names ("Alolan Rattata").

### 5. Trainer Card Pokédex

The card's caught/seen markers (`TrainerCardDataBuilder` ~line 232) list form entries under their form region, with their form's own caught state.

## Existing worlds

- Most players caught the base form, so moving to per-form checks rarely changes anything. A player who caught **only** the Alolan form for a base-region target would now see that target as missing.
- **This can push players back a tier.** `RegionProgress.fill` keeps no stored tier: it works out each region's tier from scratch on every refresh (Pokédex change, login, commands), stopping at the first tier whose previous tier isn't fully caught. New catch targets, Alola's new form entries included, would drop a player who is mid-region back to the tier before the gap.
- Fix as part of this change: on the catch-all challenge, the catch check applies only to advancing *past* the highest tier the player has already reached. Store the reached tier per region, and keep `max(stored, computed)`. Beaten gym leaders already gate tiers through advancements, so storing the reached tier doesn't let anyone skip a leader. The same regression already happens today whenever a datapack adds species to an earlier tier, so this fixes that too.

## Decisions

1. **Form tiers:** each form is placed by its own Base Stat Total.
2. **`lockRegionalForms`:** removed. A config that still sets it is ignored.
3. **Region-bias spawns:** always blocked; regional evolutions come from their own wild spawns.
4. **Biome evolutions:** kept, so regional forms can be reached early by evolving in the right biome.
