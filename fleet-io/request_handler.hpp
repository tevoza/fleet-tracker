#ifndef REQUEST_HANDLER_HPP
#define REQUEST_HANDLER_HPP

#include <iostream>
#include <mysql++.h>
#include <nlohmann/json.hpp>

enum REQUEST_CODE
{
    UPDATE_LOG  = 1,
    UPDATE_ID   = 2
};

enum REQUEST_RESULT
{
    OK          = 1,
    FAIL        = 2,
    INVALID_ID  = 3
};

class handler
{
public:
    void test();
    REQUEST_RESULT handle_request(const char* request);
};

#endif
