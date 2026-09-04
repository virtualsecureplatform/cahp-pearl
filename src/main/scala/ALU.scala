import chisel3._
import chisel3.util.{BitPat, Cat}

object ALUOpcode {
  def ADD = BitPat("b0000")
  def SUB = BitPat("b0001")
  def AND = BitPat("b0010")
  def XOR = BitPat("b0011")
  def OR  = BitPat("b0100")
  def LSL = BitPat("b0101")
  def LSR = BitPat("b0110")
  def ASR = BitPat("b0111")
  def MOV = BitPat("b1000")
}

class ALUPortIn(implicit val conf:CAHPConfig) extends Bundle {
  val inA = UInt(16.W)
  val inB = UInt(16.W)
  val opcode = UInt(4.W)
}

class ALUPortOut(implicit val conf:CAHPConfig) extends Bundle {
  val out = Output(UInt(16.W))
  val flagCarry = Output(Bool())
  val flagOverflow = Output(Bool())
  val flagSign = Output(Bool())
  val flagZero = Output(Bool())
}

class ALUPort(implicit val conf:CAHPConfig) extends Bundle{
  val in = Input(new ALUPortIn())
  val out = new ALUPortOut()
}

class ALU(implicit val conf:CAHPConfig) extends Module {

  val io = IO(new ALUPort)
  val resCarry = Wire(UInt(17.W))
  resCarry := DontCare

  when(io.in.opcode === ALUOpcode.ADD) {
    io.out.out := io.in.inA + io.in.inB
  }.elsewhen(io.in.opcode === ALUOpcode.SUB) {
    // Keep the carry from A + ~B + 1.  Negating B at 16 bits first loses
    // that carry when B is zero and makes unsigned comparisons incorrect.
    resCarry := Cat(0.U(1.W), io.in.inA) +
      Cat(0.U(1.W), (~io.in.inB).asUInt) + 1.U
    io.out.out := resCarry(15, 0)
  }.elsewhen(io.in.opcode === ALUOpcode.AND) {
    io.out.out := io.in.inA & io.in.inB
  }.elsewhen(io.in.opcode === ALUOpcode.OR) {
    io.out.out := io.in.inA | io.in.inB
  }.elsewhen(io.in.opcode === ALUOpcode.XOR) {
    io.out.out := io.in.inA ^ io.in.inB
  }.elsewhen(io.in.opcode === ALUOpcode.LSL) {
    io.out.out := (io.in.inA << io.in.inB).asUInt
  }.elsewhen(io.in.opcode === ALUOpcode.LSR) {
    io.out.out := (io.in.inA >> io.in.inB).asUInt
  }.elsewhen(io.in.opcode === ALUOpcode.ASR) {
    io.out.out := (io.in.inA.asSInt >> io.in.inB).asUInt
  }.elsewhen(io.in.opcode === ALUOpcode.MOV) {
    io.out.out := io.in.inB
  }.otherwise {
    io.out.out := DontCare
  }

  io.out.flagCarry := ~resCarry(16)
  io.out.flagSign := io.out.out(15)
  io.out.flagZero := (io.out.out === 0.U(16.W))
  io.out.flagOverflow := (io.in.inA(15) =/= io.in.inB(15)) &&
    (io.out.out(15) =/= io.in.inA(15))
}
