#ifndef REQUEST_HANDLER_HPP
#define REQUEST_HANDLER_HPP

#include <iostream>
#include <mysql++.h>
#include <nlohmann/json.hpp>

enum REQUEST_CODE
{
    UPDATE_ID   = 1,
    VERIFY_ID   = 2,
    UPDATE_LOGS = 3,
};

enum REQUEST_RESULT
{
    FAIL                = 0,
    OK                  = 1,
    INVALID_CREDENTIALS = 2,
    DB_CONN_FAILED      = 4
};

class handler
{
public:
    REQUEST_RESULT handle_request(const char* request);
private:
    REQUEST_RESULT insert_logs();
    mysqlpp::Connection _db;
};

#endif
