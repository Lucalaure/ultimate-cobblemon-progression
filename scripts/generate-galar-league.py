"""Builds the Galar league (Normal and Hard) into the built-in leagues datapack.

Usage:
    python scripts/generate-galar-league.py [--pack DIR] [--cobblemon-jar JAR]

Radical Cobblemon Trainers has no Sword/Shield trainers, so CJP ships its own: the eight Gym Leaders' gym battles and
the Champion Cup (Marnie, Hop and Bede, then Champion Leon), with their teams, levels and moves from Pokémon Sword and
Shield. Gyms 4 and 6 have a different leader in each version (Bea or Allister, Gordie or Melony); here they have both,
and a player beats both, in that order, to move on. Bea's and Allister's teams are 4 levels lower than the games', so
the level cap doesn't jump 9 levels after Kabu. Like Kanto's Normal leaders, every Pokémon
has six equal IVs rising through the journey (6 at the first gym up to 24 at the eighth, 28 in the Champion Cup and 31
for the Champion), no EVs and no held item, and abilities are each species' first one. Natures follow the Pokémon's
stronger attack stat (Adamant or Modest).

Each trainer also has a Hard version ("<id>_hard", see HARD): the Normal team plus one more Pokémon, all at the Normal
ace's level, with 31 IVs, EV spreads, held items, picked abilities and natures, and stronger moves.

It writes, under the pack's data folder:

- rctmod/trainers/<id>.json and <id>_hard.json: each trainer's Normal and Hard team;
- rctmod/mobs/trainers/single/<id>.json: RCT's trainer entry (type, the "galar" series, the trainer it requires);
- rctmod/series/galar.json: the series;
- the "trainers" and "levelCap" of each of Galar's tier files (each tier's cap is its next trainer's strongest
  Pokémon), and "series" in its region.json.

Every species, form, move, ability and item is checked against the Cobblemon jar first, and nothing is written if
one doesn't exist. Run scripts/generate-league-advancements.py afterwards for Galar's badges and tier requirements.
"""
import argparse, glob, json, os, re, sys, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_PACK = os.path.join(HERE, "..", "src", "main", "resources", "resourcepacks", "kanto_league", "data")
SERIES = "galar"

