import chisel3._
import chisel3.util.Cat

class WbUnitIn(implicit val conf:CAHPConfig) extends Bundle {
  val regWrite = new MainRegInWrite()
  val inst = UInt(24.W)
  val instAddr = UInt(conf.instAddrWidth.W)
  val finishFlag = Bool()

  override def cloneType: this.type = new WbUnitIn()(conf).asInstanceOf[this.type]
}

class IdWbUnitPort(implicit val conf:CAHPConfig) extends Bundle {
  val idIn = Input(new IfUnitOut)
  val wbIn = Input(new WbUnitIn)
  val exRegWriteIn = Input(new MainRegInWrite)
  val memRegWriteIn = Input(new MainRegInWrite)
  val exMemIn = Input(new MemUnitIn)

  val exOut = Output(new ExUnitIn)
  val memOut = Output(new MemUnitIn)
  val wbOut = Output(new WbUnitIn)

  val mainRegOut = Output(new MainRegisterOutPort)
}

class IdWbUnit(implicit val conf:CAHPConfig) extends Module {
  val io = IO(new IdWbUnitPort)

  val decoder = Module(new InstructionDecoder())
  val mainReg = Module(new MainRegister())

  io.exOut := decoder.io.exOut
  io.exOut.bcIn.pc := io.idIn.instAddr
  io.memOut := decoder.io.memOut
  io.memOut.data := mainReg.io.port.out.rs2Data
  io.wbOut := decoder.io.wbOut
  io.wbOut.inst := io.idIn.inst
  io.wbOut.instAddr := io.idIn.instAddr
  io.mainRegOut := mainReg.io.regOut

  decoder.io.inst := io.idIn.inst

  mainReg.io.port.inRead := decoder.io.regRead
  mainReg.io.port.inWrite := io.wbIn.regWrite
  mainReg.io.inst := io.wbIn.inst
  mainReg.io.instAddr := io.wbIn.instAddr

  when(decoder.io.pcImmSel){
    io.exOut.bcIn.pcImm := decoder.io.pcImm
    io.exOut.bcIn.pcAdd := true.B
  }.otherwise{
    io.exOut.bcIn.pcImm := mainReg.io.port.out.rs1Data
    io.exOut.bcIn.pcAdd := false.B
  }

  when(!decoder.io.inASel){
    io.exOut.aluIn.inA := mainReg.io.port.out.rs1Data
  }.otherwise{
    io.exOut.aluIn.inA := io.idIn.instAddr
  }

  when(!decoder.io.inBSel){
    io.exOut.aluIn.inB := mainReg.io.port.out.rs2Data
  }.otherwise{
    //LUI
    when(io.idIn.inst(5, 0) === "b000100".U(6.W)){
      io.exOut.aluIn.inB := Cat(decoder.io.imm(5, 0), 0.U(10.W))
    }.otherwise{
      io.exOut.aluIn.inB := decoder.io.imm
    }
  }

  when(conf.debugId.B){
    printf("[ID] instAddress:0x%x\n", io.idIn.instAddr)
    printf("[ID] inst:0x%x\n", io.idIn.inst)
    printf("[ID] Imm:0x%x\n", decoder.io.imm)
    //printf("[ID] pc:0x%x\n", io.exOut.bcIn.pc)
    //printf("[ID] pcAdd:%d\n", io.exOut.bcIn.pcAdd)
    //printf("[ID] InAData:0x%x\n", fwd1.io.out)
    //printf("[ID] InBData:0x%x\n", fwd2.io.out)
    //printf("[ID] InASel:%d\n", decoder.io.inASel)
    //printf("[ID] InBSel:%d\n", decoder.io.inBSel)
    //printf("[ID] RegWrite:0x%x\n", decoder.io.wbOut.regWrite)
  }
}
