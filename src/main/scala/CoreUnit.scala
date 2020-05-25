import chisel3._

class CoreUnitPort(implicit val conf:CAHPConfig) extends Bundle {
  val romPort = new RomPort()
  val ramPort = new RamPort()

  val mainRegOut = Output(new MainRegisterOutPort)
  val finishFlag = Output(Bool())
  val load = Input(Bool())
  val testRegx8 = if(conf.test) Output(UInt(16.W)) else Output(UInt(0.W))
}

class CoreUnit(implicit val conf:CAHPConfig) extends Module {
  val io = IO(new CoreUnitPort())

  val ifUnit = Module(new IfUnit)
  val idwbUnit = Module(new IdWbUnit)
  val exUnit = Module(new ExUnit)
  val memUnit = Module(new MemUnit)

  io.testRegx8 := idwbUnit.io.mainRegOut.x8
  io.finishFlag := memUnit.io.out.finishFlag
  io.romPort.addr := ifUnit.io.out.romAddr
  io.mainRegOut := idwbUnit.io.mainRegOut
  ifUnit.io.in.romData := io.romPort.data

  io.ramPort.addr := memUnit.io.ramPort.addr
  io.ramPort.writeData := memUnit.io.ramPort.writeData
  io.ramPort.writeEnable := memUnit.io.ramPort.writeEnable

  ifUnit.io.in.jump := exUnit.io.out.jump
  ifUnit.io.in.jumpAddress := exUnit.io.out.jumpAddress
  ifUnit.io.enable := (!io.load)

  idwbUnit.io.idIn := ifUnit.io.out
  idwbUnit.io.wbIn := memUnit.io.out
  idwbUnit.io.exRegWriteIn := exUnit.io.wbOut.regWrite
  idwbUnit.io.memRegWriteIn := memUnit.io.out.regWrite
  idwbUnit.io.exMemIn := exUnit.io.memOut

  exUnit.io.in := idwbUnit.io.exOut
  exUnit.io.memIn := idwbUnit.io.memOut
  exUnit.io.wbIn := idwbUnit.io.wbOut

  memUnit.io.addr := exUnit.io.out
  memUnit.io.in := exUnit.io.memOut
  memUnit.io.wbIn := exUnit.io.wbOut
  memUnit.io.ramPort.readData := io.ramPort.readData
}