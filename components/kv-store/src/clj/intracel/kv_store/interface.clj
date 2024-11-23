(ns clj.intracel.kv-store.interface
  (:require [clj.intracel.api.kv-store :as kvs-api]))

(defn start 
  "Starts up the KV-Store context which hosts the embedded database.
   | Parameter   | Description |
   | ------------|-------------|
   | `dir`       | This is the location on the filesystem where the KV-Store will persist data. |
   | `env-opts`  | Map containing options to adjust the behavior of the environment at start-up.|
   |             | | :Key:                              | :Description: | 
   |             | |------------------------------------| --------------|
   |             | | `:intracel/keyspace-max-mem-size`  | The total size (in bytes) allowed.    | 
   |             | | `:intracel/num-dbs`                | The number of independent, concurrent DB objects to support.| 
   |             | | `:intracel/keyspace-max-mem-size`  | The total size (in bytes) allowed.    | |"
  ^kvs-api/KVStoreContext [dir env-opts]
  )