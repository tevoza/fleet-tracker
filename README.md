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
### aspects
 - kotlin
 - power usage and running in the background
 - mvvm software design architecture
 - state persistence and cache database
 - permissions
 - ssl connection

## **i/o server**
### aspects
 - c++
 - asynchronous programming
 - singleton design pattern for data connector

## **web app**
### aspects
 - c# (back-end), html, css, javascript on the dotnet core framework.
 - mvc software design pattern for achieving clean separation of concerns from display and business logic, etc.
 - use of google maps javascript api to draw trucker routes on google map widget.
