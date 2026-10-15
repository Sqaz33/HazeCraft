"""Extract float convar constructor defaults from the owned DLL static initializers."""
import pathlib, json, struct, pefile, capstone, argparse, hashlib
lab=pathlib.Path('D:/DeadlockHazeLab')
parser=argparse.ArgumentParser();parser.add_argument('--refs',default=str(lab/'evidence/client-cvar-refs.json'));parser.add_argument('--out',default=str(lab/'evidence/native-float-defaults.json'));parser.add_argument('--dll',default='D:/SteamLibrary/steamapps/common/Deadlock/game/citadel/bin/win64/client.dll');args=parser.parse_args()
dll=pathlib.Path(args.dll)
data=dll.read_bytes(); pe=pefile.PE(data=data); base=pe.OPTIONAL_HEADER.ImageBase
source_hash=hashlib.sha256(data).hexdigest()
md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_64); md.detail=True
refs=json.loads(pathlib.Path(args.refs).read_text())['refs']; result={}
def register_family(reg):
    name=md.reg_name(reg)
    for family,aliases in {'rax':('rax','eax','ax','al','ah'),'rbx':('rbx','ebx','bx','bl','bh'),
                          'rcx':('rcx','ecx','cx','cl','ch'),'rdx':('rdx','edx','dx','dl','dh'),
                          'rsi':('rsi','esi','si','sil'),'rdi':('rdi','edi','di','dil')}.items():
        if name in aliases:return family
    return name.rstrip('dwb') if name.startswith('r') and name[1:2].isdigit() else name

def register_value(operand, known):
    family=register_family(operand.reg)
    if family not in known:return None
    shift=8 if md.reg_name(operand.reg) in ('ah','bh','ch','dh') else 0
    return (known[family]>>shift)&((1<<(operand.size*8))-1)

def register_store(operand, value, known):
    family=register_family(operand.reg)
    if operand.size>=4:return family,value&((1<<(operand.size*8))-1)
    # A byte/word write does not establish unknown upper register bits.
    if family not in known:return None
    shift=8 if md.reg_name(operand.reg) in ('ah','bh','ch','dh') else 0
    mask=((1<<(operand.size*8))-1)<<shift
    return family,(known[family]&~mask)|((value<<shift)&mask)
for ref in refs:
    name=ref['string']
    if not name.startswith(('citadel_','cl_citadel_','sv_','in_','experimental_citadel_')) or len(name)>100: continue
    target=int(ref['xref_va'],16)-base
    name_instruction=next(md.disasm(pe.get_data(target,15),base+target),None)
    assert name_instruction is not None, ref
    actual_names=[]
    for operand in name_instruction.operands:
        if operand.type==capstone.x86.X86_OP_MEM and operand.mem.base==capstone.x86.X86_REG_RIP:
            pointer=name_instruction.address+name_instruction.size+operand.mem.disp
            actual_names.append(pe.get_data(pointer-base,len(name)+1))
    assert (name+'\0').encode() in actual_names, 'Reference does not match the selected DLL: '+str(ref)
    func=next((f for f in pe.DIRECTORY_ENTRY_EXCEPTION if f.struct.BeginAddress<=target<f.struct.EndAddress),None)
    if not func: continue
    start=func.struct.BeginAddress; end=func.struct.EndAddress
    if end-start>1600: continue
    decoded=list(md.disasm(pe.get_data(start,end-start),base+start))
    val=None; evidence=None; integer=None; integer_evidence=None; known_registers={}
    for ins in decoded:
        if ins.mnemonic in ('xorps','pxor') and ins.op_str=='xmm2, xmm2':val=0.0;evidence=hex(ins.address)
        if ins.mnemonic=='movss' and ins.op_str.startswith('xmm2,') and len(ins.operands)>1:
            op=ins.operands[1]
            if op.type==capstone.x86.X86_OP_MEM and op.mem.base==capstone.x86.X86_REG_RIP:
                addr=ins.address+ins.size+op.mem.disp
                val=struct.unpack('<f',pe.get_data(addr-base,4))[0]
                evidence=hex(ins.address)
        if ins.mnemonic=='mov' and len(ins.operands)>1:
            dest,source=ins.operands[:2]
            if dest.type==capstone.x86.X86_OP_MEM and dest.mem.base==capstone.x86.X86_REG_RSP and dest.mem.disp==0x37 and source.type==capstone.x86.X86_OP_IMM:
                integer=source.imm&0xff;integer_evidence=hex(ins.address)
            if dest.type==capstone.x86.X86_OP_MEM and dest.mem.base==capstone.x86.X86_REG_RSP and dest.mem.disp==0x37 and source.type==capstone.x86.X86_OP_REG:
                value=register_value(source,known_registers)
                if value is not None:integer=value&0xff;integer_evidence=hex(ins.address)
        propagated=None
        if ins.operands and ins.operands[0].type==capstone.x86.X86_OP_REG:
            dest=ins.operands[0]
            if ins.mnemonic=='xor' and len(ins.operands)>1 and ins.operands[1].type==capstone.x86.X86_OP_REG and dest.reg==ins.operands[1].reg:propagated=register_store(dest,0,known_registers)
            if ins.mnemonic=='mov' and len(ins.operands)>1:
                operand=ins.operands[1]
                if operand.type==capstone.x86.X86_OP_IMM:propagated=register_store(dest,operand.imm,known_registers)
                if operand.type==capstone.x86.X86_OP_REG:
                    value=register_value(operand,known_registers)
                    if value is not None:propagated=register_store(dest,value,known_registers)
        if ins.mnemonic=='call' and ins.address>base+target:
            # Bind this name to its own registration call, not later constructors.
            break
        for written in ins.regs_access()[1]:known_registers.pop(register_family(written),None)
        if ins.mnemonic=='call':
            for family in ['rax','rcx','rdx','r8','r9','r10','r11']:known_registers.pop(family,None)
        if propagated:known_registers[propagated[0]]=propagated[1]
    if val is None and integer is not None:val=integer;evidence=integer_evidence
    if val is not None:
        result[name]={'value':val,'constructor':hex(base+start),'load':evidence,'name_reference':ref['xref_va'],
                      'sourceSha256':source_hash}
pathlib.Path(args.out).write_text(json.dumps(result,indent=2))
print(json.dumps({k:v['value'] for k,v in result.items()},indent=2))
