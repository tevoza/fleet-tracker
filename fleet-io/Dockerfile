FROM manjarolinux/base

COPY . /usr/src/fleet-io
WORKDIR /usr/src/fleet-io

RUN pacman -Syy
RUN pacman -S --noconfirm --needed base-devel boost boost-libs mysql++ nlohmann-json vim

RUN make clean
RUN make 
EXPOSE 1234 3306

RUN chmod +x ./start_server.sh

CMD ["./start_server.sh"]
