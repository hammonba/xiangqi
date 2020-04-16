#!/usr/bin/env bash
cd /home/ben/dev/datomic/datomic-pro-0.9.6045
export PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH; bin/run -m datomic.peer-server -h localhost -p 8998 -a myaccesskey,mysecret -d xiangqi,datomic:dev://localhost:4334/xiangqi -d board,datomic:dev://localhost:4334/board -d game,datomic:dev://localhost:4334/game -d user,datomic:dev://localhost:4334/user
