# Roadmap

Planned features for Cobblemon Journey Progression. Nothing here is promised for a specific release, and the order isn't a priority ranking. Finished items move to the [Changelog](../CHANGELOG.md).

## Planned

- [ ] **Badges mod integration.** Support a badges mod, and make the advancement plaques show the real badge textures.
- [ ] **Regional Pokédex advancements.** Add an advancement for completing each tier of a region's Pokédex.
- [x] **Hard mode leader levels.** In hard mode, raise every gym leader Pokémon to the level of the leader's highest-level Pokémon. Done for every Hard gym leader, Elite Four member and Champion.
- [ ] **Route-based unlocks (optional).** For players who want it closer to the official games: a setting that unlocks Pokémon by the routes you'd have reached before each gym, instead of by their stats.
- [ ] **Team showcase on the Trainer Card.** Show the exact team you used to beat each gym leader, so your card becomes a record of your whole journey.
- [ ] **Spawn-reducing item for catch-all challenges.** An item that lowers the spawn rate of Pokémon you've already caught, for players on the "catch every Pokémon in a tier" difficulty challenge.
- [x] **Two regions at once.** Done, see [the design](design/two-regions.md). Rework the secondary region so you can fight through and complete two regions side by side, to speed up the game a little.
  - The Gym Challenge block checks whether the player has two regions active and which of their next gym leaders can spawn. If both can, the player chooses which one to battle.
  - The secondary region progresses just like the primary one. Each region's requirements are checked on their own: on the "catch every Pokémon in a tier" challenge, the secondary region needs both its tier caught and its gym leader beaten before it advances, exactly like the primary region.
- [ ] **Starters at level 5.** Give players their starter Pokémon at level 5 instead of level 10, as in the original games. CJP doesn't set the starter level today, so this means finding where the level 10 comes from (Cobblemon's starter list, which servers can export with `exportStarterConfig`, or another mod) and overriding it.
- [ ] **Fossils.** Decide how fossil Pokémon should work with tiers, regions and the "catch every Pokémon in a tier" challenge.
  - Today, most fossil Pokémon (Omanyte, Kabuto, Aerodactyl, Lileep, Anorith, Cranidos, Shieldon, Tirtouga, Archen, Tyrunt, Amaura) also spawn in the wild in Cobblemon 1.8.4, so they spawn by their tier like any species and count as catch targets. Galar's fossils (Dracozolt, Arctozolt, Dracovish, Arctovish) have no wild spawn, so the catch-all challenge skips them.
  - Reviving a fossil isn't restricted at all: a revived Pokémon ignores tiers and regions, as eggs and trades do.
- [x] **Regional forms in spawns and the catch-all challenge.** Done, see [the design](design/regional-forms.md). Decide how regional forms count towards spawn rates and towards the Pokémon you need to catch for the "catch every Pokémon in a tier" challenge.
  - Ideally, if the player hasn't unlocked the original form's home region, only the regional form spawns. That way the regional form also counts towards that tier's total of Pokémon to catch.
