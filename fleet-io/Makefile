CC=g++
CFLAGS :=-c -Wall -g -I/usr/include/mysql -I/usr/include/mysql++
LDFLAGS := -lboost_regex -lboost_iostreams -lpthread -lboost_system -lcrypto -lssl -std=c++11 -L/usr/lib
LDLIBS := -lmysqlpp -lmysqlclient

all: ssl_server

ssl_server: ssl_server.o
	$(CC) $(LDFLAGS) $(LDLIBS) ssl_server.o -o ssl_server
ssl_server.o: ssl_server.cpp
	$(CC) $(CFLAGS) $(LDLIBS)  ssl_server.cpp $(LDFLAGS)

clean:
	rm -f *.o ssl_server ssl_client
