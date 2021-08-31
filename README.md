# **fleet-tracker - final year project (wip)**
A fleet tracking solution which allows trucking fleet managers to manage and monitor their fleets.  

- Fleet managers can log in and monitor their employees' live location, speed etc. in a .NET mvc web app.
- Location-logging is performed with android devices which truckers must use while driving their routes. This is implemented using Kotlin.
- An asynchronous i/o server handles transactions between android devices and the central database using an encrypted ssl connection. Asynchronous functionality is achieved using the `asio` library in c++. A text protocol is designed using a json strucutre over ssl sockets.
 - (future expansion) provided CAN bus interface to log extra information from trucks. 

# **overview**
![Project Description](docs/container.png)

# **requirements**
an extract from the initial project description is given below.  
![Project Description](docs/desc.png)

## **android application**
### functionality
 - log sensor data every 2 minutes, that is:
    1. gps co-ordinates
    2. speed
    3. acceleration
    4. altitude(optional) - typically available from GPS system.
 - store in local caching database
 - send log data to server

### aspects
 - power usage and running in the background
 - permissions
 - ssl connection

## **i/o server**
### functionality
 - asynchronous serve multiple clients over ssl connection
 - relay log data to central database
### aspects
 - ssl server
 - ssl 3-way handshake and certificates
 - asynchronously server multiple clients
