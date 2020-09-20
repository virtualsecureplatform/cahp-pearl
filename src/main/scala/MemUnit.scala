import chisel3._
import chisel3.util.Cat

class MemUnitIn(implicit val conf:CAHPConfig) extends Bundle {
  val data = UInt(16.W)
  val byte = Bool()
  val signExt = Bool()
  val read = Bool()
  val write = Bool()

  override def cloneType: this.type = new MemUnitIn()(conf).asInstanceOf[this.type]
}

class MemUnitPort(implicit val conf:CAHPConfig) extends Bundle {
  val addr = Input(new ExUnitOut)
  val in = Input(new MemUnitIn)
  val wbIn = Input(new WbUnitIn)
  val ramPort = new RamPort
  val out = Output(new WbUnitIn)
}

class MemUnit(implicit val conf:CAHPConfig) extends Module {
  val io = IO(new MemUnitPort())

  io.out := io.wbIn
  io.ramPort.addr := io.addr.res(conf.dataAddrWidth-1,1)
  io.ramPort.writeData := DontCare
  io.ramPort.writeEnable := false.B

  def sign_ext_8bit(v:UInt) : UInt = {
    val res = Wire(UInt(16.W))
    when(v(7,7) === 1.U){
      res := Cat(0xFF.U(8.W), v)
    }.otherwise{
      res := v
    }
    res
  }

  when(io.in.read){
    when(io.in.byte) {
      when(io.in.signExt){
        when(io.addr.res(0) === 0.U){
          io.out.regWrite.writeData := sign_ext_8bit(io.ramPort.readData(7, 0))
        }.otherwise{
          io.out.regWrite.writeData := sign_ext_8bit(io.ramPort.readData(15, 8))
        }
      }.otherwise{
        when(io.addr.res(0) === 0.U){
          io.out.regWrite.writeData := Cat(0.U(8.W), io.ramPort.readData(7, 0))
        }.otherwise{
          io.out.regWrite.writeData := Cat(0.U(8.W), io.ramPort.readData(15, 8))
        }
      }
    }.otherwise{
      io.out.regWrite.writeData := io.ramPort.readData
    }
  }.otherwise{
    io.out.regWrite.writeData := io.addr.res
  }

  when(io.in.write){
    io.ramPort.writeEnable := true.B
    when(io.in.byte){
      when(io.addr.res(0) === 0.U){
        io.ramPort.writeData := Cat(io.ramPort.readData(15, 8), io.in.data(7, 0))
      }.otherwise{
        io.ramPort.writeData := Cat(io.in.data(7, 0), io.ramPort.readData(7, 0))
      }
    }.otherwise{
      io.ramPort.writeData := io.in.data
    }
  }
}
