(ns xiangqi.service.secrets
  (:require [buddy.sign.jwt :as buddy]))

(defn game-cookie
  []
  (buddy/sign
    {:claims (pr-str (select-keys user-out [:user-id :roles :user-uuid :username]))}
    (:session-secret (:config component))
    {:alg :hs256
     :exp expiry}))