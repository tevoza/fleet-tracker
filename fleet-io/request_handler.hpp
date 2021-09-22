#ifndef REQUEST_HANDLER_HPP
#define REQUEST_HANDLER_HPP

#include <iostream>
#include <mysql++.h>
#include <nlohmann/json.hpp>
#include <typeinfo>

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
    DB_CONN_FAILED      = 3,
    PARSE_FAIL          = 4
};

class handler
{
public:
    handler();
    ~handler();
    nlohmann::json handle_request(const char* request);
private:
    REQUEST_RESULT insert_logs(const int& id ,const nlohmann::json& logs);
    mysqlpp::Connection* _db;
    mysqlpp::Query* _qry;
    bool verify_id(const std::string& uuid, const int& id);
    bool update_trucker_id(const std::string& uuid, const int& id);
};

#endif
