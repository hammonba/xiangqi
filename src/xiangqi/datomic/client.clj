(ns xiangqi.datomic.client
	(:require [datomic.client.api :as d]))


(def cfg {:server-type :ion
					:region "eu-west-1" ;; e.g. us-east-1
					:system "vorpal-xiangqi"
					:creds-profile "vorpal-benh"
					:endpoint "http://entry.vorpal-xiangqi.eu-west-1.datomic.net:8182/"
					:proxy-port 8182})

(def client (d/client cfg))
