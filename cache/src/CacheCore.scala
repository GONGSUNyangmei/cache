package cache
import chisel3._
import chisel3.util._
import common._
import common.storage._
import common.axi._
import common.ToZero
import common.axi


object CacheCoreState extends ChiselEnum {
  val sIdle, sIfHit, sFindVictim, sWriteBackLatency,sWriteBack, sReadAddr, sReadData,sWriteMem2Cache, sPrepareCache,sWaitCacheLatency,sRespon,sLocked = Value
}


class CacheCore (
    ADDR_WIDTH: Int = 24,
    DATA_WIDTH: Int = 512,
    LOCK_WIDTH: Int = 1,
    ASSOCIATION: Int = 1,
    ENTRY_NUMBER : Int = 1024,
    LOCK_NUMBER : Int = 2,
    RAM_LATENCY : Int = 1
)extends Module {
    val io = IO(new Bundle{
        // SSD input commands.
        val request = new cache_request(ADDR_WIDTH, DATA_WIDTH, LOCK_WIDTH)
        val response = new cache_response(DATA_WIDTH)
        val mem_interface = new AXI(33, 512, 6, 0, 4)
    })
    val OFFSET_LEN = log2Ceil(DATA_WIDTH/8)
    val INDEX_LEN = log2Ceil(ENTRY_NUMBER/ASSOCIATION)
    val TAG_LEN   = ADDR_WIDTH - OFFSET_LEN - INDEX_LEN
    val ASSOCIATION_SHIFT = log2Ceil(ASSOCIATION)
    val SET_NUMBER = ENTRY_NUMBER/ASSOCIATION
    val addr_reg = RegInit(0.U(ADDR_WIDTH.W))
    val data_reg = RegInit(0.U(DATA_WIDTH.W))
    val mask_reg = RegInit(0.U((DATA_WIDTH/8).W))
    val lock_reg = RegInit(0.U(LOCK_WIDTH.W))

    val offset = addr_reg(OFFSET_LEN - 1 ,0)
    val index = addr_reg(INDEX_LEN + OFFSET_LEN - 1, OFFSET_LEN)
    val tag = addr_reg(ADDR_WIDTH - 1, ADDR_WIDTH - INDEX_LEN - OFFSET_LEN)
    assert(DATA_WIDTH <= 512 * 8, "Cache line size must not exceed 512 bytes")
    assert(ADDR_WIDTH <= 32, "Address width must be less than 32 bits")
    val DataRam = new(XRam(UInt(DATA_WIDTH.W), ENTRY_NUMBER,latency=RAM_LATENCY, use_musk=1))
    val TagReg = Vec.fill(ENTRY_NUMBER)(RegInit(0.U(TAG_LEN.W)))
    val ValidReg = Vec.fill(ENTRY_NUMBER)(RegInit(0.U(1.W)))
    val DirtyReg = Vec.fill(ENTRY_NUMBER)(RegInit(0.U(1.W)))
    val LockReg  = RegInit(VecInit(Seq.fill(SET_NUMBER)(VecInit(Seq.fill(LOCK_NUMBER)(0.U(TAG_LEN.W))))))
    val Clock_LRU_Reg = Vec.fill(ENTRY_NUMBER)(RegInit(0.U(1.W)))
    val Clock_pointer = Vec.fill(SET_NUMBER)(RegInit(0.U(log2Ceil(ASSOCIATION).W)))

    val TagWire = Wire(Vec(ASSOCIATION, UInt(TAG_LEN.W)))
    val ValidWire = Wire(Vec(ASSOCIATION, UInt(1.W)))
    val LockWire = Wire(Vec(LOCK_NUMBER, UInt(TAG_LEN.W)))
    val isLockWire = Wire(Vec(LOCK_NUMBER, Bool()))
    val ishit = Bool()
    val isdirty = Bool()
    val islock = Bool()
    val hit_index = Reg((log2Ceil(ENTRY_NUMBER)).W)
    val if_find_victim = Bool()
    val victim_index = Reg((log2Ceil(ENTRY_NUMBER)).W)
    val data_out = Wire(UInt(DATA_WIDTH.W))
    val data_in = Reg(UInt(DATA_WIDTH.W))
    val dataram_inex = Reg((log2Ceil(ENTRY_NUMBER)).W) //todo need to modified
    val (sWriteBackLatency_count, sWriteBackLatency_out) = Counter(clock&&(state===sWriteBackLatency), RAM_LATENCY)
    val (sWaitCacheLatency_count, sWaitCacheLatency_out) = Counter(clock&&(state===sWaitCacheLatency), RAM_LATENCY)
    import CacheCoreState._
    val state = RegInit(sIdle)

    switch(state){
        is(sIdle){
            when(io.request.req.fire()){
                state := sIfHit
            }
        }
        is(sIfHit){
            when(islock){
                state := sLocked
            }.otherwise{
                when(ishit){
                    state := sPrepareCache
                }.otherwise{
                    state := sFindVictim
                }
            }
        }
        is(sFindVictim){
            when(isdirty){
                state := sWriteBackLatency
            }.otherwise{
                state := sReadAddr
            }
        }
        is(sWriteBackLatency){
            when(sWriteBackLatency_out){
                state := sWriteBack
            }
        }
        is(sWriteBack){
            when(io.mem_interface.aw.fire()&&io.mem_interface.w.fire()){
                state := sReadAddr
            }
        }
        is(sReadAddr){
            when(io.mem_interface.ar.fire()){
                state := sReadData
            }
        }
        is(sReadData){
            when(io.mem_interface.r.fire()){
                state := sWriteMem2Cache
            }
        }
        is(sWriteMem2Cache){
                state := sPrepareCache  //todo add condition
            
        }
        is(sPrepareCache){
            state := sWaitCacheLatency
        }
        is(sWaitCacheLatency){
            when(sWaitCacheLatency_out){
                state := sRespon
            }
        }
        is(sRespon){
            when(io.response.fire()){
                state := sIdle
            }
        }
        is(sLocked){
            when(io.response.fire()){
                state := sIdle
            }
        }
    }

    when(state === sIdle){
        io.request.req.ready := 1.U
        addr_reg := io.request.req.bits.addr
        data_reg := io.request.req.bits.data
        mask_reg := io.request.req.bits.mask
        lock_reg := io.request.req.bits.lock
    }.otherwise{
        io.request.req.ready := 0.U
    }

    // ishit
    for(i <- 0 until ASSOCIATION){
        TagWire(i) := TagReg(index<<ASSOCIATION_SHIFT + i)
        ValidWire(i) := ValidReg(index<<ASSOCIATION_SHIFT + i)&(TagWire(i) === tag)
    }
    ishit := ValidWire.asUInt.orR

    for(i <- 0 until LOCK_NUMBER){
        LockWire(i) := LockReg(index)(i)
        isLockWire(i) := LockWire(i) === tag
    }
    islock := isLockWire.asUInt.orR


    // find hit index
    when(state === sIfHit){
        hit_index := index<<ASSOCIATION_SHIFT + PriorityEncoder(~ValidWire.asUInt)  //todo need to check
    }
    
    // find victim
    when(state === sFindVictim){
        when(Clock_LRU_Reg(index<<ASSOCIATION_SHIFT+Clock_pointer) === 1.U){
            Clock_LRU_Reg(index<<ASSOCIATION_SHIFT+Clock_pointer) === 0.U
            if_find_victim := 0.U
            when(Clock_pointer === (SET_NUMBER - 1).U){
                Clock_pointer := 0.U
            }.otherwise{
                Clock_pointer := Clock_pointer + 1.U
            }
        }.otherwise{
            if_find_victim := 1.U
        }
        victim_index := index<<ASSOCIATION_SHIFT + Clock_pointer
    }

    isdirty := DirtyReg(victim_index)

    DataRam.io.addr_b := dataram_inex
    data_out := DataRam.io.data_out_b


    io.response.valid := state === sRespon || state === sLocked
    io.response.bits.data := //todo need to modified
    io.response.bits.success := state === sRespon

    when(state === sReadData){
        data_in := io.mem_interface.r.bits.data
    }

    when(state === sWriteMem2Cache){
        DataRam.io.data_in_a := data_in
        DataRam.io.wr_en_a := 1.U
        DataRam.io.musk_a := Fill((DATA_WIDTH/8), 1.U)
        DataRam.io.addr_a := dataram_inex   //todo need to be checked
    }.elsewhen(state === sPrepareCache){
        when(musk_reg =/= 0.U){
            DataRam.io.wr_en_a := 1.U
            DataRam.io.musk_a := musk_reg
            DataRam.io.addr_a := dataram_inex
            DataRam.io.data_in_a := data_reg
        }.otherwise{   //* not used
            DataRam.io.wr_en_a := 0.U  
            DataRam.io.musk_a := 0.U
            DataRam.io.addr_a := dataram_inex
            DataRam.io.data_in_a := 0.U
        }
    }.otherwise{
        DataRam.io.wr_en_a := 0.U
        DataRam.io.musk_a := 0.U
        DataRam.io.addr_a := 0.U
        DataRam.io.data_in_a := 0.U
    }

    io.mem_interface.aw.valid := state === sWriteBack && io.mem_interface.aw.ready && io.mem_interface.w.ready
    io.mem_interface.w.valid := state === sWriteBack && io.mem_interface.aw.ready && io.mem_interface.w.ready
    io.mem_interface.aw.bits.addr := addr_reg
    io.mem_interface.aw.bits.len := 0.U
    io.mem_interface.aw.bits.size := 6.U
    io.mem_interface.aw.bits.burst := 0.U
    io.mem_interface.aw.bits.id := 0.U
    io.mem_interface.aw.bits.cache := 0.U
    io.mem_interface.w.bits.data := data_out
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

