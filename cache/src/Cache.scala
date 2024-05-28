package cache
import chisel3._
import chisel3.util._
import common._
import common.storage._
import common.axi._
import common.ToZero
import common.axi

class Cache (
    REQ_NUM: Int = 2,
    ADDR_WIDTH: Int = 24,
    DATA_WIDTH: Int = 512,
    LOCK_WIDTH: Int = 1
)extends Module {
    val io = IO(new Bundle{
        val usr_IO =new CacheIO(2, ADDR_WIDTH, DATA_WIDTH, LOCK_WIDTH)
        val mem_interface = new AXI(33, 256, 6, 0, 4)
    })

    val cache = Module(new CacheCore(ADDR_WIDTH, DATA_WIDTH, LOCK_WIDTH))

}