# (id, name, mob type, IVs, bag, signature item, female trainer, team)
# Team entries: (species, level, moves) or (species, level, moves, aspect).
TRAINERS = [
    ("gym_leader_milo_8a01", "Gym Leader Milo", "leader", 6, [("potion", 1)], "miracle_seed", False, [
        ("gossifleur", 19, ["magicalleaf", "round", "rapidspin"]),
        ("eldegoss", 20, ["magicalleaf", "leafage", "round"]),
    ]),
    ("gym_leader_nessa_8a02", "Gym Leader Nessa", "leader", 9, [("potion", 1)], "mystic_water", True, [
        ("goldeen", 22, ["waterpulse", "whirlpool", "hornattack", "agility"]),
        ("arrokuda", 23, ["aquajet", "bite", "whirlpool", "furyattack"]),
        ("drednaw", 24, ["razorshell", "watergun", "bite", "headbutt"]),
    ]),
    ("gym_leader_kabu_8a03", "Gym Leader Kabu", "leader", 12, [("super_potion", 1)], "charcoal_stick", False, [
        ("ninetales", 25, ["willowisp", "firespin", "quickattack", "ember"]),
        ("arcanine", 25, ["willowisp", "flamewheel", "bite", "agility"]),
        ("centiskorch", 27, ["flamewheel", "coil", "bugbite", "smokescreen"]),
    ]),
    ("gym_leader_bea_8a04", "Gym Leader Bea", "leader", 15, [("super_potion", 2)], "black_belt", True, [
        # 4 levels below Sword's (34, 34, 35, 36), so the cap rises in steps from Kabu (27) to Opal (38).
        ("hitmontop", 30, ["triplekick", "quickattack", "counter", "revenge"]),
        ("pangoro", 30, ["bulletpunch", "circlethrow", "nightslash", "workup"]),
        ("sirfetchd", 31, ["detect", "swordsdance", "brutalswing", "revenge"]),
        ("machamp", 32, ["scaryface", "revenge", "knockoff", "strength"]),
    ]),
    ("gym_leader_allister_8a0d", "Gym Leader Allister", "leader", 15, [("super_potion", 2)], "spell_tag", False, [
        # Shield's gym 4 leader, 4 levels below Shield's (34, 34, 35, 36) like Bea.
        ("yamask", 30, ["brutalswing", "hex", "disable"], "galarian"),
        ("mimikyu", 30, ["shadowsneak", "babydolleyes", "honeclaws", "slash"]),
        ("cursola", 31, ["ancientpower", "curse", "hex"]),
        ("gengar", 32, ["venoshock", "hex", "hypnosis", "payback"]),
    ]),
    ("gym_leader_opal_8a05", "Gym Leader Opal", "leader", 18, [("hyper_potion", 1)], "fairy_feather", True, [
        ("weezing", 36, ["sludge", "fairywind", "tackle"], "galarian"),
        ("mawile", 36, ["drainingkiss", "crunch", "irondefense", "astonish"]),
        ("togekiss", 37, ["airslash", "drainingkiss", "ancientpower"]),
        ("alcremie", 38, ["drainingkiss", "acidarmor", "sweetkiss"]),
    ]),
    ("gym_leader_gordie_8a06", "Gym Leader Gordie", "leader", 20, [("hyper_potion", 1)], "hard_stone", False, [
        ("barbaracle", 40, ["shellsmash", "rocktomb", "razorshell"]),
        ("shuckle", 40, ["powersplit", "stoneedge", "strugglebug", "rocktomb"]),
        ("stonjourner", 41, ["stealthrock", "rocktomb", "bodyslam", "wonderroom"]),
        ("coalossal", 42, ["tarshot", "stealthrock", "heatcrash", "rocktomb"]),
    ]),
    ("gym_leader_melony_8a0e", "Gym Leader Melony", "leader", 20, [("hyper_potion", 1)], "never_melt_ice", True, [
        # Shield's gym 6 leader.
        ("frosmoth", 40, ["icywind", "featherdance", "bugbuzz", "hail"]),
        ("darmanitan", 40, ["iciclecrash", "headbutt", "taunt", "firefang"], "galarian"),
        ("eiscue", 41, ["amnesia", "freezedry", "hail", "icywind"]),
        ("lapras", 42, ["icywind", "icebeam", "surf", "sing"]),
    ]),
    ("gym_leader_piers_8a07", "Gym Leader Piers", "leader", 22, [("hyper_potion", 2)], "black_glasses", False, [
        ("scrafty", 44, ["fakeout", "sandattack", "brickbreak", "payback"]),
        ("malamar", 45, ["nightslash", "foulplay", "psychocut", "payback"]),
        ("skuntank", 45, ["suckerpunch", "screech", "toxic", "snarl"]),
        ("obstagoon", 46, ["obstruct", "throatchop", "counter", "shadowclaw"]),
    ]),
    ("gym_leader_raihan_8a08", "Gym Leader Raihan", "leader", 24, [("hyper_potion", 2)], "dragon_fang", False, [
        ("gigalith", 46, ["stealthrock", "sandtomb", "rockblast", "bodypress"]),
        ("flygon", 47, ["thunderpunch", "breakingswipe", "crunch", "steelwing"]),
        ("sandaconda", 46, ["protect", "glare", "firefang", "earthpower"]),
        ("duraludon", 48, ["breakingswipe", "bodypress", "stoneedge", "ironhead"]),
    ]),
    ("marnie_8a09", "Marnie", "e4", 28, [("hyper_potion", 2)], "black_glasses", True, [
        ("liepard", 47, ["fakeout", "torment", "nastyplot", "snarl"]),
        ("toxicroak", 47, ["venoshock", "suckerpunch", "swagger", "toxic"]),
        ("scrafty", 47, ["swagger", "scaryface", "brickbreak", "crunch"]),
        ("morpeko", 48, ["bulletseed", "torment", "spark", "bite"]),
        ("grimmsnarl", 49, ["spiritbreak", "darkestlariat", "bulkup", "torment"]),
    ]),
    ("hop_8a0a", "Hop", "e4", 28, [("hyper_potion", 2)], "sharp_beak", False, [
        ("dubwool", 48, ["cottonguard", "reversal", "bodyslam", "zenheadbutt"]),
        ("corviknight", 48, ["steelwing", "drillpeck", "scaryface", "swagger"]),
        ("pincurchin", 47, ["thunderbolt", "poisonjab", "curse"]),
        ("snorlax", 47, ["heavyslam", "highhorsepower", "hammerarm"]),
        ("inteleon", 49, ["snipeshot", "mudshot", "icywind", "darkpulse"]),
    ]),
    ("bede_8a0b", "Bede", "e4", 28, [("full_restore", 1), ("hyper_potion", 1)], "twisted_spoon", False, [
        ("mawile", 51, ["playrough", "ironhead", "crunch"]),
        ("gardevoir", 51, ["dazzlinggleam", "psychic", "calmmind", "wish"]),
        ("rapidash", 52, ["psychocut", "takedown", "dazzlinggleam", "quickattack"], "galarian"),
        ("hatterene", 53, ["psychic", "dazzlinggleam", "calmmind", "mysticalfire"]),
    ]),
    ("champion_leon_8a0c", "Champion Leon", "champ", 31, [("full_restore", 2)], "life_orb", False, [
        ("aegislash", 62, ["kingsshield", "shadowball", "sacredsword", "flashcannon"]),
        ("dragapult", 62, ["shadowball", "flamethrower", "thunderbolt", "dragonbreath"]),
        ("haxorus", 63, ["poisonjab", "irontail", "outrage", "earthquake"]),
        ("seismitoad", 64, ["toxic", "drainpunch", "liquidation", "earthquake"]),
        ("cinderace", 64, ["pyroball", "feint", "acrobatics", "quickattack"]),
        ("charizard", 65, ["fireblast", "airslash", "solarbeam", "ancientpower"]),
    ]),
]

