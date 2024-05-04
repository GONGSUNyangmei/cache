#include <Block.hpp>


Block::Block(size_t size) {
    valid = 0;
    dirty = 0;
    tag = 0;
    data = malloc(size);
    lru = 0;
}

Block::~Block() {
    free(data);
}

void Block::write(void* data) {
    memcpy(this->data, data, sizeof(data));
}

void* Block::get_data() {
    return data;
}

bool Block::is_valid() {
    return valid;
}

bool Block::is_dirty() {
    return dirty;
}

bool Block::is_hit(u_int64_t tag) {
    return this->tag == tag;
}

void Block::set_dirty() {
    dirty = 1;
}

void Block::reset_dirty() {
    dirty = 0;
}

void Block::set_valid() {
    valid = 1;
}

void Block::reset_valid() {
    valid = 0;
}

void Block::set_lru(u_int32_t lru) {
    this->lru = lru;
}   

u_int32_t Block::get_lru() {
    return lru;
}