#!/usr/bin/env bash
cd /home/ben/dev/datomic/datomic-pro-0.9.5697
export PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH; bin/transactor config/dev-transactor.properties
