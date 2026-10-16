"""Compose original drag/acceleration kernels with the audited dash callbacks.
The trajectory is an offline controller oracle, not a live-game recording.
"""
import json,struct
from pathlib import Path
from unicorn.x86_const import *
import air_accel_native_oracle as accel
import air_drag_native_oracle as drag
def f32(value):return struct.unpack('<f',struct.pack('<f',value))[0]
duration=f32(.43);post_duration=f32(.1);end_scale=f32(.2)

def invoke(module,address,velocity,dt):
 module.uc.mem_write(module.move,b'\0'*0x200);module.write(module.move+0x38,'<3f',*velocity)
 if module is accel:module.uc.mem_write(module.move+0xf4,module.pe.get_data(0x25ba434,4)*3)
 else:
  module.write(module.move+0x2c,'<2f',*module.state['inputs']);module.write(module.rotation+0x10,'<4f',0,0,2**-.5,2**-.5)
 rsp=module.stack+0x1ff08;module.write(rsp,'<Q',module.stop)
 for reg,value in [(UC_X86_REG_RSP,rsp),(UC_X86_REG_RCX,module.movement),(UC_X86_REG_RDX,module.move)]:module.uc.reg_write(reg,value)
 module.uc.reg_write(UC_X86_REG_XMM2,struct.unpack('<I',struct.pack('<f',dt))[0])
 module.uc.emu_start(address,module.stop,count=20000);assert module.uc.reg_read(UC_X86_REG_RSP)==rsp+8
 return list(struct.unpack('<3f',module.uc.mem_read(module.move+0x38,12)))

traces=[]
for dt in [1/64,1/120,.05,.003125]:
 for initial in [0,8.2,20]:
  velocity=[0,initial/.0254,0];position=[0.,0.,0.];time=0;post=None;active=True;frames=[]
  while time<.7:
   time+=dt
   drive=active
   if active and (time>=duration or (position[0]**2+position[1]**2)**.5>8):active=False;post=time
   override=8/duration*(1+(end_scale-1)*min(1,time/duration)) if active else 0
   horizontal=(velocity[0]**2+velocity[1]**2)**.5*.0254
   coefficient=.2
   progress=(time-post)/post_duration if post is not None else 0
   if 0<progress<1:coefficient=max(coefficient,min(1,max(0,(horizontal-12)/(14-12)))*4*progress)
   drag.state.update(override=override/.0254,drag=coefficient,slowScale=1,inputs=[1,0] if drive else [0,0])
   velocity=invoke(drag,0x18085a730,velocity,dt)
   accel.state.update(override=override/.0254,wish=[0,1,0] if drive else [0,0,0],wishSpeed=max(f32(8.2),override)/.0254 if drive else 0,accelScale=f32(f32(60)*f32(.01)) if active else 1)
   velocity=invoke(accel,0x18085a1b0,velocity,dt)
   velocity[2]=max(velocity[2],100)*dt*1 if drive else velocity[2]-800*dt
   position=[p+v*dt*.0254 for p,v in zip(position,velocity)]
   frames.append(dict(time=time,active=active,position=list(position),velocity=[v*.0254 for v in velocity]))
  traces.append(dict(dt=dt,initialSpeed=initial,frames=frames))
out=Path('D:/DeadlockHazeLab/research/25950029/movement/native-air-dash-traces.json')
out.write_text(json.dumps(dict(build=accel.source['build'],sourceSha256=accel.source['clientSha256'],
 qualification='Original current drag and acceleration kernels; dash wish cap, gravity suppression/during-drag and post-drag composed from audited native callbacks. No collision, stamina or live game verification.',traces=traces),indent=2))
print({'trajectories':len(traces),'frames':sum(len(t['frames']) for t in traces),'out':str(out)})
