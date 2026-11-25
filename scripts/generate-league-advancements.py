"""Generates UCP's "Pokemon Journey" advancements from the region data, and points the tier files at them.

Usage:
    python scripts/generate-league-advancements.py [--pack DIR] [--names FILE] [--rct-jar JAR]

    --pack     The datapack's data folder (default: the built-in leagues pack,
               src/main/resources/resourcepacks/kanto_league/data).
    --names    Badge and Champion names per league (default: scripts/league-advancements.json).
    --rct-jar  The Radical Cobblemon Trainers jar, for the names and signature items of trainers the pack doesn't
               override. By default it's looked up in the Gradle cache (run ./gradlew build once first).

Run it after changing a league's trainers or adding a region. It writes, under
<pack>/ultimate-cobblemon-progression/advancement/journey/:

- root.json: the "Pokemon Journey" tab.
- <region>/...: for each league region (one with a "series"), one advancement per trainer listed in its tier files,
  chained in order: a badge for each gym leader (a gym can have two, beaten in order, like Galar's Bea and Allister),
  one per Elite Four member, then the Champion. Each is earned by
  beating that trainer (trigger ultimate-cobblemon-progression:defeat_trainer). Badge advancements also name their
  PokeBadges item ("ultimate-cobblemon-progression:badge", e.g. pokebadges:boulder_kanto_badge): with PokeBadges
  installed, UCP shows it as the icon and awards that badge.
- <region>/catch_tier_<n>.json: for each tier of a league region with Pokémon, a hidden advancement earned by catching
  every obtainable Pokémon of that tier (trigger ultimate-cobblemon-progression:catch_tier). They form one line per
  league under its badges: tier 1 hangs off the first badge, and each later tier off the tier before it.
- milestones/badge_<n>.json and milestones/champion.json: hidden advancements earned by beating the n-th gym (its last
  leader) or the Champion of ANY league. Regions without a series unlock their tiers with these, so they follow whichever
  league the player is in, and adding a league only means adding its trainers here.

It also rewrites the "requirements" of every region's tier files to use these advancements (a league's tier N needs
the advancement of the trainer that unlocks it; tier N of any other region needs milestone N-1, its last tier the
Champion milestone) and each league's region.json "completedBy". Everything else in those files is kept.
The journey/ folder is rebuilt from scratch each run.
"""
import argparse, glob, importlib.util, json, os, re, shutil, sys, zipfile

MOD = "ultimate-cobblemon-progression"
HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_PACK = os.path.join(HERE, "..", "src", "main", "resources", "resourcepacks", "kanto_league", "data")

# The tier file writer from generate-region-tiers.py, so both scripts write the same layout.
_spec = importlib.util.spec_from_file_location("region_tiers", os.path.join(HERE, "generate-region-tiers.py"))
region_tiers = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(region_tiers)


def find_rct_jar():
    root = os.path.join(os.path.expanduser("~"), ".gradle", "caches", "modules-2", "files-2.1", "maven.modrinth", "rctmod")
    jars = [j for j in glob.glob(os.path.join(root, "**", "*.jar"), recursive=True) if "sources" not in os.path.basename(j)]
    if not jars:
        sys.exit("Radical Cobblemon Trainers jar not found in the Gradle cache; pass --rct-jar.")
    return max(jars, key=os.path.getmtime)


class Trainers:
    """Trainer names and signature items: the pack's own files first, then RCT's."""

    def __init__(self, pack, rct_jar):
        self.pack = pack
        self.jar = zipfile.ZipFile(rct_jar)

    def _json(self, path):
        local = os.path.join(self.pack, "rctmod", path)
        if os.path.exists(local):
            with open(local, encoding="utf-8") as f:
                return json.load(f)
        try:
            return json.loads(self.jar.read("data/rctmod/" + path))
        except KeyError:
            return {}

    def name(self, trainer_id):
        name = self._json(f"trainers/{trainer_id}.json").get("name")
        if name:
            return name
        return " ".join(w.capitalize() for w in re.sub(r"_[0-9a-f]{4}$", "", trainer_id).split("_"))

    def icon(self, trainer_id):
        return self._json(f"mobs/trainers/single/{trainer_id}.json").get("signatureItem") or "cobblemon:poke_ball"


def slug(text):
    return re.sub(r"[^a-z0-9]+", "_", text.lower()).strip("_")


def text(key, fallback):
    return {"translate": f"advancements.{MOD}.{key}", "fallback": fallback}