# Each trainer's Dynamax Pokémon, as in Sword and Shield: "dynamax", or "gmax" to Gigantamax (it gets the Gigantamax
# Factor). Piers never Dynamaxes. RCT's battle AI uses these through RCT API's "gimmicks" field; the battle only offers
# Dynamax with the Cobblemon: Mega Showdown mod installed, so without it they change nothing.
DYNAMAX = {
    "gym_leader_milo_8a01": ("eldegoss", "dynamax"),
    "gym_leader_nessa_8a02": ("drednaw", "gmax"),
    "gym_leader_kabu_8a03": ("centiskorch", "gmax"),
    "gym_leader_bea_8a04": ("machamp", "gmax"),
    "gym_leader_allister_8a0d": ("gengar", "gmax"),
    "gym_leader_opal_8a05": ("alcremie", "gmax"),
    "gym_leader_gordie_8a06": ("coalossal", "gmax"),
    "gym_leader_melony_8a0e": ("lapras", "gmax"),
    "gym_leader_raihan_8a08": ("duraludon", "gmax"),
    "marnie_8a09": ("grimmsnarl", "gmax"),
    "hop_8a0a": ("inteleon", "dynamax"),
    "bede_8a0b": ("hatterene", "gmax"),
    "champion_leon_8a0c": ("charizard", "gmax"),
}

# The tier each trainer's win unlocks, in battle order: tier 2 needs Milo, …, tier 5 both gym 4 leaders (Bea, then
# Allister), tier 7 both gym 6 leaders (Gordie, then Melony), tier 9 Raihan, and tier 10 the Champion Cup and Leon.
TIER_OF = {
    "gym_leader_milo_8a01": 2, "gym_leader_nessa_8a02": 3, "gym_leader_kabu_8a03": 4,
    "gym_leader_bea_8a04": 5, "gym_leader_allister_8a0d": 5, "gym_leader_opal_8a05": 6,
    "gym_leader_gordie_8a06": 7, "gym_leader_melony_8a0e": 7, "gym_leader_piers_8a07": 8,
    "gym_leader_raihan_8a08": 9, "marnie_8a09": 10, "hop_8a0a": 10, "bede_8a0b": 10, "champion_leon_8a0c": 10,
}

