(ns clj.intracel.api.kv-store)

(defrecord KVStoreContext [ctx])

(defprotocol KVStoreDb 
  (start [_ ]))