package cache
import chisel3._
import chisel3.util._
import common._
import common.storage._
import common.axi._
import common.ToZero
import common.axi
import chisel3.experimental.ChiselEnum
import scala.language.postfixOps
import math.{pow, max}

object CacheCoreState extends ChiselEnum {
  val sIdle, sIfHit, sFindVictim, sWriteBackLatency,sWriteBack, sReadAddr, sReadData,sWriteMem2Cache, sPrepareCache,sWaitCacheLatency,sRespon,sLocked = Value
}


class CacheCore (
    ADDR_WIDTH: Int = 24,
    DATA_WIDTH: Int = 512,
    LOCK_WIDTH: Int = 1,
    ASSOCIATION: Int = 4,
    ENTRY_NUMBER : Int = 1024,
    LOCK_NUMBER : Int = 2,
    RAM_LATENCY : Int = 1,
    PORT_WIDTH: Int = 4
)extends Module {
    val io = IO(new Bundle{
        // SSD input commands.
        val request = Flipped(Decoupled(new cachecore_request(ADDR_WIDTH, DATA_WIDTH, LOCK_WIDTH,PORT_WIDTH)))
        val response = Decoupled(new cache_response(DATA_WIDTH))
        val mem_interface = new AXI(33, 512, 6, 0, 4)
    })
    val OFFSET_LEN_raw = log2Ceil(DATA_WIDTH/8)
    val INDEX_LEN_raw = log2Ceil(ENTRY_NUMBER/ASSOCIATION)
    val OFFSET_LEN = max(1, OFFSET_LEN_raw)
    val INDEX_LEN = max(1, INDEX_LEN_raw)
    val TAG_LEN   = ADDR_WIDTH - OFFSET_LEN - INDEX_LEN
    val ASSOCIATION_SHIFT = log2Ceil(ASSOCIATION)
    //val SET_NUMBER = ENTRY_NUMBER/ASSOCIATION
    val addr_reg = RegInit(0.U(ADDR_WIDTH.W))
    val data_reg = RegInit(0.U(DATA_WIDTH.W))
    val mask_reg = RegInit(0.U((DATA_WIDTH/8).W))
    val lock_reg = RegInit(0.U(LOCK_WIDTH.W))
    val port_reg = RegInit(0.U(PORT_WIDTH.W))
    val offset = addr_reg(OFFSET_LEN - 1 ,0)
    val index = addr_reg(INDEX_LEN + OFFSET_LEN - 1, OFFSET_LEN)
    val tag = addr_reg(ADDR_WIDTH - 1,INDEX_LEN + OFFSET_LEN)
    assert(DATA_WIDTH <= 512 * 8, "Cache line size must not exceed 512 bytes")
    assert(ADDR_WIDTH <= 32, "Address width must be less than 32 bits")
    val DataRam = XRam(UInt(DATA_WIDTH.W), ENTRY_NUMBER,latency=RAM_LATENCY, use_musk=1)
    //val sqTail  = RegInit(VecInit(Seq.fill(ENTRY_NUMBER)(0.U(TAG_LEN.W)))
    val TagReg = RegInit(VecInit(Seq.fill(ENTRY_NUMBER)(0.U(TAG_LEN.W))))
    val ValidReg = RegInit(VecInit(Seq.fill(ENTRY_NUMBER)(0.U(1.W))))
    val DirtyReg = RegInit(VecInit(Seq.fill(ENTRY_NUMBER)(0.U(1.W))))
    val LockReg  = RegInit(VecInit(Seq.fill(ENTRY_NUMBER)(0.U(1.W))))
    val LockPort  = RegInit(VecInit(Seq.fill(ENTRY_NUMBER)(0.U(PORT_WIDTH.W))))
    val Clock_LRU_Reg = RegInit(VecInit(Seq.fill(ENTRY_NUMBER)(0.U(1.W))))
    val Clock_pointer = RegInit(VecInit(Seq.fill(ENTRY_NUMBER/ASSOCIATION)(0.U(max(1,log2Ceil(ASSOCIATION)).W))))
    io.mem_interface.init()
    val TagWire = Wire(Vec(ASSOCIATION, UInt(TAG_LEN.W)))
    val ValidWire = Wire(Vec(ASSOCIATION, UInt(1.W)))
    //val LockWire = Wire(Vec(ASSOCIATION, UInt(TAG_LEN.W)))

    val ishit = Wire(Bool())
    val isdirty = Wire(Bool())
    val islock = Wire(Bool())
    val hit_index = Reg(UInt((log2Ceil(ENTRY_NUMBER)).W))
    val if_find_victim = Wire(Bool())
    val victim_index = Reg(UInt((log2Ceil(ENTRY_NUMBER)).W))
    val data_out = Wire(UInt(DATA_WIDTH.W))
    val data_in = Reg(UInt(DATA_WIDTH.W))
    val dataram_inex = Reg(UInt((log2Ceil(ENTRY_NUMBER)).W)) //todo need to modified
    import CacheCoreState._
    val  sWriteBackLatency_count = RegInit(0.U(3.W))
    val sWaitCacheLatency_count = RegInit(0.U(3.W))
    
    val state = RegInit(sIdle)


    switch(state){
        is(sIdle){ //* 0
            when(io.request.fire()){
                state := sIfHit
            }
        }
        is(sIfHit){//* 1
            when(islock ){
                state := sLocked
            }.otherwise{
                when(ishit){
                    state := sPrepareCache
                }.otherwise{
                    state := sFindVictim
                }
            }
        }
        is(sFindVictim){    //* 2
            when(if_find_victim){
                when(isdirty){
                    state := sWriteBackLatency
                }.otherwise{
                    state := sReadAddr
                }
            }
        }
        is(sWriteBackLatency){  //* 3
            when(sWriteBackLatency_count + 1.U === RAM_LATENCY.asUInt()){
                state := sWriteBack
            }
        }
        is(sWriteBack){     //* 4
            when(io.mem_interface.aw.fire()&&io.mem_interface.w.fire()){
                state := sReadAddr
            }
        }
        is(sReadAddr){    //* 5 
            when(io.mem_interface.ar.fire()){
                state := sReadData
            }
        }
        is(sReadData){      //* 6
            when(io.mem_interface.r.fire()){
                state := sWriteMem2Cache
            }
        }
        is(sWriteMem2Cache){     //* 7
                state := sPrepareCache  //todo add condition
            
        }
        is(sPrepareCache){          //* 8  prepare cache and lock cache
            when(mask_reg.orR){  //* write option don't need to wait for data
                state := sRespon
            }.otherwise{
                state := sWaitCacheLatency
            }
        }
        is(sWaitCacheLatency){     //* 9 
            when(sWaitCacheLatency_count + 1.U === RAM_LATENCY.asUInt()){
                state := sRespon
            }
        }
        is(sRespon){            //* 10 
            when(io.response.fire()){
                state := sIdle
            }
        }
        is(sLocked){                //* 11
            when(io.response.fire()){
                state := sIdle
            }
        }
    }

    val hit_index_wire = Wire(UInt((log2Ceil(ENTRY_NUMBER)).W))
    when(state === sIfHit){
        when(ishit){
            dataram_inex := hit_index_wire
        }.otherwise{
            dataram_inex := (index<<ASSOCIATION_SHIFT) + Clock_pointer(index)
        }
    }.elsewhen(state === sFindVictim){
        dataram_inex := (index<<ASSOCIATION_SHIFT) + Clock_pointer(index)
    }.otherwise{
        dataram_inex := dataram_inex
    }

    when(state===sWriteBackLatency){
        sWriteBackLatency_count := sWriteBackLatency_count + 1.U
    }.otherwise{
        sWriteBackLatency_count := 0.U
    }

    when(state===sWaitCacheLatency){
        sWaitCacheLatency_count := sWaitCacheLatency_count + 1.U
    }.otherwise{
        sWaitCacheLatency_count := 0.U
    }

    when(state === sIdle){
        io.request.ready := 1.U
        addr_reg := io.request.bits.addr
        data_reg := io.request.bits.data
        mask_reg := io.request.bits.mask
        lock_reg := io.request.bits.lock
        port_reg := io.request.bits.port
    }.otherwise{
        io.request.ready := 0.U
    }

    // ishit
    for(i <- 0 until ASSOCIATION){
        TagWire(i) := TagReg((index<<ASSOCIATION_SHIFT) + i.asUInt)
        ValidWire(i) := ValidReg((index<<ASSOCIATION_SHIFT) + i.asUInt)&(TagWire(i) === tag)
    }
    ishit := ValidWire.asUInt.orR


    
    hit_index_wire := (index<<ASSOCIATION_SHIFT) + PriorityEncoder(~ValidWire.asUInt) -1.U
    islock := LockReg(hit_index_wire) & (LockPort(hit_index_wire) =/= port_reg)
    


    
    // find hit index
    when(state === sIfHit){
        hit_index := hit_index_wire//todo need to check
    }//todo not used
    


    // find victim
    when(state === sFindVictim){   //todo state change for find victim
        when(Clock_LRU_Reg((index<<ASSOCIATION_SHIFT)+Clock_pointer(index)) === 1.U){
            Clock_LRU_Reg((index<<ASSOCIATION_SHIFT)+Clock_pointer(index)) := 0.U
            if_find_victim := 0.U
            when(Clock_pointer(index) === (ASSOCIATION - 1).U){
                Clock_pointer(index) := 0.U
            }.otherwise{
                Clock_pointer(index) := Clock_pointer(index) + 1.U
            }
        }.otherwise{
            if_find_victim := 1.U
        }
        victim_index := (index<<ASSOCIATION_SHIFT) + Clock_pointer(index)
    }.otherwise{
        if_find_victim := 0.U
    }

    isdirty := DirtyReg((index<<ASSOCIATION_SHIFT) + Clock_pointer(index))

    DataRam.io.addr_b := dataram_inex
    data_out := DataRam.io.data_out_b

    val reg_data_out = Reg(UInt(DATA_WIDTH.W))
    when(state === sWaitCacheLatency || state === sWriteBackLatency){
        reg_data_out := data_out
    }

    io.response.valid := state === sRespon || state === sLocked
    io.response.bits.data := reg_data_out
    io.response.bits.success := state === sRespon

    when(state === sReadData){
        data_in := io.mem_interface.r.bits.data
    }

    when(state === sWriteMem2Cache){
            TagReg(dataram_inex) := tag
            ValidReg(dataram_inex) := 1.U
    }
    when(state === sWriteMem2Cache){
        DataRam.io.data_in_a := data_in
        DataRam.io.wr_en_a := 1.U
        DataRam.io.musk_a.get := Fill((DATA_WIDTH/8), 1.U)
        DataRam.io.addr_a := dataram_inex   //todo need to be checked
    }.elsewhen(state === sPrepareCache){
        when(mask_reg =/= 0.U){
            DataRam.io.wr_en_a := mask_reg.orR
            DataRam.io.musk_a.get := mask_reg
            DataRam.io.addr_a := dataram_inex
            DataRam.io.data_in_a := data_reg
        }.otherwise{   //* not used
            DataRam.io.wr_en_a := 0.U  
            DataRam.io.musk_a.get := 0.U
            DataRam.io.addr_a := dataram_inex
            DataRam.io.data_in_a := 0.U
        }
    }.otherwise{
        DataRam.io.wr_en_a := 0.U
        DataRam.io.musk_a.get := 0.U
        DataRam.io.addr_a := 0.U
        DataRam.io.data_in_a := 0.U
    }


    when(state === sPrepareCache){   //* deal with lock signal
        Clock_LRU_Reg(dataram_inex) := 1.U
        when(lock_reg =/= 0.U){
            LockReg(dataram_inex) := 1.U
            LockPort(dataram_inex) := port_reg
        }.otherwise{
            LockReg(dataram_inex) := 0.U
        }
        when(mask_reg =/= 0.U){
            DirtyReg(dataram_inex) := 1.U
        }
    }

    when(state === sWriteBack){
        DirtyReg(dataram_inex) := 0.U
    }
    io.mem_interface.aw.valid := state === sWriteBack && io.mem_interface.aw.ready && io.mem_interface.w.ready
    io.mem_interface.w.valid := state === sWriteBack && io.mem_interface.aw.ready && io.mem_interface.w.ready
    io.mem_interface.aw.bits.addr := addr_reg
    io.mem_interface.aw.bits.len := 0.U
    io.mem_interface.aw.bits.size := 6.U
    io.mem_interface.aw.bits.burst := 0.U
    io.mem_interface.aw.bits.id := 0.U
    io.mem_interface.aw.bits.cache := 0.U
    io.mem_interface.w.bits.data := reg_data_out
    io.mem_interface.w.bits.strb := Fill((DATA_WIDTH/8), 1.U)
    io.mem_interface.w.bits.last := 1.U
    io.mem_interface.b.ready := 1.U

    io.mem_interface.ar.valid := state === sReadAddr 
    io.mem_interface.ar.bits.addr := addr_reg
    io.mem_interface.ar.bits.len := 0.U
    io.mem_interface.ar.bits.size := 6.U
    io.mem_interface.ar.bits.burst := 0.U
    io.mem_interface.ar.bits.id := 0.U
    io.mem_interface.r.ready := state === sReadData
    
}