# Hard versions ("<id>_hard"), like the other regions' Hard leaders: every Pokémon at the Normal team's ace level, one
# more Pokémon than Normal (six in the Champion Cup), 31 IVs, full EV spreads, held items and competitive moves. Each
# Hard team keeps the Normal team's Pokémon and Dynamaxes the same one (see DYNAMAX).
# Team entries: (species, ability, nature, held item or None, EV spread, moves), plus an aspect for a regional form.
# EV spreads: "atk" (Attack and Speed), "spa" (Sp. Atk and Speed), "hpatk" (HP and Attack), "hpspa" (HP and Sp. Atk),
# "def" (HP and Defense), "spd" (HP and Sp. Def).
EV_SPREADS = {
    "atk": {"hp": 4, "atk": 252, "spe": 252}, "spa": {"hp": 4, "spa": 252, "spe": 252},
    "hpatk": {"hp": 252, "atk": 252, "def": 4}, "hpspa": {"hp": 252, "spa": 252, "spd": 4},
    "def": {"hp": 252, "def": 252, "spd": 4}, "spd": {"hp": 252, "def": 4, "spd": 252},
}
HARD = {
    "gym_leader_milo_8a01": ([("super_potion", 2)], [
        ("gossifleur", "regenerator", "bold", "eviolite", "def", ["magicalleaf", "rapidspin", "sweetscent", "hypervoice"]),
        ("roselia", "naturalcure", "modest", "eviolite", "hpspa", ["magicalleaf", "sludgebomb", "toxicspikes", "synthesis"]),
        ("eldegoss", "regenerator", "modest", "sitrus_berry", "hpspa", ["magicalleaf", "hypervoice", "cottonguard", "synthesis"]),
    ]),
    "gym_leader_nessa_8a02": ([("super_potion", 2)], [
        ("pelipper", "drizzle", "modest", "damp_rock", "hpspa", ["waterpulse", "airslash", "roost", "uturn"]),
        ("goldeen", "swiftswim", "adamant", "eviolite", "atk", ["waterfall", "hornattack", "agility", "flail"]),
        ("arrokuda", "swiftswim", "adamant", "mystic_water", "atk", ["aquajet", "liquidation", "bite", "psychicfangs"]),
        ("drednaw", "swiftswim", "adamant", "white_herb", "atk", ["razorshell", "rocktomb", "crunch", "shellsmash"]),
    ]),
    "gym_leader_kabu_8a03": ([("super_potion", 2)], [
        ("torkoal", "drought", "relaxed", "heat_rock", "def", ["stealthrock", "flamewheel", "bodyslam", "yawn"]),
        ("ninetales", "flashfire", "timid", "charcoal_stick", "spa", ["willowisp", "flamethrower", "hex", "nastyplot"]),
        ("arcanine", "intimidate", "adamant", "sitrus_berry", "atk", ["flamewheel", "crunch", "wildcharge", "extremespeed"]),
        ("centiskorch", "flamebody", "adamant", "sitrus_berry", "hpatk", ["firelash", "leechlife", "coil", "crunch"]),
    ]),
    "gym_leader_bea_8a04": ([("hyper_potion", 2)], [
        ("hitmontop", "intimidate", "impish", "sitrus_berry", "def", ["fakeout", "triplekick", "rapidspin", "suckerpunch"]),
        ("grapploct", "limber", "careful", "leftovers", "spd", ["octolock", "brickbreak", "suckerpunch", "bulkup"]),
        ("pangoro", "ironfist", "adamant", "expert_belt", "hpatk", ["drainpunch", "bulletpunch", "crunch", "bulkup"]),
        ("sirfetchd", "steadfast", "adamant", "black_belt", "atk", ["brickbreak", "leafblade", "knockoff", "swordsdance"]),
        ("machamp", "guts", "adamant", "flame_orb", "hpatk", ["crosschop", "knockoff", "rocktomb", "bulletpunch"]),
    ]),
    "gym_leader_allister_8a0d": ([("hyper_potion", 2)], [
        ("yamask", "wanderingspirit", "bold", "eviolite", "def", ["willowisp", "hex", "protect", "brutalswing"], "galarian"),
        ("mimikyu", "disguise", "jolly", "life_orb", "atk", ["shadowclaw", "playrough", "swordsdance", "shadowsneak"]),
        ("cursola", "perishbody", "modest", "spell_tag", "hpspa", ["hex", "ancientpower", "willowisp", "strengthsap"]),
        ("polteageist", "cursedbody", "calm", "leftovers", "spd", ["shadowball", "gigadrain", "willowisp", "strengthsap"]),
        ("gengar", "cursedbody", "timid", "black_sludge", "spa", ["shadowball", "sludgebomb", "hypnosis", "willowisp"]),
    ]),
    "gym_leader_opal_8a05": ([("hyper_potion", 2)], [
        ("weezing", "mistysurge", "bold", "black_sludge", "def", ["strangesteam", "sludgebomb", "willowisp", "defog"], "galarian"),
        ("klefki", "prankster", "bold", "light_clay", "def", ["foulplay", "reflect", "lightscreen", "dazzlinggleam"]),
        ("mawile", "intimidate", "adamant", "sitrus_berry", "hpatk", ["playrough", "ironhead", "suckerpunch", "swordsdance"]),
        ("togekiss", "serenegrace", "timid", "leftovers", "spa", ["airslash", "dazzlinggleam", "nastyplot", "roost"]),
        ("alcremie", "sweetveil", "calm", "leftovers", "spd", ["dazzlinggleam", "mysticalfire", "calmmind", "recover"]),
    ]),
    "gym_leader_gordie_8a06": ([("hyper_potion", 2)], [
        ("shuckle", "sturdy", "bold", "mental_herb", "def", ["stealthrock", "stickyweb", "encore", "toxic"]),
        ("barbaracle", "toughclaws", "adamant", "white_herb", "atk", ["shellsmash", "stoneedge", "razorshell", "xscissor"]),
        ("crustle", "sturdy", "adamant", "rocky_helmet", "hpatk", ["spikes", "rockblast", "xscissor", "knockoff"]),
        ("stonjourner", "powerspot", "adamant", "hard_stone", "hpatk", ["stoneedge", "heavyslam", "highhorsepower", "wonderroom"]),
        ("coalossal", "flamebody", "relaxed", "leftovers", "def", ["heatcrash", "stoneedge", "willowisp", "rapidspin"]),
    ]),
    "gym_leader_melony_8a0e": ([("hyper_potion", 2)], [
        ("abomasnow", "snowwarning", "quiet", "never_melt_ice", "hpspa", ["blizzard", "gigadrain", "iceshard", "woodhammer"]),
        ("frosmoth", "icescales", "modest", "leftovers", "spa", ["quiverdance", "icebeam", "bugbuzz", "gigadrain"]),
        ("darmanitan", "gorillatactics", "jolly", "choice_scarf", "atk", ["iciclecrash", "flareblitz", "uturn", "earthquake"], "galarian"),
        ("eiscue", "iceface", "adamant", "sitrus_berry", "atk", ["iciclecrash", "bellydrum", "zenheadbutt", "liquidation"]),
        ("lapras", "waterabsorb", "modest", "leftovers", "hpspa", ["freezedry", "surf", "thunderbolt", "icebeam"]),
    ]),
    "gym_leader_piers_8a07": ([("hyper_potion", 2)], [
        ("scrafty", "intimidate", "careful", "sitrus_berry", "spd", ["fakeout", "drainpunch", "knockoff", "bulkup"]),
        ("malamar", "contrary", "adamant", "leftovers", "hpatk", ["superpower", "knockoff", "psychocut", "rest"]),
        ("skuntank", "aftermath", "adamant", "black_sludge", "atk", ["suckerpunch", "crunch", "poisonjab", "toxic"]),
        ("toxtricity", "punkrock", "modest", "throat_spray", "spa", ["overdrive", "sludgebomb", "boomburst", "voltswitch"]),
        ("obstagoon", "guts", "adamant", "flame_orb", "atk", ["facade", "obstruct", "knockoff", "closecombat"]),
    ]),
    "gym_leader_raihan_8a08": ([("hyper_potion", 2)], [
        ("gigalith", "sandstream", "adamant", "smooth_rock", "hpatk", ["stealthrock", "stoneedge", "earthquake", "heavyslam"]),
        ("flygon", "levitate", "jolly", "choice_scarf", "atk", ["earthquake", "dragonclaw", "uturn", "firepunch"]),
        ("sandaconda", "sandspit", "impish", "leftovers", "def", ["glare", "earthquake", "coil", "stoneedge"]),
        ("turtonator", "shellarmor", "modest", "white_herb", "spa", ["shellsmash", "dragonpulse", "flamethrower", "focusblast"]),
        ("duraludon", "lightmetal", "modest", "life_orb", "spa", ["dracometeor", "flashcannon", "thunderbolt", "bodypress"]),
    ]),
    "marnie_8a09": ([("full_restore", 1), ("hyper_potion", 1)], [
        ("liepard", "prankster", "timid", "focus_sash", "spa", ["fakeout", "thunderwave", "nastyplot", "darkpulse"]),
        ("toxicroak", "dryskin", "adamant", "black_sludge", "atk", ["drainpunch", "suckerpunch", "gunkshot", "swordsdance"]),
        ("scrafty", "intimidate", "careful", "leftovers", "spd", ["fakeout", "drainpunch", "knockoff", "bulkup"]),
        ("skuntank", "aftermath", "adamant", "black_sludge", "atk", ["suckerpunch", "crunch", "poisonjab", "toxic"]),
        ("morpeko", "hungerswitch", "jolly", "sitrus_berry", "atk", ["aurawheel", "knockoff", "partingshot", "protect"]),
        ("grimmsnarl", "prankster", "careful", "light_clay", "spd", ["spiritbreak", "darkestlariat", "reflect", "lightscreen"]),
    ]),
    "hop_8a0a": ([("full_restore", 1), ("hyper_potion", 1)], [
        ("dubwool", "fluffy", "impish", "leftovers", "def", ["cottonguard", "bodypress", "doubleedge", "swordsdance"]),
        ("corviknight", "pressure", "impish", "leftovers", "def", ["bravebird", "bodypress", "roost", "bulkup"]),
        ("trevenant", "harvest", "careful", "sitrus_berry", "spd", ["woodhammer", "hornleech", "willowisp", "phantomforce"]),
        ("pincurchin", "electricsurge", "modest", "sitrus_berry", "hpspa", ["risingvoltage", "thunderbolt", "toxicspikes", "suckerpunch"]),
        ("snorlax", "thickfat", "adamant", "leftovers", "hpatk", ["bodyslam", "heavyslam", "highhorsepower", "curse"]),
        ("inteleon", "torrent", "timid", "life_orb", "spa", ["snipeshot", "icebeam", "darkpulse", "uturn"]),
    ]),
    "bede_8a0b": ([("full_restore", 2)], [
        ("mawile", "intimidate", "adamant", "sitrus_berry", "hpatk", ["playrough", "ironhead", "suckerpunch", "swordsdance"]),
        ("gothitelle", "competitive", "calm", "leftovers", "spd", ["psychic", "thunderbolt", "calmmind", "rest"]),
        ("klefki", "prankster", "bold", "light_clay", "def", ["reflect", "lightscreen", "dazzlinggleam", "spikes"]),
        ("gardevoir", "trace", "timid", "life_orb", "spa", ["moonblast", "psychic", "calmmind", "mysticalfire"]),
        ("rapidash", "pastelveil", "jolly", "life_orb", "atk", ["playrough", "zenheadbutt", "highhorsepower", "swordsdance"], "galarian"),
        ("hatterene", "magicbounce", "quiet", "leftovers", "hpspa", ["psychic", "dazzlinggleam", "mysticalfire", "trickroom"]),
    ]),
    "champion_leon_8a0c": ([("full_restore", 2)], [
        ("aegislash", "stancechange", "quiet", "leftovers", "hpspa", ["kingsshield", "shadowball", "flashcannon", "shadowsneak"]),
        ("dragapult", "infiltrator", "timid", "choice_scarf", "spa", ["shadowball", "dracometeor", "flamethrower", "uturn"]),
        ("haxorus", "moldbreaker", "jolly", "lum_berry", "atk", ["dragondance", "outrage", "earthquake", "poisonjab"]),
        ("seismitoad", "swiftswim", "adamant", "life_orb", "atk", ["liquidation", "earthquake", "drainpunch", "raindance"]),
        ("cinderace", "blaze", "jolly", "heavy_duty_boots", "atk", ["pyroball", "highjumpkick", "uturn", "suckerpunch"]),
        ("charizard", "solarpower", "timid", "life_orb", "spa", ["fireblast", "airslash", "solarbeam", "focusblast"]),
    ]),
}

