`timescale 1ns / 1ns
module tb_CacheCore(

    );

reg                 clock                         =0;
reg                 reset                         =0;
wire                io_request_ready              ;
reg                 io_request_valid              =0;
reg       [23:0]    io_request_bits_addr          =0;
reg       [511:0]   io_request_bits_data          =0;
reg       [63:0]    io_request_bits_mask          =0;
reg                 io_request_bits_lock          =0;
reg       [3:0]     io_request_bits_port          =0;
reg                 io_response_ready             =0;
wire                io_response_valid             ;
wire      [511:0]   io_response_bits_data         ;
wire                io_response_bits_success      ;
reg                 io_mem_interface_aw_ready     =0;
wire                io_mem_interface_aw_valid     ;
wire      [32:0]    io_mem_interface_aw_bits_addr ;
wire      [1:0]     io_mem_interface_aw_bits_burst;
wire      [3:0]     io_mem_interface_aw_bits_cache;
wire      [5:0]     io_mem_interface_aw_bits_id   ;
wire      [3:0]     io_mem_interface_aw_bits_len  ;
wire                io_mem_interface_aw_bits_lock ;
wire      [2:0]     io_mem_interface_aw_bits_prot ;
wire      [3:0]     io_mem_interface_aw_bits_qos  ;
wire      [3:0]     io_mem_interface_aw_bits_region;
wire      [2:0]     io_mem_interface_aw_bits_size ;
reg                 io_mem_interface_ar_ready     =0;
wire                io_mem_interface_ar_valid     ;
wire      [32:0]    io_mem_interface_ar_bits_addr ;
wire      [1:0]     io_mem_interface_ar_bits_burst;
wire      [3:0]     io_mem_interface_ar_bits_cache;
wire      [5:0]     io_mem_interface_ar_bits_id   ;
wire      [3:0]     io_mem_interface_ar_bits_len  ;
wire                io_mem_interface_ar_bits_lock ;
wire      [2:0]     io_mem_interface_ar_bits_prot ;
wire      [3:0]     io_mem_interface_ar_bits_qos  ;
wire      [3:0]     io_mem_interface_ar_bits_region;
wire      [2:0]     io_mem_interface_ar_bits_size ;
reg                 io_mem_interface_w_ready      =0;
wire                io_mem_interface_w_valid      ;
wire      [511:0]   io_mem_interface_w_bits_data  ;
wire                io_mem_interface_w_bits_last  ;
wire      [63:0]    io_mem_interface_w_bits_strb  ;
wire                io_mem_interface_r_ready      ;
reg                 io_mem_interface_r_valid      =0;
reg       [511:0]   io_mem_interface_r_bits_data  =0;
reg                 io_mem_interface_r_bits_last  =0;
reg       [1:0]     io_mem_interface_r_bits_resp  =0;
reg       [5:0]     io_mem_interface_r_bits_id    =0;
wire                io_mem_interface_b_ready      ;
reg                 io_mem_interface_b_valid      =0;
reg       [5:0]     io_mem_interface_b_bits_id    =0;
reg       [1:0]     io_mem_interface_b_bits_resp  =0;

IN#(605)in_io_request(
        clock,
        reset,
        {io_request_bits_addr,io_request_bits_data,io_request_bits_mask,io_request_bits_lock,io_request_bits_port},
        io_request_valid,
        io_request_ready
);
// addr, data, mask, lock, port
// 24'h0, 512'h0, 64'h0, 1'h0, 4'h0

OUT#(513)out_io_response(
        clock,
        reset,
        {io_response_bits_data,io_response_bits_success},
        io_response_valid,
        io_response_ready
);
// data, success
// 512'h0, 1'h0

OUT#(64)out_io_mem_interface_aw(
        clock,
        reset,
        {io_mem_interface_aw_bits_addr,io_mem_interface_aw_bits_burst,io_mem_interface_aw_bits_cache,io_mem_interface_aw_bits_id,io_mem_interface_aw_bits_len,io_mem_interface_aw_bits_lock,io_mem_interface_aw_bits_prot,io_mem_interface_aw_bits_qos,io_mem_interface_aw_bits_region,io_mem_interface_aw_bits_size},
        io_mem_interface_aw_valid,
        io_mem_interface_aw_ready
);
// addr, burst, cache, id, len, lock, prot, qos, region, size
// 33'h0, 2'h0, 4'h0, 6'h0, 4'h0, 1'h0, 3'h0, 4'h0, 4'h0, 3'h0

OUT#(64)out_io_mem_interface_ar(
        clock,
        reset,
        {io_mem_interface_ar_bits_addr,io_mem_interface_ar_bits_burst,io_mem_interface_ar_bits_cache,io_mem_interface_ar_bits_id,io_mem_interface_ar_bits_len,io_mem_interface_ar_bits_lock,io_mem_interface_ar_bits_prot,io_mem_interface_ar_bits_qos,io_mem_interface_ar_bits_region,io_mem_interface_ar_bits_size},
        io_mem_interface_ar_valid,
        io_mem_interface_ar_ready
);
// addr, burst, cache, id, len, lock, prot, qos, region, size
// 33'h0, 2'h0, 4'h0, 6'h0, 4'h0, 1'h0, 3'h0, 4'h0, 4'h0, 3'h0

OUT#(577)out_io_mem_interface_w(
        clock,
        reset,
        {io_mem_interface_w_bits_data,io_mem_interface_w_bits_last,io_mem_interface_w_bits_strb},
        io_mem_interface_w_valid,
        io_mem_interface_w_ready
);
// data, last, strb
// 512'h0, 1'h0, 64'h0

IN#(521)in_io_mem_interface_r(
        clock,
        reset,
        {io_mem_interface_r_bits_data,io_mem_interface_r_bits_last,io_mem_interface_r_bits_resp,io_mem_interface_r_bits_id},
        io_mem_interface_r_valid,
        io_mem_interface_r_ready
);
// data, last, resp, id
// 512'h0, 1'h0, 2'h0, 6'h0

IN#(8)in_io_mem_interface_b(
        clock,
        reset,
        {io_mem_interface_b_bits_id,io_mem_interface_b_bits_resp},
        io_mem_interface_b_valid,
        io_mem_interface_b_ready
);
// id, resp
// 6'h0, 2'h0


CacheCore CacheCore_inst(
        .*
);

/*
addr,data,mask,lock,port
in_io_request.write({24'h0,512'h0,64'h0,1'h0,4'h0});

data,last,resp,id
in_io_mem_interface_r.write({512'h0,1'h0,2'h0,6'h0});

id,resp
in_io_mem_interface_b.write({6'h0,2'h0});

*/

initial begin
        reset <= 1;
        clock = 1;
        #1000;
        reset <= 0;
        #100;
        out_io_response.start();
        out_io_mem_interface_aw.start();
        
        out_io_mem_interface_w.start();
        #50;
        //* mormal read & cache miss
//        in_io_request.write({24'h0,512'h0,64'h0,1'h0,4'h0});
//        #50;
        
//        wait(io_mem_interface_ar_ready&io_mem_interface_ar_valid) 
//        in_io_mem_interface_r.write({512'h1,1'h1,2'h0,6'h0});
//        #50;
        //* write miss  
        in_io_request.write({24'h40,512'h2,64'hffffffffffffffff,1'h0,4'h0});  
        #50;
        out_io_mem_interface_ar.start();      
        wait(io_mem_interface_ar_ready&io_mem_interface_ar_valid) 
        in_io_mem_interface_r.write({512'h3,1'h1,2'h0,6'h0});
        #50;
//        //* mormal read & cache hit
//        in_io_request.write({24'h0,512'h0,64'h0,1'h0,4'h0});
//        #50;
//        //* mormal read & cache hit
//        in_io_request.write({24'h40,512'h0,64'h0,1'h0,4'h0});
//        #50;
        //* read miss, cache dirty
        in_io_request.write({24'h10040,512'h0,64'h0,1'h0,4'h0});        
        wait(io_mem_interface_ar_ready&io_mem_interface_ar_valid) 
        in_io_mem_interface_r.write({512'h4,1'h1,2'h0,6'h0});
        #50;
        in_io_request.write({24'h20040,512'h0,64'h0,1'h0,4'h0});        
        wait(io_mem_interface_ar_ready&io_mem_interface_ar_valid) 
        in_io_mem_interface_r.write({512'h4,1'h1,2'h0,6'h0});
        #50;
        in_io_request.write({24'h30040,512'h0,64'h0,1'h0,4'h0});        
        wait(io_mem_interface_ar_ready&io_mem_interface_ar_valid) 
        in_io_mem_interface_r.write({512'h4,1'h1,2'h0,6'h0});
        #50;
        in_io_request.write({24'h40040,512'h0,64'h0,1'h0,4'h0});        
        wait(io_mem_interface_ar_ready&io_mem_interface_ar_valid) 
        in_io_mem_interface_r.write({512'h4,1'h1,2'h0,6'h0});
        #50;
//        // write hit
//        in_io_request.write({24'h10040,512'h5,64'hffffffffffffffff,1'h0,4'h0});
//        #50;
//        //read hit 
//        in_io_request.write({24'h10040,512'h0,64'h0,1'h0,4'h0});
//        #50;
end

always #5 clock=~clock;
endmodule