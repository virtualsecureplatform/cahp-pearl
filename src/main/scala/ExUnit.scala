/*
Copyright 2019 Naoki Matsumoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import chisel3._

class ExUnitPort(implicit val conf:CAHPConfig) extends Bundle {
  val in = Input(new ExUnitIn)
  val memIn = Input(new MemUnitIn)
  val wbIn = Input(new WbUnitIn)

  val out = new ExUnitOut
  val memOut = Output(new MemUnitIn)
  val wbOut = Output(new WbUnitIn)
}

class ExUnitIn(implicit val conf:CAHPConfig) extends Bundle {
  val aluIn = new ALUPortIn

  val bcIn = new BranchControllerIn()
}

class BranchControllerIn(implicit val conf:CAHPConfig) extends Bundle {
  val pcOpcode = UInt(3.W)
  val pc = UInt(16.W)
  val pcImm = UInt(16.W)
  val pcAdd = Bool()
}

class ExUnitOut(implicit val conf:CAHPConfig) extends Bundle {
  val res = Output(UInt(16.W))
  val jumpAddress = Output(UInt(conf.instAddrWidth.W))
  val jump = Output(Bool())

  override def cloneType: this.type = new ExUnitOut()(conf).asInstanceOf[this.type]
}

class ExUnit(implicit val conf:CAHPConfig) extends Module {
  val io = IO(new ExUnitPort)
  val alu = Module(new ALU)

  alu.io.in := io.in.aluIn
  io.out.res := alu.io.out.out

  io.memOut := io.memIn
  io.wbOut := io.wbIn
  io.wbOut.regWrite.writeData := io.out.res

  when(io.in.bcIn.pcAdd) {
    io.out.jumpAddress := io.in.bcIn.pc + io.in.bcIn.pcImm
  }.otherwise{
    io.out.jumpAddress := io.in.bcIn.pcImm
  }

  val flagCarry = alu.io.out.flagCarry
  val flagOverflow = alu.io.out.flagOverflow
  val flagSign = alu.io.out.flagSign
  val flagZero = alu.io.out.flagZero

  io.out.jump := false.B
  when(io.in.bcIn.pcOpcode === 1.U){
    io.out.jump := flagZero
  }.elsewhen(io.in.bcIn.pcOpcode === 2.U){
    io.out.jump := flagCarry
  }.elsewhen(io.in.bcIn.pcOpcode === 3.U){
    io.out.jump := flagCarry||flagZero
  }.elsewhen(io.in.bcIn.pcOpcode === 4.U){
    io.out.jump := true.B
  }.elsewhen(io.in.bcIn.pcOpcode === 5.U){
    io.out.jump := !flagZero
  }.elsewhen(io.in.bcIn.pcOpcode === 6.U){
    io.out.jump := flagSign != flagOverflow
  }.elsewhen(io.in.bcIn.pcOpcode === 7.U){
    io.out.jump := (flagSign != flagOverflow)||flagZero
  }

  when(conf.debugEx.B) {
    printf("[EX] opcode:0x%x\n", io.in.aluIn.opcode)
    printf("[EX] inA:0x%x\n", io.in.aluIn.inA)
    printf("[EX] inB:0x%x\n", io.in.aluIn.inB)
    printf("[EX] Res:0x%x\n", io.out.res)
    printf("[EX] Jump:%d\n", io.out.jump)
    printf("[EX] JumpAddress:0x%x\n", io.out.jumpAddress)
  }

  //when(io.out.jump){
  //  printf("JUMP addr:0x%x pcAdd:%d pc:0x%x pcImm:0x%x\n", io.out.jumpAddress, pExReg.bcIn.pcAdd, pExReg.bcIn.pc, pExReg.bcIn.pcImm)
  //}
}