# Biomes for each trainer's RCT entry (they never spawn naturally: spawnWeightFactor is 0).
BIOMES = {"gym_leader_milo_8a01": "is_forest", "gym_leader_nessa_8a02": "is_ocean", "gym_leader_kabu_8a03": "is_badlands",
          "gym_leader_bea_8a04": "is_mountain", "gym_leader_allister_8a0d": "is_forest", "gym_leader_opal_8a05": "is_forest",
          "gym_leader_gordie_8a06": "is_mountain", "gym_leader_melony_8a0e": "is_snowy", "gym_leader_piers_8a07": "is_plains",
          "gym_leader_raihan_8a08": "is_mountain"}


def find_cobblemon_jar():
    root = os.path.join(os.path.expanduser("~"), ".gradle", "caches", "modules-2", "files-2.1", "com.cobblemon", "fabric")
    jars = [j for j in glob.glob(os.path.join(root, "**", "fabric-*.jar"), recursive=True) if "sources" not in os.path.basename(j)]
    if not jars:
        sys.exit("Cobblemon jar not found in the Gradle cache; pass --cobblemon-jar.")
    return max(jars, key=os.path.getmtime)


def load_cobblemon(jar_path):
    species, names = {}, set()
    with zipfile.ZipFile(jar_path) as jar:
        for name in jar.namelist():
            names.add(name)
            m = re.match(r"^data/cobblemon/species/[^/]+/([^/]+)\.json$", name)
            if m:
                species[m.group(1)] = json.loads(jar.read(name))
        lang = json.loads(jar.read("assets/cobblemon/lang/en_us.json"))
    items = {k.split(".", 2)[2] for k in lang if k.startswith("item.cobblemon.")}
    return species, items


