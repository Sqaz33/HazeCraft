"""Read-only VPK extraction. Private output must be OUTSIDE the Git repository."""
import argparse, struct, pathlib, zlib, hashlib, json, re

class Vpk:
    def __init__(self, path):
        self.path = pathlib.Path(path)
        self.blob = self.path.read_bytes()
        magic, version, tree_size = struct.unpack_from('<III', self.blob)
        assert magic == 0x55AA1234 and version in (1, 2)
        self.header = 28 if version == 2 else 12
        self.data_start = self.header + tree_size
        self.entries = {}
        p = self.header
        def cstr():
            nonlocal p
            end = self.blob.index(b'\0', p)
            s = self.blob[p:end].decode('utf8')
            p = end + 1
            return s
        while ext := cstr():
            while folder := cstr():
                while name := cstr():
                    crc, pre, archive, off, size, term = struct.unpack_from('<IHHIIH', self.blob, p)
                    assert term == 0xffff
                    p += 18
                    preload = self.blob[p:p+pre]
                    p += pre
                    key = ('' if folder == ' ' else folder + '/') + name + ('' if ext == ' ' else '.' + ext)
                    self.entries[key] = crc, archive, off, size, preload
    def read(self, key):
        crc, archive, off, size, pre = self.entries[key]
        if archive == 0x7fff:
            data = self.blob[self.data_start+off:self.data_start+off+size]
        else:
            path = self.path.with_name(self.path.name.replace('_dir.vpk', f'_{archive:03d}.vpk'))
            with path.open('rb') as f:
                f.seek(off)
                data = f.read(size)
        data = pre + data
        assert len(data) == size + len(pre) and zlib.crc32(data) == crc, key
        return data

def omit_edit_info(data):
    """Omit RED2 editor metadata only. All gameplay/model blocks remain byte-identical.
    VRF 20 cannot decode KV3 type 0x1a in October 2026 RED2. Do not edit DATA.
    """
    if len(data) < 16: return data
    size, header_version, resource_version, rel, count = struct.unpack_from('<IHHII', data)
    if header_version != 12 or count > 128: return data
    start = 8 + rel
    entries = []
    for i in range(count):
        p = start + i * 12
        kind, offset, length = struct.unpack_from('<4sII', data, p)
        if kind != b'RED2': entries.append((kind, p + 4 + offset, length))
    if len(entries) == count: return data
    out = bytearray(data)
    struct.pack_into('<I', out, 12, len(entries))
    for i, (kind, absolute, length) in enumerate(entries):
        p = start + i * 12
        struct.pack_into('<4sII', out, p, kind, absolute - p - 4, length)
        assert out[absolute:absolute+length] == data[absolute:absolute+length]
    return bytes(out)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--vpk', required=True)
    ap.add_argument('--out', required=True)
    ap.add_argument('--pattern', required=True)
    ap.add_argument('--strip-edit-info', action='store_true')
    args = ap.parse_args()
    out = pathlib.Path(args.out).resolve()
    repo = pathlib.Path(__file__).resolve().parents[1]
    assert not out.is_relative_to(repo), 'Retail data must remain outside Git'
    vpk = Vpk(args.vpk)
    pattern = re.compile(args.pattern, re.I)
    records = []
    for key in vpk.entries:
        if not pattern.search(key): continue
        raw = vpk.read(key)
        converted = omit_edit_info(raw) if args.strip_edit_info and key.endswith('_c') else raw
        dest = out / key
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(converted)
        records.append(dict(path=key, size=len(raw), sha256=hashlib.sha256(raw).hexdigest(), editor_metadata_omitted=raw != converted))
    out.mkdir(parents=True, exist_ok=True)
    (out/'extraction-manifest.json').write_text(json.dumps(records, indent=2), encoding='utf8')
    print(f'CRC-verified {len(records)} resources, {sum(r["size"] for r in records):,} bytes; {out}')

if __name__ == '__main__': main()
