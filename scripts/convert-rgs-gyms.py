"""Turns the 8 gym buildings from Radical Gyms & Structures (RGS, MIT, by JayOnline) into village centres.

Usage:
    python scripts/convert-rgs-gyms.py <RGS data/rgs/structure folder> src/main/resources/data/ultimate-cobblemon-progression/structure

For each gym and each village style (plains, desert, savanna, snowy, taiga) it writes
village/<style>/<gym>.nbt, which VillageGyms adds to that style's town-centre pool. Changes from RGS:

- The command blocks that summoned the leader, the start pressure plate, the rematch button and their redstone are removed.
- A Gym Challenge block goes 2 blocks behind the leader's old spot, facing the entrance, so the leader it
  summons stands where RGS put them.
- CobbleFurnies furniture is kept, so the gyms need CobbleFurnies (UCP skips village gyms without it).
- Jigsaw blocks connect the gym to the village's streets, villagers, iron golem and cats, plus a village bell.
"""
import gzip, os, struct, sys

# ---------------------------------------------------------------- NBT (just what structure files use)

def _read(f, n): return f.read(n)

def read_payload(f, t):
    if t == 1: return ('b', struct.unpack('>b', _read(f, 1))[0])
    if t == 2: return ('s', struct.unpack('>h', _read(f, 2))[0])
    if t == 3: return ('i', struct.unpack('>i', _read(f, 4))[0])
    if t == 4: return ('l', struct.unpack('>q', _read(f, 8))[0])
    if t == 5: return ('f', struct.unpack('>f', _read(f, 4))[0])
    if t == 6: return ('d', struct.unpack('>d', _read(f, 8))[0])
    if t == 7:
        n = struct.unpack('>i', _read(f, 4))[0]; return ('ba', _read(f, n))
    if t == 8:
        n = struct.unpack('>H', _read(f, 2))[0]; return ('str', _read(f, n).decode('utf-8'))
    if t == 9:
        et = _read(f, 1)[0]; n = struct.unpack('>i', _read(f, 4))[0]
        return ('list', et, [read_payload(f, et) for _ in range(n)])
    if t == 10:
        d = {}
        while True:
            tt = _read(f, 1)[0]
            if tt == 0: return ('c', d)
            n = struct.unpack('>H', _read(f, 2))[0]
            key = _read(f, n).decode('utf-8')   # read the name before the value
            d[key] = read_payload(f, tt)
    if t == 11:
        n = struct.unpack('>i', _read(f, 4))[0]; return ('ia', list(struct.unpack('>%di' % n, _read(f, 4 * n))))
    if t == 12:
        n = struct.unpack('>i', _read(f, 4))[0]; return ('la', list(struct.unpack('>%dq' % n, _read(f, 8 * n))))
    raise ValueError('tag %d' % t)

TYPE_IDS = {'b': 1, 's': 2, 'i': 3, 'l': 4, 'f': 5, 'd': 6, 'ba': 7, 'str': 8, 'list': 9, 'c': 10, 'ia': 11, 'la': 12}

def write_str(s):
    b = s.encode('utf-8'); return struct.pack('>H', len(b)) + b

def write_payload(v):
    k = v[0]
    if k == 'b': return struct.pack('>b', v[1])
    if k == 's': return struct.pack('>h', v[1])
    if k == 'i': return struct.pack('>i', v[1])
    if k == 'l': return struct.pack('>q', v[1])
    if k == 'f': return struct.pack('>f', v[1])
    if k == 'd': return struct.pack('>d', v[1])
    if k == 'ba': return struct.pack('>i', len(v[1])) + v[1]
    if k == 'str': return write_str(v[1])
    if k == 'list':
        et = v[1]
        return bytes([et]) + struct.pack('>i', len(v[2])) + b''.join(write_payload(i) for i in v[2])
    if k == 'c':
        return b''.join(bytes([TYPE_IDS[tv[0]]]) + write_str(name) + write_payload(tv) for name, tv in v[1].items()) + b'\x00'
    if k == 'ia': return struct.pack('>i', len(v[1])) + struct.pack('>%di' % len(v[1]), *v[1])
    if k == 'la': return struct.pack('>i', len(v[1])) + struct.pack('>%dq' % len(v[1]), *v[1])
    raise ValueError(k)

def load(path):
    with gzip.open(path, 'rb') as f:
        t = _read(f, 1)[0]; n = struct.unpack('>H', _read(f, 2))[0]; _read(f, n)
        return read_payload(f, t)

