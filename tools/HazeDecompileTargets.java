// Targeted offline decompilation of the owner's PE; output stays in the external private lab.
import ghidra.app.script.GhidraScript;
import ghidra.app.cmd.disassemble.DisassembleCommand;
import ghidra.app.decompiler.*;
import ghidra.program.model.address.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.symbol.SourceType;
import ghidra.program.model.data.FloatDataType;
import ghidra.program.model.data.LongLongDataType;
import ghidra.program.model.data.VoidDataType;
import ghidra.program.model.data.BooleanDataType;
import ghidra.program.model.data.IntegerDataType;
import java.nio.file.*;
import java.util.*;

public class HazeDecompileTargets extends GhidraScript {
    @Override public void run() throws Exception {
        String[] arguments=getScriptArgs();Path output=Path.of(arguments[1]);Files.createDirectories(output);
        // Replace only this script's provisional Movement_* definitions before a refreshed import.
        List<Address> previous=new ArrayList<>();
        for(Function f:currentProgram.getFunctionManager().getFunctions(true))
            if(f.getName().startsWith("Movement_"))previous.add(f.getEntryPoint());
        if(arguments.length>2 && arguments[2].equals("replace"))
            for(Address address:previous)currentProgram.getFunctionManager().removeFunction(address);
        List<Function> functions=new ArrayList<>();
        for(String line:Files.readAllLines(Path.of(arguments[0]))){
            if(line.isBlank()||line.startsWith("#"))continue;
            String[] parts=line.split("\\|");AddressSet body=new AddressSet();Address entry=null;
            for(String segment:parts[1].split(",")){
                String[] ends=segment.split("-");Address first=toAddr(ends[0]),last=toAddr(ends[1]).subtract(1);body.add(first,last);if(entry==null||first.compareTo(entry)<0)entry=first;
                new DisassembleCommand(first,new AddressSet(first,last),true).applyTo(currentProgram,monitor);
            }
            // A previously selected leaf block may later be proven to belong to
            // a larger function. Remove only this script's fully enclosed labels.
            List<Address> interior=new ArrayList<>();
            for(Function candidate:currentProgram.getFunctionManager().getFunctions(body,true))
                if(!candidate.getEntryPoint().equals(entry) && candidate.getName().startsWith("Movement_")
                        && body.contains(candidate.getBody()))interior.add(candidate.getEntryPoint());
            for(Address address:interior)currentProgram.getFunctionManager().removeFunction(address);
            Function function=getFunctionAt(entry);
            if(function==null)function=currentProgram.getFunctionManager().createFunction(parts[0],entry,body,SourceType.USER_DEFINED);
            else {function.setName(parts[0],SourceType.USER_DEFINED);function.setBody(body);}
            if(function==null)throw new IllegalStateException("Could not define "+parts[0]);
            function.setNoReturn(false);
            if(parts.length>2){
                if(parts[2].equals("stackprobe"))function.setCallFixup("alloca_probe");
                if(Set.of("state","maxspeed","chargefraction","aircontrol").contains(parts[2])){
                    String kind=parts[2];
                    function.setCallingConvention("__fastcall");
                    function.setReturnType(kind.equals("state")?BooleanDataType.dataType:FloatDataType.dataType,SourceType.USER_DEFINED);
                    List<Parameter> parameters=new ArrayList<>();
                    parameters.add(new ParameterImpl("self",LongLongDataType.dataType,currentProgram));
                    if(kind.equals("state"))parameters.add(new ParameterImpl("stateIndex",IntegerDataType.dataType,currentProgram));
                    if(kind.equals("maxspeed"))for(int i=0;i<3;i++)parameters.add(new ParameterImpl("flag"+i,BooleanDataType.dataType,currentProgram));
                    if(kind.equals("chargefraction")){
                        parameters.add(new ParameterImpl("panel",LongLongDataType.dataType,currentProgram));
                        parameters.add(new ParameterImpl("index",IntegerDataType.dataType,currentProgram));
                    }
                    if(kind.equals("aircontrol"))parameters.add(new ParameterImpl("velocity",LongLongDataType.dataType,currentProgram));
                    function.replaceParameters(Function.FunctionUpdateType.DYNAMIC_STORAGE_ALL_PARAMS,true,SourceType.USER_DEFINED,parameters.toArray(new Parameter[0]));
                }
                if(parts[2].equals("floatthis")||parts[2].equals("modelhandle")){
                    function.setCallingConvention("__fastcall");
                    function.setReturnType(parts[2].equals("floatthis")?FloatDataType.dataType:LongLongDataType.dataType,SourceType.USER_DEFINED);
                    List<Parameter> parameters=new ArrayList<>();
                    parameters.add(new ParameterImpl("selfOrOutput",LongLongDataType.dataType,currentProgram));
                    if(parts[2].equals("modelhandle"))parameters.add(new ParameterImpl("pawn",LongLongDataType.dataType,currentProgram));
                    function.replaceParameters(Function.FunctionUpdateType.DYNAMIC_STORAGE_ALL_PARAMS,true,SourceType.USER_DEFINED,parameters.toArray(new Parameter[0]));
                }

                if(parts[2].equals("returnsfloat"))function.setReturnType(FloatDataType.dataType,SourceType.USER_DEFINED);
                if(parts[2].equals("motion")||parts[2].equals("motionbool")){
                    function.setCallingConvention("__fastcall");
                    function.setReturnType(parts[2].equals("motionbool")?BooleanDataType.dataType:VoidDataType.dataType,SourceType.USER_DEFINED);
                    function.replaceParameters(Function.FunctionUpdateType.DYNAMIC_STORAGE_ALL_PARAMS,true,SourceType.USER_DEFINED,
                        new ParameterImpl("movement",LongLongDataType.dataType,currentProgram),
                        new ParameterImpl("moveData",LongLongDataType.dataType,currentProgram),
                        new ParameterImpl("dt",FloatDataType.dataType,currentProgram));
                }
                int count=parts[2].equals("bias")?2:parts[2].equals("remap")?5:0;
                if(count>0){
                    Parameter[] parameters=new Parameter[count];for(int i=0;i<count;i++)parameters[i]=new ParameterImpl("value"+i,FloatDataType.dataType,currentProgram);
                    function.setReturnType(FloatDataType.dataType,SourceType.USER_DEFINED);
                    function.replaceParameters(Function.FunctionUpdateType.DYNAMIC_STORAGE_ALL_PARAMS,true,SourceType.USER_DEFINED,parameters);
                }
            }
            if(parts.length<3 || !parts[2].equals("bind"))functions.add(function);
        }
        DecompInterface decompiler=new DecompInterface();decompiler.openProgram(currentProgram);
        try{
            for(Function function:functions){
                println("HAZE_DECOMPILE "+function.getName()+" "+function.getEntryPoint());
                DecompileResults result=decompiler.decompileFunction(function,90,monitor);
                if(!result.decompileCompleted())throw new IllegalStateException(result.getErrorMessage());
                Files.writeString(output.resolve(function.getName()+".c"),result.getDecompiledFunction().getC());
            }
            println("HAZE_BATCH_COMPLETE "+functions.size());
        }finally{decompiler.dispose();}
    }
}
