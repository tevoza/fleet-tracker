#include "request_handler.hpp"

class dbconn_fail_ex : public std::exception
{
    virtual const char* what() const throw()
    {
        return "Database connection could not be established";
    }
} dbconn_ex;

handler::handler()
{
    _db = new mysqlpp::Connection(false);
    if (_db == nullptr)
    {
        throw std::bad_alloc();
    }

    if (!(*_db).connect("FleetDB", "127.0.0.1", "root", "mypassword"))
    {
        throw dbconn_ex;
    } 
    _qry = new mysqlpp::Query(_db);
}

handler::~handler(){
    delete _db;
    delete _qry;
}

nlohmann::json handler::handle_request(const char* request) 
{
    nlohmann::json resp;
    try 
    {
        auto rec = nlohmann::json::parse(request);
        std::cout << rec.dump(4) << std::endl;
        int request_code = rec["req"];
        switch (request_code)
        {   
            case UPDATE_ID:
                std::cout << "UPDATE_ID" << std::endl;
                if (update_trucker_id(rec["uuid"], rec["id"]))
                {
                    resp["res"]=OK;

                    std::vector<mysqlpp::Row> v;
                    *_qry << "SELECT * FROM Trucker WHERE ID = " << rec["id"];
                    (*_qry).storein(v);
                    std::string name;
                    v[0]["Name"].to_string(name);
                    std::cout << name <<std::endl;
                    resp["trucker"]         = name;
                } else { resp["res"] = FAIL; }
                break;

            case VERIFY_ID:
                std::cout << "VERIFY_ID" << std::endl;
                resp["res"] = (verify_id(rec["uuid"], rec["id"])) ? OK : INVALID_CREDENTIALS;
                break;

            case UPDATE_LOGS:
                if (verify_id(rec["uuid"], rec["id"]))
                {
                    resp["res"] = insert_logs(rec["id"], rec["data"]);

                } else {
                    resp["res"] = INVALID_CREDENTIALS;
                }
                std::cout << "UPDATE_LOGS" << std::endl;
                break;
        }
    }
    catch (std::exception& e)
    {
       std::cerr << "Yikes: " << e.what() << "\n";
       resp["res"] = PARSE_FAIL;          
    }

    return resp;
}

bool handler::verify_id(const std::string& uuid, const int& id)
{
    std::vector<mysqlpp::Row> v;
    *_qry   << "SELECT * FROM Trucker WHERE "
            << "ID = " << id << " AND "
            << "AndroidID = '"<< uuid << "' AND "
            << "Verified = TRUE;";
    (*_qry).storein(v);

    return (v.size() != 0 ? true : false);
}

bool handler::update_trucker_id(const std::string& uuid, const int& id)
{
    std::vector<mysqlpp::Row> v;
    *_qry   << "SELECT * FROM Trucker WHERE "
            << "ID = " << id;
    (*_qry).storein(v);

    if (v.size() == 0){
        return false;
    }

    int verified = v[0]["Verified"];
    if (verified){
        return false;
    }

    *_qry   << "UPDATE Trucker SET "
            << "Verified = true, "
            << "AndroidID = '" << uuid << "' "
            << "WHERE ID = " << id;

    (*_qry).execute();

    return true;
}

REQUEST_RESULT handler::insert_logs(const int& id ,const nlohmann::json& logs)
{
   for(const auto& value: logs)
   {
        *_qry << "INSERT INTO TruckerLog " 
              << "(TruckerID, TimeStamp, Longitude, Latitude, Speed, Acceleration)"
              << "VALUES ("
              << id << ", "
              << value["tim"] << ", "
              << value["lon"] << ", "
              << value["lat"] << ", "
              << value["spd"] << ", "
              << value["acc"] << ");";
        (*_qry).execute();
    }
    std::cout << "inserted records; " << std::endl;

    return OK;
}
