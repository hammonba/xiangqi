(ns xiangqi.datomic.peer
	(:require
						[datomic.client.api :as dca]))


;(def peer (d/connect "datomic:dev://localhost:4334/xiangqi"))

(def cfg {:server-type :peer-server
					:access-key "myaccesskey"
					:secret "mysecret"
					:endpoint "localhost:8998"})
(def client (dca/client cfg))
(def conn (dca/connect client {:db-name "xiangiqqi"}))
