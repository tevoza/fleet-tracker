FROM gcc:latest

COPY . /usr/src/fleet-io

WORKDIR /usr/src/fleet-io

RUN make

CMD ["./start_server"]