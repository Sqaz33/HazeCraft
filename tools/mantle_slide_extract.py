"""Recover the omitted MantleType_t exit boost from the owner's pinned DLL."""
import hashlib,json,struct
from pathlib import Path
import capstone,pefile

lab=Path('D:/DeadlockHazeLab')
source=lab/'reference/25950029/client.dll'
digest=hashlib.sha256(source.read_bytes()).hexdigest()
schema=json.loads((lab/'evidence/25950029-jump-slide-schema.json').read_text())
field=next(f for f in schema['fields'] if f['field']=='m_flSlideExitBoostOnGround')
assert field['offset']=='0x28'
pe=pefile.PE(str(source),fast_load=True)
decoder=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64)
store=next(i for i in decoder.disasm(pe.get_data(0x23ccb0,64),0x18023ccb0) if i.mnemonic=='mov' and i.op_str.startswith('dword ptr [rcx + 0x28], '))
value=struct.unpack('<f',struct.pack('<I',int(store.op_str.split(', ')[1],0)))[0]
assert value==400
proof=dict(value=value,offset='0x28',store=hex(store.address),constructor='0x18023ccb0',clientSha256=digest,schema=[field],verification='Current named schema and exact constructor store; exit branch 1802851f0 adds this along held input')
path=lab/'research/25950029/movement/native-defaults.json'
defaults=json.loads(path.read_text());defaults['MantleType_t']['m_flSlideExitBoostOnGround']=proof
path.write_text(json.dumps(defaults,indent=2))
(lab/'evidence/25950029-mantle-slide-stage5-boost.json').write_text(json.dumps(proof,indent=2))
print('Verified MantleType_t +0x28 =',value,'Source units; updated private defaults')
