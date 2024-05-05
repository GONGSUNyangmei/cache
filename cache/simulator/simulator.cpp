#include<iostream>
#include<string.h>
#include "Cache.hpp"
#include<fstream>
#include<random>
#include<ctime>
using namespace std;

int main() {
    int a=0;
    std::cout<<"Hello World"<<std::endl;
    std:string str = "LRU";
    
    //cache->read((u_int64_t)0);
    //cache->print_snapshot();
    char ch[64] ;
    ch[0] = 0x1a;
    ch[1] = 0x2b;
    // cache->write((u_int64_t)0,(void*)ch);
    // cache->write((u_int64_t)0x4000,(void*)ch);
    // cache->write((u_int64_t)0x8000,(void*)ch);
    // cache->write((u_int64_t)0xc000,(void*)ch);
    // cache->write((u_int64_t)0x10000,(void*)ch);
    // cache->write((u_int64_t)0x8000,(void*)ch);
    // cache->read((u_int64_t)0x10000);
    // cache->read((u_int64_t)0x10000);
    //read  from trace.txt and write/read to cache
    //FILE *fp;
 


   std::random_device rd;
    int read_num = 100;
    u_int64_t addr;

    //random read number interations

    u_int64_t * addr_array = new u_int64_t[1000];
    int random1;
    int random2;

    for(int j=1 ; j<=128 ; j=j*2){
        std::cout<<"Associativity: "<<j<<std::endl;
        Cache* cache = new Cache(65536, 64, j, 0, 0, 1, 10);
        for(int i = 0; i < read_num; i++) {
            random1 = (rd()%990)+10;
            random2 = (rd()%90)+10;
            for(int k=0; k<random1; k++){
                addr_array[k] = (rd()%8000)<<6;
            }
            for(int m=0; m<random2; m++){
                for(int n=0; n<random1; n++){
                    cache->read(addr_array[n]);
                }
            }
        }
        std::cout<<std::endl;
        cache->print_stats();
    }
   
    
    
    
    //std::cout<<"Data: "<<*(int*)cache->read((u_int64_t)12345678)<<std::endl;
    return 0;
}

