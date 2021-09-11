#include "request_handler.hpp"

REQUEST_RESULT handler::handle_request(const char* request) 
{
    try 
    {
        auto rec = nlohmann::json::parse(request);
        std::cout << rec.dump(4) << std::endl;

        _db = mysqlpp::Connection(false);
        if (!_db.connect("FleetDB", "127.0.0.1", "root", "mypassword"))
        {
           std::cout << "db conn failed\n";
           return DB_CONN_FAILED;
        } 

    }
    catch (std::exception& e)
    {
       std::cerr << "Yikes: " << e.what() << "\n";
       return FAIL;
    }
    return OK;
}

REQUEST_RESULT handler::insert_logs()
{
//   for(const auto& value: rec["data"])
//   {
//        mysqlpp::Query qry = _db.query();
//        qry << "INSERT INTO TruckerLog " 
//          << "(TruckerID, TimeStamp, Longitude, Latitude, Speed, Acceleration)"
//          << "VALUES ("
//          << rec["id"] << ", "
//          << value["tim"] << ", "
//          << value["lon"] << ", "
//          << value["lat"] << ", "
//          << value["spd"] << ", "
//          << value["acc"] << ");";
//        qry.execute();
//
//        if (mysqlpp::StoreQueryResult res = qry.store())
//        {
//          std::cout << "added record";
//        }
//        else
//        {
//          std::cerr << "add record failed: " << qry.error() << std::endl;
//        }
//    }
}
