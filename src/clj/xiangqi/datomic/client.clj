(ns xiangqi.datomic.client
	(:require [datomic.client.api :as dca]))


(def cfg {:server-type :ion
					:region "eu-west-1" ;; e.g. us-east-1
					:system "vorpal-xiangqi"
					:creds-profile "vorpal-benh"
					:endpoint "http://entry.vorpal-xiangqi.eu-west-1.datomic.net:8182/"
					:proxy-port 8182})

(def client (delay (dca/client cfg)))
(def conn (delay (dca/connect @client {:db-name "xiangqi"})))

(defn retrieve-disposition
	[db board-eid]
	(dca/pull db
		'[{:board/next-player [:db/ident]}
			{:board/disposition [{:disposition/piece [:db/ident
																								{:piece/type [:db/ident]}
																								{:piece/player [:db/ident]}]}
													 {:disposition/location [:db/ident
																									 :location/x
																									 :location/y
																									 {:location/player [:db/ident]}]}]}]
		board-eid))