#include <cstdlib>
#include <mysql++.h>
#include <iostream>
#include <boost/bind.hpp>
#include <boost/asio.hpp>
#include <boost/asio/ssl.hpp>
#include <nlohmann/json.hpp>

typedef boost::asio::ssl::stream<boost::asio::ip::tcp::socket> ssl_socket;

class session
{
public:
    session(boost::asio::io_context& io_context,
        boost::asio::ssl::context& context)
        : socket_(io_context, context)
    {
    }

    ssl_socket::lowest_layer_type& socket()
    {
        return socket_.lowest_layer();
    }

    void start()
    {
        socket_.async_handshake(boost::asio::ssl::stream_base::server,
            boost::bind(&session::handle_handshake, this,
            boost::asio::placeholders::error));
    }

    void handle_handshake(const boost::system::error_code& error)
    {
        if (!error)
        {
            std::cout << "handle_handshake: success\n";
            socket_.async_read_some(boost::asio::buffer(data_, max_length),
                boost::bind(&session::handle_read, this,
                boost::asio::placeholders::error,
                boost::asio::placeholders::bytes_transferred));
        }
        else
        {
            std::cout << "handle_handshake: fail\n";
            delete this;
        }
    }

    void handle_read(const boost::system::error_code& error, size_t bytes_transferred)
    {
        if (!error)
        {
            std::cout<<"handle_read: success\n";
            std::cout << "received : ";
            std::cout.write(data_, bytes_transferred);
            std::cout << "parsing..." << std::endl;
            try {
                auto rec = nlohmann::json::parse(data_);
                std::cout << rec <<std::endl;
                std::cout << "done." <<std::endl;

                mysqlpp::Connection conn(false);
                if (!conn.connect("FleetDB", "127.0.0.1", "root", "pass"))
                {
                    std::cout << "db -> failed\n";
                } 
                else
                {
                    mysqlpp::Query qry = conn.query();
                    qry << "INSERT INTO TruckerLog " 
                        << "(TruckerID, Timestamp, Longitude, Latitude, Speed, Acceleration) "
                        << "VALUES ("
                        << rec["id"] << ", "
                        << rec["time"] << ", "
                        << rec["long"] << ", "
                        << rec["lat"] << ", "
                        << rec["speed"] << ", "
                        << rec["accel"] << ");";
                    qry.execute();
                    
                    if (mysqlpp::StoreQueryResult res = qry.store())
                    {
                        std::cout << "added record";
                    }
                    else
                    {
                        std::cerr << "add record failed: " << qry.error() << std::endl;
                    }
                }
            }
            catch (std::exception& e)
            {
                std::cerr << "Yikes: " << e.what() << "\n";
                delete this;
            }
            /*  
            boost::asio::async_write(socket_,
                boost::asio::buffer(data_, bytes_transferred),
                boost::bind(&session::handle_write, this,
                boost::asio::placeholders::error));*/
            delete this;
        }
        else
        {
            std::cout<<"handle_read: fail\n" << error.message() << "\n";
            delete this;
        }
    }

    void handle_write(const boost::system::error_code& error)
    {
        if (!error)
        {
            socket_.async_read_some(boost::asio::buffer(data_, max_length),
                boost::bind(&session::handle_read, this,
                boost::asio::placeholders::error,
                boost::asio::placeholders::bytes_transferred));
        }
        else
        {
          delete this;
        }
    }

private:
    ssl_socket socket_;
    enum { max_length = 1024 };
    char data_[max_length];
};

class server
{
public:
    server(boost::asio::io_context& io_context, unsigned short port, char* pk_file,
            char* pem_file) : io_context_(io_context), acceptor_(io_context,
            boost::asio::ip::tcp::endpoint(boost::asio::ip::tcp::v4(), port)),
            context_(boost::asio::ssl::context::sslv23)
    {
        context_.set_options(
            boost::asio::ssl::context::default_workarounds
            | boost::asio::ssl::context::no_sslv2
            | boost::asio::ssl::context::sslv23
            | boost::asio::ssl::context::single_dh_use);
        //context_.set_password_callback(boost::bind(&server::get_password, this));
        context_.use_certificate_chain_file(pem_file);
        context_.use_private_key_file(pk_file, boost::asio::ssl::context::pem);

        start_accept();
    }

    std::string get_password() const
    {
        return "test";
    }

    void start_accept()
    {
        std::cout << "start_accept\n";
        session* new_session = new session(io_context_, context_);
        acceptor_.async_accept(new_session->socket(),
            boost::bind(&server::handle_accept, this, new_session,
            boost::asio::placeholders::error));
    }

    void handle_accept(session* new_session,
      const boost::system::error_code& error)
    {
        if (!error)
        {
            
            std::cout << "handle_accept:started\n";
            new_session->start();
        }
        else
        {
            std::cout << "handle_accept:failed\n";
            delete new_session;
        }
        start_accept();
    }

private:
    boost::asio::io_context& io_context_;
    boost::asio::ip::tcp::acceptor acceptor_;
    boost::asio::ssl::context context_;
};

int main(int argc, char* argv[])
{
    try
    {
        if (argc != 4)
        {
            std::cerr << "Usage: ssl_server <port> <private_key> <pem_file>\n";
            return 1;
        }

        boost::asio::io_context io_context;
        server s(io_context, std::atoi(argv[1]), argv[2], argv[3]);
        io_context.run();   
    }
    catch (std::exception& e)
    {
        std::cerr << "Exception: " << e.what() << "\n";
    }
    return 0;
}