def learnable(data, form):
    """Every move id the species (or its form) can learn, from Cobblemon's learnset ("1:tackle", "tm:protect")."""
    moves = set()
    for source in (data, form or {}):
        for entry in source.get("moves", []):
            moves.add(entry.split(":", 1)[1])
    return moves


def first_ability(data, form):
    abilities = (form or {}).get("abilities") or data.get("abilities", [])
    plain = [a for a in abilities if not a.startswith("h:")]
    return (plain or abilities)[0].replace("h:", "")


def gender(data, female_trainer):
    ratio = data.get("maleRatio", 0.5)
    if ratio < 0:
        return "GENDERLESS"
    if ratio == 0:
        return "FEMALE"
    if ratio == 1:
        return "MALE"
    return "FEMALE" if female_trainer else "MALE"


def nature(data, form):
    stats = (form or {}).get("baseStats") or data["baseStats"]
    return "cobblemon:adamant" if stats["attack"] >= stats["special_attack"] else "cobblemon:modest"


def main():
    p = argparse.ArgumentParser(description="Build the Galar league into the datapack (see the top of this file).")
    p.add_argument("--pack", default=DEFAULT_PACK)
    p.add_argument("--cobblemon-jar")
    args = p.parse_args()

    species, items = load_cobblemon(args.cobblemon_jar or find_cobblemon_jar())

    problems = []
    files = {}
    previous = None
    for index, (tid, name, kind, ivs, bag, signature, female, team) in enumerate(TRAINERS):
        for item, _ in bag:
            if item not in items:
                problems.append(f"{tid}: bag item {item} doesn't exist")
        if signature not in items:
            problems.append(f"{tid}: signature item {signature} doesn't exist")
        members = []
        for entry in team:
            sid, level, moves = entry[:3]
            aspect = entry[3] if len(entry) > 3 else None
            data = species.get(sid)
            if data is None or not data.get("implemented", False):
                problems.append(f"{tid}: {sid} isn't an implemented Cobblemon species")
                continue
            form = None
            if aspect:
                form = next((f for f in data.get("forms", []) if aspect in f.get("aspects", [])), None)
                if form is None:
                    problems.append(f"{tid}: {sid} has no {aspect} form")
                    continue
            known = learnable(data, form)
            for move in moves:
                if move not in known:
                    problems.append(f"{tid}: {sid} can't learn {move}")
            member = {
                "species": f"cobblemon:{sid}",
                "gender": gender(data, female),
                "level": level,
                "nature": nature(data, form),
                "ability": first_ability(data, form),
                "moveset": moves,
                "ivs": {s: ivs for s in ("hp", "atk", "def", "spa", "spd", "spe")},
            }
            if aspect:
                member["aspects"] = [aspect]
            max_pick = DYNAMAX.get(tid)
            if max_pick and max_pick[0] == sid:
                if max_pick[1] == "gmax" and not any(f.get("name", "").lower() == "gmax" for f in data.get("forms", [])):
                    problems.append(f"{tid}: {sid} has no Gigantamax form in Cobblemon")
                member["gimmicks"] = {"dynamax": True, "gmax": max_pick[1] == "gmax"}
            members.append(member)
        files[f"rctmod/trainers/{tid}.json"] = {
            "name": name,
            "ai": {"type": "rct", "data": {"maxSelectMargin": 0.15}},
            "battleRules": {"maxItemUses": 2},
            "bag": [{"item": f"cobblemon:{i}", "quantity": q} for i, q in bag],
            "team": members,
        }
        files[f"rctmod/mobs/trainers/single/{tid}.json"] = {
            "type": kind,
            "series": [SERIES],
            "optional": False,
            "requiredDefeats": [[previous]] if previous else [],
            "maxTrainerDefeats": 1,
            "maxTrainerWins": -1,
            "battleCooldownTicks": 240,
            "signatureItem": f"cobblemon:{signature}",
            "spawnWeightFactor": 0,
            "biomeTagWhitelist": [BIOMES.get(tid, "is_plains")],
            "biomeTagBlacklist": ["is_void", "is_nether", "is_end"],
        }
        # The Hard version: same RCT entry, but optional, so RCT's series and level cap only follow the Normal one.
        if tid in HARD:
            hard_bag, hard_team = HARD[tid]
            ace = max(e[1] for e in team)
            for item, _ in hard_bag:
                if item not in items:
                    problems.append(f"{tid}_hard: bag item {item} doesn't exist")
            size = 6 if kind in ("e4", "champ") else min(6, len(team) + 1)
            if len(hard_team) != size:
                problems.append(f"{tid}_hard: has {len(hard_team)} Pokémon, expected {size}")
            normal_species = {(e[0], e[3] if len(e) > 3 else None) for e in team}
            hard_species = {(e[0], e[6] if len(e) > 6 else None) for e in hard_team}
            if not normal_species <= hard_species:
                problems.append(f"{tid}_hard: leaves out Normal Pokémon {sorted(normal_species - hard_species, key=str)}")
            hard_members = []
            for entry in hard_team:
                sid, ability, nat, held, spread, moves = entry[:6]
                aspect = entry[6] if len(entry) > 6 else None
                data = species.get(sid)
                if data is None or not data.get("implemented", False):
                    problems.append(f"{tid}_hard: {sid} isn't an implemented Cobblemon species")
                    continue
                form = None
                if aspect:
                    form = next((f for f in data.get("forms", []) if aspect in f.get("aspects", [])), None)
                    if form is None:
                        problems.append(f"{tid}_hard: {sid} has no {aspect} form")
                        continue
                known = learnable(data, form)
                for move in moves:
                    if move not in known:
                        problems.append(f"{tid}_hard: {sid} can't learn {move}")
                if len(set(moves)) != len(moves) or not 1 <= len(moves) <= 4:
                    problems.append(f"{tid}_hard: {sid} has a bad moveset {moves}")
                abilities = [a.replace("h:", "") for a in ((form or {}).get("abilities") or data.get("abilities", []))]
                if ability not in abilities:
                    problems.append(f"{tid}_hard: {sid} can't have {ability} (has {abilities})")
                if held and held not in items:
                    problems.append(f"{tid}_hard: held item {held} doesn't exist")
                evs = {s: EV_SPREADS[spread].get(s, 0) for s in ("hp", "atk", "def", "spa", "spd", "spe")}
                member = {
                    "species": f"cobblemon:{sid}",
                    "gender": gender(data, female),
                    "level": ace,
                    "nature": f"cobblemon:{nat}",
                    "ability": ability,
                    "moveset": moves,
                    "ivs": {s: 31 for s in ("hp", "atk", "def", "spa", "spd", "spe")},
                    "evs": evs,
                }
                if held:
                    member["heldItem"] = [held]
                if aspect:
                    member["aspects"] = [aspect]
                max_pick = DYNAMAX.get(tid)
                if max_pick and max_pick[0] == sid:
                    member["gimmicks"] = {"dynamax": True, "gmax": max_pick[1] == "gmax"}
                hard_members.append(member)
            files[f"rctmod/trainers/{tid}_hard.json"] = {
                "name": name,
                "ai": {"type": "rct", "data": {"maxSelectMargin": 0.05}},
                "battleRules": {"maxItemUses": 2},
                "bag": [{"item": f"cobblemon:{i}", "quantity": q} for i, q in hard_bag],
                "team": hard_members,
            }
            files[f"rctmod/mobs/trainers/single/{tid}_hard.json"] = dict(
                files[f"rctmod/mobs/trainers/single/{tid}.json"], optional=True)
        previous = tid

    missing_hard = [t[0] for t in TRAINERS if t[0] not in HARD]
    if missing_hard:
        problems.append(f"no Hard version for {missing_hard}")
    for tid, (sid, _) in DYNAMAX.items():
        hard_team = files.get(f"rctmod/trainers/{tid}_hard.json", {}).get("team", [])
        if not any(m["species"] == f"cobblemon:{sid}" and "gimmicks" in m for m in hard_team):
            problems.append(f"{tid}_hard: its Dynamax Pokémon {sid} isn't on its team")
        if not any(m["species"] == f"cobblemon:{sid}" and "gimmicks" in m
                   for m in files.get(f"rctmod/trainers/{tid}.json", {}).get("team", [])):
            problems.append(f"{tid}: its Dynamax Pokémon {sid} isn't on its team")
    if problems:
        sys.exit("Nothing written:\n  " + "\n  ".join(problems))

    files[f"rctmod/series/{SERIES}.json"] = {
        "title": "Galar",
        "description": "Take on the Gym Challenge, win the Champion Cup and defeat Champion Leon.",
        "difficulty": 5,
        "requiredSeries": [],
    }
    for path, content in files.items():
        full = os.path.join(args.pack, path)
        os.makedirs(os.path.dirname(full), exist_ok=True)
        with open(full, "w", encoding="utf-8", newline="\n") as f:
            json.dump(content, f, indent=2)
            f.write("\n")

    # Tier files: trainers and level caps. A tier's cap is the strongest Pokémon of the next trainer to beat.
    region_dir = os.path.join(args.pack, "ultimate-cobblemon-progression", "regions", SERIES)
    aces = {t[0]: max(e[1] for e in t[7]) for t in TRAINERS}
    order = [t[0] for t in TRAINERS]
    for tier in range(1, 11):
        path = os.path.join(region_dir, f"tier_{tier}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        trainers = [[t, t + "_hard"] for t in order if TIER_OF[t] == tier]
        next_trainer = next((t for t in order if TIER_OF[t] > tier), None)
        data["levelCap"] = aces[next_trainer] if next_trainer else 100
        data["trainers"] = trainers
        write_tier(path, data)
        print(f"tier_{tier}: cap {data['levelCap']}, trainers {[g[0] for g in trainers]}")

    meta_path = os.path.join(region_dir, "region.json")
    with open(meta_path, encoding="utf-8") as f:
        meta = json.load(f)
    meta["series"] = SERIES
    with open(meta_path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(meta, f, indent=2)
        f.write("\n")
    print(f"Wrote {len(TRAINERS)} trainers and the {SERIES} series.")


def write_tier(path, data):
    """Same layout as the other tier files: short lists on one line, species one per line."""
    lines = ["{", f'  "levelCap": {data["levelCap"]},']
    if data.get("trainers"):
        lines.append(f'  "trainers": {json.dumps(data["trainers"])},')
    groups = data.get("requirements", {}).get("advancements", [])
    if groups:
        lines += ['  "requirements": {', f'    "advancements": {json.dumps(groups)}', "  },"]
    lines.append('  "species": [')
    lines.append(",\n".join(f'    "{s}"' for s in data.get("species", [])))
    lines += ["  ]", "}"]
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