def save(path, root):
    with gzip.open(path, 'wb') as f:
        f.write(b'\x0a' + write_str('') + write_payload(root))

def S(s): return ('str', s)
def I(i): return ('i', i)
def C(d): return ('c', d)

# ---------------------------------------------------------------- the conversion

STYLES = ['plains', 'desert', 'savanna', 'snowy', 'taiga']
GYMS = ['pewter', 'cerulean', 'vermilion', 'celadon', 'fuchsia', 'saffron', 'cinnabar', 'blackthorn']

GYM_BLOCK = 'ultimate-cobblemon-progression:gym_challenge'
GYM_POS = (12, 3, 15)            # 2 blocks behind the leader's RGS spot (12, 3, 13), facing the north entrance
# start pressure plate, rematch button, and the redstone wire under each
REMOVE_TO_AIR = [(12, 3, 3), (21, 3, 13), (12, 1, 3), (22, 2, 13)]
BELL_POS = (5, 3, 6)
MOSAIC = 'cobblefurnies:vanilla_mosaic'

def convert(src, style):
    root = src[1]
    palette = [(e[1]['Name'][1], {k: v[1] for k, v in e[1].get('Properties', ('c', {}))[1].items()})
               for e in root['palette'][2]]
    blocks = {}
    for b in root['blocks'][2]:
        d = b[1]
        pos = tuple(x[1] for x in d['pos'][2])
        name, props = palette[d['state'][1]]
        nbt = d.get('nbt')
        if 'command_block' in name:
            continue                     # becomes structure void, so the ground stays
        blocks[pos] = (name, props, nbt)

    for pos in REMOVE_TO_AIR:
        blocks[pos] = ('minecraft:air', {}, None)
    blocks[GYM_POS] = (GYM_BLOCK, {'facing': 'north'}, C({'id': S(GYM_BLOCK)}))
    blocks[BELL_POS] = ('minecraft:bell', {'attachment': 'floor', 'facing': 'south', 'powered': 'false'}, C({'id': S('minecraft:bell')}))

    def jigsaw(pos, orientation, pool, final_state, name, joint):
        blocks[pos] = ('minecraft:jigsaw', {'orientation': orientation}, C({
            'id': S('minecraft:jigsaw'), 'name': S(name), 'target': S(name), 'pool': S(pool),
            'final_state': S(final_state), 'joint': S(joint),
            'selection_priority': I(0), 'placement_priority': I(0)}))

    streets = 'minecraft:village/%s/streets' % style
    for pos, orientation in (((10, 1, 0), 'north_up'), ((0, 1, 12), 'west_up'),
                             ((24, 1, 12), 'east_up'), ((12, 1, 25), 'south_up')):
        jigsaw(pos, orientation, streets, 'minecraft:structure_void', 'minecraft:street', 'aligned')

    villagers = 'minecraft:village/%s/villagers' % style
    for pos, pool in (((3, 2, 5), villagers), ((21, 2, 5), villagers),
                      ((3, 2, 20), 'minecraft:village/common/iron_golem'), ((21, 2, 20), 'minecraft:village/common/cats')):
        jigsaw(pos, 'up_north', pool, MOSAIC, 'minecraft:bottom', 'rollable')

    index, new_palette, new_blocks = {}, [], []
    for pos in sorted(blocks, key=lambda p: (p[1], p[2], p[0])):
        name, props, nbt = blocks[pos]
        key = (name, tuple(sorted(props.items())))
        if key not in index:
            index[key] = len(new_palette)
            entry = {'Name': S(name)}
            if props: entry['Properties'] = C({k: S(v) for k, v in props.items()})
            new_palette.append(C(entry))
        entry = {'pos': ('list', 3, [I(x) for x in pos]), 'state': I(index[key])}
        if nbt: entry['nbt'] = nbt
        new_blocks.append(C(entry))

    out = dict(root)
    out['palette'] = ('list', 10, new_palette)
    out['blocks'] = ('list', 10, new_blocks)
    out['entities'] = ('list', 10, [])
    return ('c', out)

def main():
    src_dir, out_root = sys.argv[1], sys.argv[2]
    for gym in GYMS:
        src = load(os.path.join(src_dir, gym + '_gym.nbt'))
        for style in STYLES:
            out_dir = os.path.join(out_root, 'village', style)
            os.makedirs(out_dir, exist_ok=True)
            save(os.path.join(out_dir, gym + '_gym.nbt'), convert(src, style))
        print('converted', gym)

if __name__ == '__main__':
    main()
