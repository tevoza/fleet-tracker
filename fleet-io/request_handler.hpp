#ifndef REQUEST_HANDLER_HPP
#define REQUEST_HANDLER_HPP

#include <iostream>
#include <mysql++.h>
#include <nlohmann/json.hpp>

enum request_code
{
    UPDATE_LOG  = 1,
    UPDATE_ID   = 2
};

class handler
{
public:
    handler(char* request);
    ~handler();
};

#endif
