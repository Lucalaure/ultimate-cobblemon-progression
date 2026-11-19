"""Generates a region's tier_N.json files for UCP region mode.

Reads one or more regional Pokedexes (e.g. cobblemon:kanto) and every species' base stats from the Cobblemon jar,
ranks the region's species by Base Stat Total, and splits them into equal-sized tiers (weakest first).
region.json is not touched.

Usage:
    python scripts/generate-region-tiers.py --dex cobblemon:sinnoh --out-dir <regions/sinnoh folder> [options]

Options:
    --dex ID[,ID...]        One or more regional dexes, e.g. cobblemon:sinnoh,cobblemon:hisui. Regional-form entries
                            (e.g. vulpix-alola) are skipped: UCP tiers species, so a form follows its base species' region.
    --out-dir DIR           The region's folder, e.g.
                            src/main/resources/resourcepacks/kanto_league/data/ultimate-cobblemon-progression/regions/kanto
    --level-caps N,N,...    One level cap per tier. Leave it out to keep each existing tier file's cap (and tier count).
    --advancements A;A;...  What unlocks each tier, one entry per tier, separated by ";". Leave an entry empty for a
                            tier with no requirement (normally tier 1). Separate alternatives with "|": any one of them
                            unlocks the tier, e.g. "rctmod:trainers/defeat_leader_brock|rctmod:trainers/defeat_gym_leader_roark_1".
                            Leave the option out to keep each existing tier file's requirements.
    --cobblemon-jar JAR     The Cobblemon jar. By default it's looked up in the Gradle cache (run ./gradlew build once first).

What it keeps from the existing tier files: each tier's "trainers" list (the gym leaders a Gym Challenge block summons),
and its level cap and requirements unless you pass new ones. Only "species" is always worked out from scratch.

Examples:
    # Re-sort Johto's species into its tiers, keeping its caps and its Kanto-or-Sinnoh badge requirements:
    python scripts/generate-region-tiers.py --dex cobblemon:johto --out-dir .../regions/johto

    # A new 3-tier region:
    python scripts/generate-region-tiers.py --dex cobblemon:hoenn --out-dir .../regions/hoenn \\
        --level-caps 15,30,100 \\
        --advancements ";rctmod:trainers/defeat_leader_brock;rctmod:trainers/defeat_champion_terry"
"""
import argparse, glob, json, os, re, sys, zipfile

STATS = ("hp", "attack", "defence", "special_attack", "special_defence", "speed")


def find_cobblemon_jar():
    root = os.path.join(os.path.expanduser("~"), ".gradle", "caches", "modules-2", "files-2.1", "com.cobblemon", "fabric")
    jars = [j for j in glob.glob(os.path.join(root, "**", "fabric-*.jar"), recursive=True) if "sources" not in os.path.basename(j)]
    if not jars:
        sys.exit("Cobblemon jar not found in the Gradle cache; pass --cobblemon-jar.")
    return max(jars, key=os.path.getmtime)


def ranked_species(jar_path, dexes):
    """The dexes' species as (id, bst), weakest first, ties by id."""
    with zipfile.ZipFile(jar_path) as jar:
        names = jar.namelist()
        species_files = {}
        for name in names:
            m = re.match(r"^data/cobblemon/species/[^/]+/([^/]+)\.json$", name)
            if m:
                species_files[m.group(1)] = name

        ids = []
        for dex in dexes:
            ns, path = dex.split(":", 1)
            entry = f"data/{ns}/dexes/{path}.json"
            if entry not in names:
                sys.exit(f"Pokedex {dex} not found in {jar_path}")
            for sid in json.loads(jar.read(entry))["entries"]:
                if sid not in ids:
                    ids.append(sid)

        ranked = []
        for sid in ids:
            file = species_files.get(sid.split(":", 1)[1])
            if not file:
                continue  # a regional form, not a species
            stats = json.loads(jar.read(file))["baseStats"]
            ranked.append((sid, sum(int(stats[s]) for s in STATS)))
    if not ranked:
        sys.exit(f"No species found for {', '.join(dexes)}")
    ranked.sort(key=lambda e: (e[1], e[0]))
    return ranked


