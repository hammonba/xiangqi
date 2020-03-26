(ns xiangqi.datomic.peer
	(:require
	 [datomic.client.api :as dca]))


;(def peer (d/connect "datomic:dev://localhost:4334/xiangqi"))

(def cfg {:server-type :peer-server
					:access-key "myaccesskey"
					:secret "mysecret"
					:endpoint "localhost:8998"
					:validate-hostnames false})
(def client (delay (dca/client cfg)))
(def conn (delay (dca/connect @client {:db-name "xiangqi"})))

;(d/delete-database client {:db-name "xiangiqqi"})
(comment
	(xtx/transact-tx-matrix
		dp/conn
		(get-in (clojure.edn/read (java.io.PushbackReader. (jio/reader (jio/file "/home/ben/dev/clojure/xiangqi/resources/datomic-schema.edn"))))
			[:initial-config :txes]))
	)
