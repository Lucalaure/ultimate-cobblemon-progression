"""Adds every regional form to its region's tier files, placed by the form's own Base Stat Total.

Usage:
    python scripts/add-regional-forms.py [--regions-dir DIR] [--cobblemon-jar JAR]

    --regions-dir    The regions folder (default: the built-in leagues pack,
                     src/main/resources/resourcepacks/kanto_league/data/ultimate-cobblemon-progression/regions).
    --cobblemon-jar  The Cobblemon jar. By default it's looked up in the Gradle cache (run ./gradlew build once first).

A regional form belongs to the region whose region.json claims its aspect in "formAspects" (Alolan forms to Alola,
Hisuian forms to Sinnoh, ...). Region-bias forms (a Pikachu that evolves into Alolan Raichu) and battle-only forms
(Galarian Darmanitan's Zen Mode) are left out: region-bias Pokémon never spawn in region mode, and battle-only forms
can't be caught.

Each form goes in the first tier whose strongest species has at least the form's Base Stat Total, so the tiers' own
species stay where they are. A form is written as "cobblemon:<species> <aspect>", plus more aspects when several forms
share one (Paldean Tauros: "cobblemon:tauros paldean blaze-breed").

The script is safe to run again: it removes the form entries it finds and adds them back. Run it after
generate-region-tiers.py, which rebuilds a region's species lists from scratch.
"""
import argparse, glob, importlib.util, json, os, re, sys, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_REGIONS = os.path.join(HERE, "..", "src", "main", "resources", "resourcepacks", "kanto_league", "data",
                               "ultimate-cobblemon-progression", "regions")
STATS = ("hp", "attack", "defence", "special_attack", "special_defence", "speed")

# The tier file writer and jar lookup from generate-region-tiers.py, so both scripts write the same layout.
_spec = importlib.util.spec_from_file_location("region_tiers", os.path.join(HERE, "generate-region-tiers.py"))
region_tiers = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(region_tiers)


def bst(stats):
    return sum(int(stats[s]) for s in STATS)


def load_species(jar_path):
    """Every Cobblemon species by id path ("rattata"), as its parsed JSON."""
    out = {}
    with zipfile.ZipFile(jar_path) as jar:
        for name in jar.namelist():
            m = re.match(r"^data/cobblemon/species/[^/]+/([^/]+)\.json$", name)
            if m:
                out[m.group(1)] = json.loads(jar.read(name))
    return out


def picked_form(forms, aspects):
    """The form CJP picks for these aspects: the one with all of them and the fewest others, first on ties."""
    best = None
    for f in forms:
        fa = f.get("aspects", [])
        if all(a in fa for a in aspects) and (best is None or len(fa) < len(best.get("aspects", []))):
            best = f
    return best


def entry_for(path, forms, form, regional):
    """'cobblemon:rattata alolan', with more of the form's aspects if the regional aspect alone picks another form."""
    aspects = [regional]
    if picked_form(forms, aspects) is not form:
        aspects = [regional] + [a for a in form["aspects"] if a != regional]
    return f"cobblemon:{path} " + " ".join(aspects)


def main():
    p = argparse.ArgumentParser(description="Add regional forms to the region tier files (see the top of this file).")
    p.add_argument("--regions-dir", default=DEFAULT_REGIONS)
    p.add_argument("--cobblemon-jar")
    args = p.parse_args()

    species = load_species(args.cobblemon_jar or region_tiers.find_cobblemon_jar())

    regions = {}  # region id -> (formAspects, {tier: json})
    for meta_path in glob.glob(os.path.join(args.regions_dir, "*", "region.json")):
        folder = os.path.dirname(meta_path)
        with open(meta_path, encoding="utf-8") as f:
            meta = json.load(f)
        regions[os.path.basename(folder)] = (folder, [a.lower() for a in meta.get("formAspects", [])],
                                            region_tiers.read_existing(folder))

    aspect_region = {}
    for rid, (_, aspects, _) in regions.items():
        for a in aspects:
            if not a.startswith("region-bias-"):
                aspect_region.setdefault(a, rid)

    # The regional forms of each region, as (entry, bst).
    region_forms = {rid: [] for rid in regions}
    for path, data in sorted(species.items()):
        if not data.get("implemented", False):
            continue
        forms = data.get("forms", [])
        for form in forms:
            if form.get("battleOnly", False):
                continue
            regional = next((a for a in form.get("aspects", []) if a in aspect_region), None)
            if regional is None:
                continue
            stats = form.get("baseStats") or data["baseStats"]
            region_forms[aspect_region[regional]].append((entry_for(path, forms, form, regional), bst(stats)))

    for rid, (folder, _, tiers) in sorted(regions.items()):
        if not tiers:
            continue
        # Strip the form entries a previous run added; plain species ids have no space.
        for t in tiers.values():
            t["species"] = [s for s in t.get("species", []) if " " not in s.strip()]
        forms = region_forms[rid]

        # Each tier's strongest species, from Cobblemon's stats (species from other mods are skipped).
        tier_max = {}
        for n, t in tiers.items():
            bsts = [bst(species[s.split(":", 1)[1]]["baseStats"]) for s in t["species"]
                    if s.startswith("cobblemon:") and s.split(":", 1)[1] in species]
            tier_max[n] = max(bsts) if bsts else None
        last = max(tiers)

        placed = {n: [] for n in tiers}
        for entry, value in forms:
            tier = next((n for n in sorted(tiers) if tier_max[n] is not None and tier_max[n] >= value), last)
            placed[tier].append(entry)

        for n in sorted(tiers):
            t = tiers[n]
            listed = sorted(t["species"]) + sorted(placed[n])
            region_tiers.write_tier(os.path.join(folder, f"tier_{n}.json"), t["levelCap"], t.get("trainers", []),
                                    region_tiers.requirement_groups(t), listed)
        if forms:
            print(f"{rid}: {len(forms)} forms: " + ", ".join(f"tier {n} +{len(placed[n])}" for n in sorted(tiers) if placed[n]))


if __name__ == "__main__":
    main()
