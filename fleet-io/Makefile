CC=g++
CFLAGS :=-c -Wall -g -I/usr/include/mysql -I/usr/include/mysql++
LDFLAGS := -lboost_regex -lboost_iostreams -lpthread -lboost_system -lcrypto -lssl -std=c++20 -L/usr/lib
LDLIBS := -lmysqlpp -lmysqlclient

all: ssl_server 

ssl_server: ssl_server.o request_handler.o
	$(CC) $(LDFLAGS) $(LDLIBS) ssl_server.o request_handler.o -o ssl_server
ssl_server.o: ssl_server.cpp
	$(CC) $(CFLAGS) $(LDLIBS)  ssl_server.cpp $(LDFLAGS)
request_handler.o: request_handler.cpp
	$(CC) $(CFLAGS) $(LDLIBS)  request_handler.cpp  $(LDFLAGS)

clean:
	rm -f *.o ssl_server ssl_client
