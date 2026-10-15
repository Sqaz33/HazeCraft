"""Find RIP-relative references to a verified retail vtable."""
import argparse,re,struct,json
from pathlib import Path
import pefile
p=argparse.ArgumentParser();p.add_argument('vtable');p.add_argument('out');p.add_argument('--loads',action='store_true',help='Include RIP-relative MOV loads as well as address references');p.add_argument('--dll',default='D:/SteamLibrary/steamapps/common/Deadlock/game/citadel/bin/win64/client.dll');a=p.parse_args()
pe=pefile.PE(a.dll,fast_load=True);base=pe.OPTIONAL_HEADER.ImageBase;target=int(a.vtable,16);refs=[]
for s in pe.sections:
 if not s.Characteristics&0x20000000:continue
 raw=s.get_data();start=base+s.VirtualAddress
 pattern=rb'[\x48\x4c][\x8b\x8d][\x05\x0d\x15\x1d\x25\x2d\x35\x3d]' if a.loads else rb'[\x48\x4c]\x8d[\x05\x0d\x15\x1d\x25\x2d\x35\x3d]'
 for m in re.finditer(pattern,raw):
  i=m.start()
  if start+i+7+struct.unpack_from('<i',raw,i+3)[0]==target:refs.append(hex(start+i))
Path(a.out).write_text(json.dumps(refs,indent=2));print(refs)
