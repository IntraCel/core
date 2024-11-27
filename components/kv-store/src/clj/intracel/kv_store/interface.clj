(ns clj.intracel.kv-store.interface
  "The `clj.intracel.kv-store.interface` namespace defines polylith interface for the KV-Store. 
Generally, this is the namespace users of the KV-Store will put in their [[require]] statement 
to use the component."
  (:require [clj.intracel.api.kv-store :as kvs-api]
            [clj.intracel.kv-store.lmdb :as lmdb]))

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
  ^kvs-api/KVStoreContext [env-opts]
  (lmdb/create-context env-opts))

(defn db
  "Returns a hosted embedded database. See [[clj.intracel.api.kv-store/KVStoreDb]].
  
  Depends on: [[create-context]]"
  ^kvs-api/KVStoreDbContext [^kvs-api/KVStoreContext kvs-ctx db-name db-opts]
  (lmdb/db kvs-ctx db-name db-opts))

(defn set-key-serde
  "Sets the default key SerDe used for serializing and deserializing to and from the database. See [[clj.intracel.api.kv-store/KVStoreDb]].
  
  Depends on: [[db]]"
  [^kvs-api/KVStoreDbContext kvs-db ^kvs-api/KVSerde key-serde]
  (lmdb/set-key-serde kvs-db key-serde))

(defn set-val-serde
  "Sets the default value SerDe used for serializing and deserializing to and from the database. See [[clj.intracel.api.kv-store/KVStoreDb]].

Depends on: [[db]]"
  [^kvs-api/KVStoreDbContext kvs-db ^kvs-api/KVSerde val-serde]
  (lmdb/set-val-serde kvs-db val-serde))

(defn put
  "Puts a value into the KV-Store. This is a multi-arity function with the 3-parameter version using the default SerDe (see [[set-key-serde]], [[set-val-serde]]) and the 5-parameter version allowing the caller to provide its own key and value SerDes. See [[clj.intracel.api.kv-store/KVStoreDb]].

Depends on: [[db]]."
  ([^kvs-api/KVStoreDbContext kvs-db key value]
  (lmdb/put kvs-db key value))
  ([^kvs-api/KVStoreDbContext kvs-db value ^kvs-api/KVSerde key-serde ^kvs-api/KVSerde val-serde]
  (lmdb/put kvs-db key value key-serde val-serde)))

(defn set-pre-get-hook
  "This enables the caller to customize the behavior performed when doing a key look-up in [[get]] by allowing caller code to pre-process the key. See [[clj.intracel.api.kv-store/KVStoreDb]].

Depends on: [[db]]"
  [^kvs-api/KVStoreDbContext kvs-db pre-fn]
  (lmdb/set-pre-get-hook kvs-db pre-fn))

(defn get
  "Gets a value from the KV-Store. If set, the `pre-get-hook` fn will be called on the key provided. This is a multi-arity function with the 2-parameter version using the default SerDe (see [[set-key-serde]], [[set-val-serde]]) and the 4-parameter version allowing the caller to provide its own key and value SerDes. See [[clj.intracel.api.kv-store/KVStoreDb]].

Depends on: [[db]]"
  ([^kvs-api/KVStoreDbContext kvs-db key]
  (lmdb/get kvs-db key))
  ([^kvs-api/KVStoreDbContext kvs-db key ^kvs-api/KVSerde key-serde ^kvs-api/KVSerde val-serde]
  (lmdb/get kvs-db key key-serde val-serde)))

(defn delete
  "Removes a key and its corresponding value from the KV-Store. This is a multi-arity function with the 2-parameter version using the default SerDe (see [[set-key-serde]]), and the 3-parameter version allowing the caller to provide its own key SerDe. See [[clj.intracel.api.kv-store/KVStoreDb]].

Depends on: [[db]]"
  ([^kvs-api/KVStoreDbContext kvs-db key]
  (lmdb/delete kvs-db key))
  ([^kvs-api/KVStoreDbContext kvs-db key ^kvs-api/KVSerde key-serde]
  (lmdb/delete kvs-db key key-serde)))