def badge_item(region_id, badge_name):
    """The PokeBadges item for a badge, e.g. "Boulder Badge" in kanto -> pokebadges:boulder_kanto_badge."""
    return f"pokebadges:{slug(badge_name).removesuffix('_badge')}_{region_id}_badge"


def advancement(path, parent, icon, title, description, frame, trainers, hidden_display=False, badge=None):
    key = path.replace("/", ".")
    adv = {}
    if parent:
        adv["parent"] = f"{MOD}:{parent}"
    if badge:
        # Read by UCP (PokeBadgesCompat): with PokeBadges installed, this item becomes the icon and is awarded too.
        adv[f"{MOD}:badge"] = badge
    if not hidden_display:
        adv["display"] = {
            "icon": {"id": icon},
            "title": text(key + ".title", title),
            "description": text(key + ".description", description),
            "frame": frame,
            "show_toast": True,
            "announce_to_chat": True,
            "hidden": False,
        }
    adv["criteria"] = {"defeat": {"trigger": f"{MOD}:defeat_trainer", "conditions": {"trainers": trainers}}}
    adv["requirements"] = [["defeat"]]
    return adv


def catch_icon(tier, last):
    """A Poké Ball for the early tiers, better balls for later ones, a Master Ball for the last."""
    if last:
        return "cobblemon:master_ball"
    return "cobblemon:poke_ball" if tier <= 3 else "cobblemon:great_ball" if tier <= 6 else "cobblemon:ultra_ball"


def catch_advancement(region_id, region_title, tier, last, parent):
    path = f"journey/{region_id}/catch_tier_{tier}"
    key = path.replace("/", ".")
    return path, {
        "parent": f"{MOD}:{parent}",
        "display": {
            "icon": {"id": catch_icon(tier, last)},
            "title": text(key + ".title", f"{region_title} Tier {tier} Caught"),
            "description": text(key + ".description", f"Catch every Pokémon of {region_title}'s tier {tier}"),
            "frame": "challenge" if last else "goal",
            "show_toast": True,
            "announce_to_chat": True,
            "hidden": True,
        },
        "criteria": {"catch": {"trigger": f"{MOD}:catch_tier", "conditions": {"region": region_id, "tier": tier}}},
        "requirements": [["catch"]],
    }


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(data, indent=2, ensure_ascii=False) + "\n")


def load_regions(pack):
    regions = {}
    for region_json in glob.glob(os.path.join(pack, MOD, "regions", "*", "region.json")):
        folder = os.path.dirname(region_json)
        with open(region_json, encoding="utf-8") as f:
            meta = json.load(f)
        tiers = region_tiers.read_existing(folder)
        regions[os.path.basename(folder)] = (folder, meta, tiers)
    return dict(sorted(regions.items(), key=lambda r: (r[1][1].get("order", 0), r[0])))


