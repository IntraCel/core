(ns clj.intracel.kv-store.interface
  (:require [clj.intracel.api.kv-store :as kvs-api]))

(defn create-context
  "Starts up the KV-Store context which hosts the embedded database.
   | Parameter   | Description |
   | ------------|-------------|
   | `env-opts`  | Map containing options to adjust the behavior of the environment at start-up.|
   |             | | :Key:                              | :Description: | 
   |             | |------------------------------------| --------------|
   |             | | `:intracel/keyspace-max-mem-size`  | The total size (in bytes) allowed.    | 
   |             | | `:intracel/num-dbs`                | The number of independent, concurrent DB objects to support.|
   |             | | `:intracel/storage-path`           | Local filesystem path where the data will be persisted to disk.||
   Returns:
   A `clj.intracel.api.kv-store/KVStoreContext`"
  ^kvs-api/KVStoreContext [env-opts])

(defn db
  "Returns a hosted embedded database. See [[clj.intracel.api.kv-store/KVStoreDb]].
  
  Depends on: [[create-context]]"
  ^kvs-api/KVStoreDbContext [^kvs-api/KVStoreContext kvs-ctx db-name db-opts])