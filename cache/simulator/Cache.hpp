#include<iostream>



class block {
    public:
    block(size_t size);
    ~block();
    private:
    int valid;
    int dirty;
    uint_64_t tag;
    void* data;
    int lru;
};


class set {
    public:
    set(int block_num,int block_offset,int index); //todo add a function pointer to the replacement policy
    ~set();
    void read(uint_64_t addr);
    void write(uint_64_t addr);
    void print();
    private:
    block* blocks;
    int block_num;
    int block_offset;
    int index;
};

class Cache {
    public:
        Cache(int size, int block_size, int associativity, int write_policy, int write_miss_policy, int hit_latency, int miss_latency);
        ~Cache();
        void read(long addr);
        void write(long addr);
        void print();
        void print_stats();
        void print_config();
        void print_sets();
        void print_set(int set);
        void print_set(int set, int way);
        void print_set(int set, int way, int block);
        void print_block(long addr);
        void print_block(long addr, int way);
        void print_block(long addr, int way, int block);
        void print_block(long addr, int way, int block, int offset);
        void print_block(long addr, int way, int block, int offset, int tag);
        void print_block(long addr, int way, int block, int offset, int tag, int valid);
        void print_block(long addr, int way, int block, int offset, int tag, int valid, int dirty);
        void print_block(long addr, int way, int block, int offset, int tag, int valid, int dirty, int lru);
        void print_block(long addr, int way, int block, int offset, int tag, int valid, int dirty, int lru, int data);
        void print_block(long addr, int way, int block, int offset, int tag, int valid, int dirty, int lru, int data, int data_valid);
        void print_block(long addr, int way, int block, int offset, int tag, int valid, int dirty, int lru, int data, int data_valid, int data_dirty);
        void print_block(long addr, int way, int block, int offset, int tag, int valid, int dirty, int lru, int data, int data_valid, int data_dirty, int data_lru);
        void print_block(long addr, int way, int block, int offset, int tag, int valid, int dirty, int lru, int data, int data_valid, int data_dirty, int data_lru, int data_tag);
        void print_block(long addr, int way, int block, int offset, int tag, int valid, int dirty, int lru, int data, int data_valid, int data_dirty, int data_lru, int data_tag, int data_addr);
    private:
        set* sets;
        int size;
        int block_size;
        int associativity;
        int write_policy;
        int write_miss_policy;
        int hit_latency;
        int miss_latency;
        int num_sets;
        int num_blocks;
        int num_offset_bits;
        int num_index_bits;
        int num_tag_bits;
        int num_block_offset_bits;
        int num_block_index_bits;
        int num_block_tag_bits;
        int num_block_offset;
        int num_block_index;
        int num_block_tag;
        int num_block;
        int num_offset;
        int num_index;
        int num_tag;
        int num_lru;
        int num_dirty;
        int num_valid;
        int num_data;
        int num_data_valid;
        int num_data_dirty;
        int num_data_lru;
        int num_data_tag;
        int num_data_addr;
        int num_data_offset;
        int num_data_index;
        int num_data_tag;
        int num_data_block;
        int num_data_way;
        int num_data_set;
        int num_data_cache;
        int num_data_hit;
        int num_data_miss;
        int num_data_load;
        int num_data_store;
        int num_data_victim;
        int num_data_swap;
        int num_data_write;
        int num_data_read;
        int num_data_write_miss;
        int num_data_read_miss;
        int num_data_write_hit;
        int num_data_read_hit;
        int num_data_write_miss_allocate;
        int num_data_write_miss_no_allocate;
        int num_data_write_miss_write_through;
        int num_data_write_miss_write_back;
        int num_data_write_miss_write_allocate;
        int num_data_write_miss_no_allocate;
        int num_data_write_miss_write_through;
        int num_data_write_miss_write_back;
        int num_data_write_miss_write_allocate;
        int num_data_write_miss_no_allocate;
        int num_data_write_miss_write_through;
        int num_data_write_miss_write_back;
        int num_data_write_miss_write_allocate;
        int num_data_write_miss_no_allocate;
        int num_data_write_miss_write_through;
        int num_data_write_miss_write_back;
        int num_data_write_miss_write_allocate;
        int num_data_write_miss_no_allocate;
        int num_data_write_miss_write_through;
        int num_data_write_miss_write_back;
        int num_data_write_miss_write_allocate;
        int num_data_write_miss_no_allocate;
        int num_data

}