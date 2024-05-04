#include<Block.hpp>
#include<Cache.hpp>
#include<math.h>

Set::Set(int block_num,int block_offset,int index_len,Replacement* replacer) {
    size_t size = 2<<block_offset;
    blocks = new Block*[block_num];
    for(int i = 0; i < block_num; i++) {
        blocks[i] = new Block(size);
    }
    this->block_num = block_num;
    this->block_offset = block_offset;
    this->index_len = index_len;
    this->replacer = replacer; 
}

Set::~Set() {
    for(int i = 0; i < block_num; i++) {
        delete blocks[i];
    }
    delete[] blocks;
}

void* Set::read(u_int64_t addr) {
    u_int64_t tag = addr >> (index_len + block_offset);
    for(int i = 0; i < block_num; i++) {
        if(blocks[i]->is_hit(tag)) {
            return blocks[i]->get_data();
        }
    }
    int victim = replacer->get_victim(blocks);
    blocks[victim]->write((void*)addr);
    replacer->update(1,victim,blocks);
    return blocks[victim]->get_data();
}

void Set::write(u_int64_t addr, void* data) {
    u_int64_t tag = addr >> (index_len + block_offset);
    for(int i = 0; i < block_num; i++) {
        if(blocks[i]->is_hit(tag)) {
            blocks[i]->write(data);
            return;
        }
    }
    int victim = replacer->get_victim(blocks);
    blocks[victim]->write(data);
    replacer->update(1,victim,blocks);
}

void Set::static_update() {
    replacer->update(0,0,blocks);
}

Cache::Cache(int size, int block_size, int associativity, int write_policy, int write_miss_policy, int hit_latency, int miss_latency) {
    Replacement* replacer ;
    //if(replace_strategy == "LRU") {
        replacer = new LRU(associativity);
    // }else{
    //     replacer = new LRU(associativity);
    // }
    int set_num = size / (block_size * associativity);
    int block_num = associativity;
    int block_offset = std::log2(block_size);
    int index_len = std::log2(set_num);
    sets = new Set*[set_num];
    for(int i = 0; i < set_num; i++) {
        sets[i] = new Set(block_num,block_offset,index_len,replacer);
    }
    this->size = size;
    this->set_num = set_num;
    this->block_size = block_size;
    this->associativity = associativity;
    this->write_policy = write_policy;  //write through or write back
    this->write_miss_policy = write_miss_policy; //write allocate or write no allocate
    this->hit_latency = hit_latency;
    this->miss_latency = miss_latency;
}

Cache::~Cache() {
    for(int i = 0; i < set_num; i++) {
        delete sets[i];
    }
    delete[] sets;
}

void Cache::read(u_int64_t addr) {
    u_int64_t index = (addr >> (int)(std::log2(block_size))) & (set_num - 1);
    for(int i = 0; i < set_num; i++) {
        if(i == index) {
            sets[i]->read(addr);
            return;
        }
        else{
            sets[i]->static_update();
        }
    }
}

void Cache::write(u_int64_t addr) {
    u_int64_t index = (addr >> (int)(std::log2(block_size))) & (set_num - 1);
    for(int i = 0; i < set_num; i++) {
        if(i == index) {
            sets[i]->write(addr, (void*)addr);
            return;
        }
        else{
            sets[i]->static_update();
        }
    }
}