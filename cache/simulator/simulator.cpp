#include<iostream>
#include<string.h>
#include "Cache.hpp"

using namespace std;

int main() {
    int a=0;
    std::cout<<"Hello World"<<std::endl;
    std:string str = "LRU";
    Cache* cache = new Cache(1024, 64, 4, 0, 0, 1, 10);
    // cache->read((u_int64_t)12345678);
    // cache->write(0x12345678);
    return 0;
}