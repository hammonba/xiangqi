#!/usr/bin/env bash
cd /home/ben/dev/datomic/datomic-pro-0.9.6045
export PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH;  bin/console -p 8081 dev datomic:dev://localhost:4334/