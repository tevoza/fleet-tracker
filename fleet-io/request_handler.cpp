#include "request_handler.hpp"

handler::handler(char* request) 
{
    try 
    {
       auto rec = nlohmann::json::parse(request);
       std::cout << rec <<std::endl;
       std::cout << "done." <<std::endl;

       //mysqlpp::Connection conn(false);
       //if (!conn.connect("FleetDB", "127.0.0.1", "root", "pass"))
       //{
       //    std::cout << "db -> failed\n";
       //} 
       //else
       //{
       //    mysqlpp::Query qry = conn.query();
       //    qry << "INSERT INTO TruckerLog " 
       //        << "(TruckerID, Timestamp, Longitude, Latitude, Speed, Acceleration) "
       //        << "VALUES ("
       //        << rec["id"] << ", "
       //        << rec["time"] << ", "
       //        << rec["long"] << ", "
       //        << rec["lat"] << ", "
       //        << rec["speed"] << ", "
       //        << rec["accel"] << ");";
       //    qry.execute();
       //    
       //    if (mysqlpp::StoreQueryResult res = qry.store())
       //    {
       //        std::cout << "added record";
       //    }
       //    else
       //    {
       //        std::cerr << "add record failed: " << qry.error() << std::endl;
       //    }
       //}
    }
    catch (std::exception& e)
    {
       std::cerr << "Yikes: " << e.what() << "\n";
       //delete this;
    }
}
