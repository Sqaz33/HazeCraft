"""Extract float convar constructor defaults from the owned DLL static initializers."""
import pathlib, json, struct, pefile, capstone
lab=pathlib.Path('D:/DeadlockHazeLab')
dll=pathlib.Path('D:/SteamLibrary/steamapps/common/Deadlock/game/citadel/bin/win64/client.dll')
data=dll.read_bytes(); pe=pefile.PE(data=data); base=pe.OPTIONAL_HEADER.ImageBase
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64); md.detail=True
refs=json.loads((lab/'evidence/client-cvar-refs.json').read_text())['refs']; result={}
for ref in refs:
    name=ref['string']
    if not name.startswith(('citadel_','sv_')) or len(name)>100: continue
    target=int(ref['xref_va'],16)-base
    func=next((f for f in pe.DIRECTORY_ENTRY_EXCEPTION if f.struct.BeginAddress<=target<f.struct.EndAddress),None)
    if not func: continue
    start=func.struct.BeginAddress; end=func.struct.EndAddress
    if end-start>1600: continue
    decoded=list(md.disasm(pe.get_data(start,end-start),base+start))
    val=None; evidence=None
    for ins in decoded:
        if ins.address>base+target: break
        if ins.mnemonic=='movss' and ins.op_str.startswith('xmm2,') and len(ins.operands)>1:
            op=ins.operands[1]
            if op.type==capstone.x86.X86_OP_MEM and op.mem.base==capstone.x86.X86_REG_RIP:
                addr=ins.address+ins.size+op.mem.disp
                val=struct.unpack('<f',pe.get_data(addr-base,4))[0]
                evidence=hex(ins.address)
    if val is not None:
        result[name]={'value':val,'constructor':hex(base+start),'load':evidence,'name_reference':ref['xref_va']}
(lab/'evidence/native-float-defaults.json').write_text(json.dumps(result,indent=2))
print(json.dumps({k:v['value'] for k,v in result.items()},indent=2))
