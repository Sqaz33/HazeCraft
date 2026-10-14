"""Static PE string-reference evidence. Never attaches to a running game."""
import pathlib, re, json, struct, argparse
import pefile, capstone

def main():
    p = argparse.ArgumentParser()
    p.add_argument('dll'); p.add_argument('out'); p.add_argument('--pattern', default='gravity|citadel_camera|sv_accelerate|sv_friction|sv_airaccelerate|units_per_meter|meters_to_units|citadel_dash|citadel_jump')
    a = p.parse_args()
    path = pathlib.Path(a.dll); data = path.read_bytes(); pe = pefile.PE(data=data, fast_load=True)
    base = pe.OPTIONAL_HEADER.ImageBase
    strings = {}
    for m in re.finditer(rb'[ -~]{5,}\x00', data):
        text = m.group()[:-1].decode('ascii')
        if re.search(a.pattern,text,re.I):
            try: va = base + pe.get_rva_from_offset(m.start())
            except Exception: continue
            strings[va] = text
    dis = capstone.Cs(capstone.CS_ARCH_X86, capstone.CS_MODE_64)
    results = []
    for section in pe.sections:
        if not section.Characteristics & 0x20000000: continue
        raw = section.get_data(); va = base + section.VirtualAddress
        # RIP-relative LEA/MOV forms. Decode each candidate from its exact opcode.
        for m in re.finditer(rb'[\x48\x4c][\x8d\x8b][\x05\x0d\x15\x1d\x25\x2d\x35\x3d]', raw):
            off = m.start()
            if off + 7 > len(raw): continue
            target = va + off + 7 + struct.unpack_from('<i', raw, off+3)[0]
            if target not in strings: continue
            # Approximate instruction boundary prior to xref for investigation only.
            start = max(0, off-96)
            rows = [f'{i.address:016x}: {i.mnemonic} {i.op_str}' for i in dis.disasm(raw[start:off+180],va+start)]
            results.append(dict(string=strings[target], string_va=hex(target), xref_va=hex(va+off), context=rows))
    pathlib.Path(a.out).write_text(json.dumps(dict(dll=str(path),strings={hex(k):v for k,v in strings.items()},refs=results),indent=2),encoding='utf8')
    print(f'{path.name}: {len(strings)} matching strings, {len(results)} static references')

if __name__=='__main__': main()