def read_existing(out_dir):
    """Existing tier files by tier number."""
    tiers = {}
    for path in glob.glob(os.path.join(out_dir, "tier_*.json")):
        m = re.match(r"^tier_(\d+)\.json$", os.path.basename(path))
        if m:
            with open(path, encoding="utf-8") as f:
                tiers[int(m.group(1))] = json.load(f)
    return tiers


def parse_advancements(text):
    """'a;b|c;' -> [[], [a]... ] : one list of alternative groups per tier."""
    tiers = []
    for entry in text.split(";"):
        entry = entry.strip()
        alts = [a.strip() for a in entry.split("|") if a.strip()] if entry else []
        tiers.append([alts] if alts else [])
    return tiers


def write_tier(path, cap, trainers, advancements, species):
    """Same layout as the shipped tier files: short lists on one line, species one per line."""
    lines = ["{", f'  "levelCap": {cap},']
    if trainers:
        lines.append(f'  "trainers": {json.dumps(trainers)},')
    if advancements:
        groups = [g[0] if len(g) == 1 else g for g in advancements]
        lines += ['  "requirements": {', f'    "advancements": {json.dumps(groups)}', "  },"]
    lines.append('  "species": [')
    lines.append(",\n".join(f'    "{s}"' for s in species))
    lines += ["  ]", "}"]
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")


def requirement_groups(tier_json):
    """A tier file's advancements as a list of alternative groups."""
    groups = tier_json.get("requirements", {}).get("advancements", [])
    return [g if isinstance(g, list) else [g] for g in groups]


def main():
    p = argparse.ArgumentParser(description="Generate a region's tier_N.json files (see the top of this file).")
    p.add_argument("--dex", required=True)
    p.add_argument("--out-dir", required=True)
    p.add_argument("--level-caps")
    p.add_argument("--advancements")
    p.add_argument("--cobblemon-jar")
    args = p.parse_args()

    existing = read_existing(args.out_dir)
    caps = [int(c) for c in args.level_caps.split(",")] if args.level_caps else None
    advancements = parse_advancements(args.advancements) if args.advancements is not None else None

    tier_count = len(caps) if caps else len(advancements) if advancements else len(existing)
    if tier_count == 0:
        sys.exit("No existing tier files to keep caps and requirements from; pass --level-caps and --advancements.")
    if caps and advancements and len(caps) != len(advancements):
        sys.exit(f"--level-caps has {len(caps)} entries but --advancements has {len(advancements)}; give one of each per tier.")
    for t in range(1, tier_count + 1):
        if t not in existing and (caps is None or advancements is None):
            sys.exit(f"tier_{t}.json doesn't exist yet, so --level-caps and --advancements are needed.")

    ranked = ranked_species(args.cobblemon_jar or find_cobblemon_jar(), [d.strip() for d in args.dex.split(",")])
    os.makedirs(args.out_dir, exist_ok=True)

    for t in range(1, tier_count + 1):
        # Equal-count split: species at rank i go to tier floor(i * tiers / count) + 1.
        in_tier = [e for i, e in enumerate(ranked) if i * tier_count // len(ranked) + 1 == t]
        old = existing.get(t, {})
        cap = caps[t - 1] if caps else old["levelCap"]
        reqs = advancements[t - 1] if advancements else requirement_groups(old)
        write_tier(os.path.join(args.out_dir, f"tier_{t}.json"), cap, old.get("trainers", []), reqs,
                   sorted(sid for sid, _ in in_tier))
        bsts = [bst for _, bst in in_tier]
        print(f"tier_{t}: {len(in_tier):3} species, BST {min(bsts) if bsts else '-'}-{max(bsts) if bsts else '-'}, cap {cap}")

    for t in sorted(existing):
        if t > tier_count:
            print(f"warning: tier_{t}.json is beyond the {tier_count} tiers generated; delete it if the region should have fewer tiers")


if __name__ == "__main__":
    main()
