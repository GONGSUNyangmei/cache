package cache
import chisel3._
import chisel3.util._
import common._
import common.storage._
import common.axi._
import common.ToZero
import common.axi

class DDR_DRIVER (ENABLE_AXI_CTRL		: Boolean=false,
                  BOARD: String="u280",
                  CHANNEL			: Int=0,
                  IP_CORE_NAME  : String="DDR4_mig_blackbox")
  extends RawModule {
   val io = IO(new Bundle {
   
    /////////ddr0 input clock
    //val ddr0_sys_100M_p=Input(Clock())                       
    //val ddr0_sys_100M_n=Input(Clock())      
    //val ddriver_clk=Input(Clock())                   
    ///////////ddr0 PHY interface
    val ddrpin		= new DDRPin()   
    ///////////ddr0 user interface
	  val user_clk  =Output(Clock())         
    val user_rst        =Output(UInt(1.W))        
     
    ///////////     AXI interface
    val axi         = Flipped(new AXI(34,512,4,0,8))
    val axi_ctrl         = if (ENABLE_AXI_CTRL) {Some(Flipped(new AXI(32,32,0,0,0)))} else None
  })


}