def main():
    p = argparse.ArgumentParser(description="Generate UCP's Pokemon Journey advancements (see the top of this file).")
    p.add_argument("--pack", default=DEFAULT_PACK)
    p.add_argument("--names", default=os.path.join(HERE, "league-advancements.json"))
    p.add_argument("--rct-jar")
    args = p.parse_args()

    pack = os.path.normpath(args.pack)
    with open(args.names, encoding="utf-8") as f:
        names = json.load(f)
    trainers = Trainers(pack, args.rct_jar or find_rct_jar())
    regions = load_regions(pack)
    out = os.path.join(pack, MOD, "advancement", "journey")
    if os.path.isdir(out):
        shutil.rmtree(out)

    write_json(os.path.join(out, "root.json"), {
        "display": {
            "icon": {"id": f"{MOD}:trainer_card"},
            "title": text("journey.root.title", "Pokémon Journey"),
            "description": text("journey.root.description", "Earn every league's badges and become Champion"),
            "frame": "task",
            "background": "minecraft:textures/gui/advancements/backgrounds/adventure.png",
            "show_toast": False,
            "announce_to_chat": False,
            "hidden": False,
        },
        "criteria": {"start": {"trigger": "minecraft:tick"}},
        "requirements": [["start"]],
    })

    badge_trainers = {}      # milestone n -> every league's n-th badge trainers
    champion_trainers = []
    for region_id, (folder, meta, tiers) in regions.items():
        if "series" not in meta:
            continue
        league = names.get(region_id)
        if not league:
            sys.exit(f"League region '{region_id}' has no names in {args.names}")
        title = meta.get("title", region_id)
        steps = [t for t in sorted(tiers) if tiers[t].get("trainers")]
        if not steps:
            print(f"warning: league region '{region_id}' lists no trainers; skipping it")
            continue

        parent = "journey/root"
        unlocks = {}  # tier -> advancement id that unlocks it
        badge = 0     # badges so far (a gym with two leaders gives two)
        slot = 0      # gyms so far: milestone n is the n-th gym of any league, whatever its number of leaders
        for t in steps:
            groups = [g if isinstance(g, list) else [g] for g in tiers[t]["trainers"]]
            last_tier = t == steps[-1]
            if not last_tier:
                slot += 1
            for gi, group in enumerate(groups):
                first = group[0]
                badge_id = None
                if last_tier and gi == len(groups) - 1:
                    path, frame = f"journey/{region_id}/champion", "challenge"
                    adv_title, desc = league["champion"], f"Defeat {trainers.name(first)} and become Champion of {title}"
                    champion_trainers += group
                elif not last_tier:
                    # A gym leader: each earns a badge. A gym with two leaders (Galar's Bea and Allister) is beaten
                    # in order, and its milestone comes with the last one.
                    badge += 1
                    if badge > len(league["badges"]):
                        sys.exit(f"'{region_id}' has more gym leaders than badge names in {args.names}")
                    adv_title = league["badges"][badge - 1]
                    path, frame, desc = f"journey/{region_id}/{slug(adv_title)}", "goal", f"Defeat {trainers.name(first)}"
                    badge_id = badge_item(region_id, adv_title)
                    if gi == len(groups) - 1:
                        badge_trainers.setdefault(slot, []).extend(group)
                else:
                    adv_title = trainers.name(first)
                    path, frame, desc = f"journey/{region_id}/{slug(adv_title)}", "task", f"Defeat {adv_title}"
                write_json(os.path.join(out, *path.split("/")[1:]) + ".json",
                           advancement(path, parent, trainers.icon(first), adv_title, desc, frame, group,
                                       badge=badge_id))
                parent = path
            unlocks[t] = parent
        # Catching every Pokémon of a tier: one line per league, tier 1 off the first badge and each tier off the one
        # before, so the line runs under the badges instead of branching off each of them.
        caught = 0
        catch_parent = unlocks[steps[0]]
        for t in sorted(tiers):
            if not tiers[t].get("species"):
                continue
            path, adv = catch_advancement(region_id, title, t, t == max(tiers), catch_parent)
            write_json(os.path.join(out, *path.split("/")[1:]) + ".json", adv)
            catch_parent = path
            caught += 1

        if badge != len(league["badges"]):
            print(f"warning: '{region_id}' has {badge} gym leaders but {len(league['badges'])} badge names")

        for t, tier in tiers.items():
            reqs = [[f"{MOD}:{unlocks[t]}"]] if t in unlocks else []
            region_tiers.write_tier(os.path.join(folder, f"tier_{t}.json"), tier["levelCap"], tier.get("trainers", []),
                                    reqs, tier.get("species", []))
        meta["completedBy"] = f"{MOD}:{unlocks[steps[-1]]}"
        write_json(os.path.join(folder, "region.json"), meta)
        print(f"{region_id}: {badge} badges, then {len(tiers[steps[-1]]['trainers'])} trainers for the last tier, "
              f"{caught} tier catch advancements")

    for n, group in sorted(badge_trainers.items()):
        write_json(os.path.join(out, "milestones", f"badge_{n}.json"),
                   advancement(f"journey/milestones/badge_{n}", None, None, None, None, None, group, hidden_display=True))
    write_json(os.path.join(out, "milestones", "champion.json"),
               advancement("journey/milestones/champion", None, None, None, None, None, champion_trainers, hidden_display=True))

    # Regions without a series: tier N needs milestone N-1, the last tier the Champion milestone.
    for region_id, (folder, meta, tiers) in regions.items():
        if "series" in meta or not tiers:
            continue
        last = max(tiers)
        for t, tier in tiers.items():
            if t == 1:
                reqs = region_tiers.requirement_groups(tier)
            elif t == last:
                reqs = [[f"{MOD}:journey/milestones/champion"]]
            elif t - 1 in badge_trainers:
                reqs = [[f"{MOD}:journey/milestones/badge_{t - 1}"]]
            else:
                sys.exit(f"'{region_id}' tier {t} has no badge milestone to use (only {len(badge_trainers)} badges)")
            region_tiers.write_tier(os.path.join(folder, f"tier_{t}.json"), tier["levelCap"], tier.get("trainers", []),
                                    reqs, tier.get("species", []))
        print(f"{region_id}: tiers follow the badge milestones")
    print(f"milestones: {len(badge_trainers)} badges + champion")


if __name__ == "__main__":
    main()
