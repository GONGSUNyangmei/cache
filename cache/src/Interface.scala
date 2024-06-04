package cache

import chisel3._
import chisel3.util._
import chisel3.experimental.{DataMirror, requireIsChiselType,Analog}

class cache_request (
    ADDR_WIDTH: Int = 24,
    DATA_WIDTH: Int = 512,
    LOCK_WIDTH: Int = 4
)extends Bundle {
  val addr = UInt(ADDR_WIDTH.W)
  val data = UInt(DATA_WIDTH.W)
  val mask = UInt((DATA_WIDTH/8).W)
  val lock = UInt(LOCK_WIDTH.W)
}

class cachecore_request (
    ADDR_WIDTH: Int = 24,
    DATA_WIDTH: Int = 512,
    LOCK_WIDTH: Int = 4,
    PORT_WIDTH: Int = 4
)extends Bundle {
  val addr = UInt(ADDR_WIDTH.W)
  val data = UInt(DATA_WIDTH.W)
  val mask = UInt((DATA_WIDTH/8).W)
  val lock = UInt(LOCK_WIDTH.W)
  val port = UInt(PORT_WIDTH.W)
}


class cache_response(
    DATA_WIDTH: Int = 512
) extends Bundle {
  val data = UInt(DATA_WIDTH.W)
  val success = Bool()
}


class CacheIO (
    REQ_NUM: Int = 2,
    ADDR_WIDTH: Int = 24,
    DATA_WIDTH: Int = 512,
    LOCK_WIDTH: Int = 1
)extends Bundle {
    val  req = Flipped(Vec(REQ_NUM, Decoupled(new cache_request(ADDR_WIDTH, DATA_WIDTH, LOCK_WIDTH))))
    val resp = Vec(REQ_NUM, Decoupled(new cache_response(DATA_WIDTH)))
}


