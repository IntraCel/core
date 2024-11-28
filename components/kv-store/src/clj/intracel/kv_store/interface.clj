(ns clj.intracel.kv-store.interface
  "The `clj.intracel.kv-store.interface` namespace defines polylith interface for the KV-Store. 
Generally, this is the namespace users of the KV-Store will put in their [[require]] statement 
to use the component."
  (:require [clj.intracel.api.interface.protocols :as proto]
            [clj.intracel.kv-store.lmdb :as lmdb])
  (:import [clj.intracel.api.interface.protocols KVStoreContext KVStoreDbContext KVSerde]
           [java.nio ByteBuffer]
           ))

(declare new-kv-store-context)

(defn create-kv-store-context
  "Starts up the KV-Store context which hosts the embedded database.
   | Parameter   | Description |
   | ------------|-------------|
   | `ctx-opts`  | Map containing options to adjust the behavior of the environment at start-up.|
   |             | | :Key:                                            | :Description: | 
   |             | | -------------------------------------------------| --------------|
   |             | | `:intracel.kv-store/type`                        | Used to determine which type of data store to create. Currently only option is `:lmdb`.| 
   |             | | `:intracel.kv-store.lmdb/keyspace-max-mem-size`  | The total size (in bytes) allowed.    | 
   |             | | `:intracel.kv-store.lmdb/num-dbs`                | The number of independent, concurrent DB objects to support.|
   |             | | `:intracel.kv-store.lmdb/storage-path`           | Local filesystem path where the data will be persisted to disk.||
   Returns:
   A `clj.intracel.api.kv-store/KVStoreContext`"
  [ctx-opts]
  (proto/map->KVStoreContext {:ctx (new-kv-store-context ctx-opts)}))

(defmulti new-kv-store-context (fn [ctx-opts] (:intracel.kv-store/type ctx-opts)))

(defmethod new-kv-store-context :lmdb [ctx-opts]
  (lmdb/create-kv-store-context ctx-opts))


(defn db
  "Returns a hosted embedded database. See [[clj.intracel.api.kv-store/KVStoreDb]].
  
  Depends on: [[create-kv-store-context]]"
  [^KVStoreContext kvs-ctx db-name db-opts]
  (proto/db kvs-ctx db-name db-opts))

(defn set-key-serde
  "Sets the default key SerDe used for serializing and deserializing to and from the database. See [[clj.intracel.api.kv-store/KVStoreDb]].
  
  Depends on: [[db]]"
  [^KVStoreDbContext kvs-db ^KVSerde key-serde]
  (proto/set-key-serde kvs-db key-serde))

(defn set-val-serde
  "Sets the default value SerDe used for serializing and deserializing to and from the database. See [[clj.intracel.api.kv-store/KVStoreDb]].

Depends on: [[db]]"
  [^KVStoreDbContext kvs-db ^KVSerde val-serde]
  (proto/set-val-serde kvs-db val-serde))

(defn kv-put
  "Puts a value into the KV-Store. This is a multi-arity function with the 3-parameter version using the default SerDe (see [[set-key-serde]], [[set-val-serde]]) and the 5-parameter version allowing the caller to provide its own key and value SerDes. See [[clj.intracel.api.kv-store/KVStoreDb]].

Depends on: [[db]]."
  ([^KVStoreDbContext kvs-db key value]
  (proto/kv-put kvs-db key value))
  ([^KVStoreDbContext kvs-db value ^KVSerde key-serde ^KVSerde val-serde]
  (proto/kv-put kvs-db key value key-serde val-serde)))

(defn set-pre-get-hook
  "This enables the caller to customize the behavior performed when doing a key look-up in [[kv-get]] by allowing caller code to pre-process the key. See [[clj.intracel.api.kv-store/KVStoreDb]].

Depends on: [[db]]"
  [^KVStoreDbContext kvs-db pre-fn]
  (proto/set-pre-get-hook kvs-db pre-fn))

(defn kv-get
  "Gets a value from the KV-Store. If set, the `pre-get-hook` fn will be called on the key provided. This is a multi-arity function with the 2-parameter version using the default SerDe (see [[set-key-serde]], [[set-val-serde]]) and the 4-parameter version allowing the caller to provide its own key and value SerDes. See [[clj.intracel.api.kv-store/KVStoreDb]].

Depends on: [[db]]"
  ([^KVStoreDbContext kvs-db key]
  (proto/kv-get kvs-db key))
  ([^KVStoreDbContext kvs-db key ^KVSerde key-serde ^KVSerde val-serde]
  (proto/kv-get kvs-db key key-serde val-serde)))

(defn kv-del
  "Removes a key and its corresponding value from the KV-Store. This is a multi-arity function with the 2-parameter version using the default SerDe (see [[set-key-serde]]), and the 3-parameter version allowing the caller to provide its own key SerDe. See [[clj.intracel.api.kv-store/KVStoreDb]].

Depends on: [[db]]"
  ([^KVStoreDbContext kvs-db key]
  (proto/kv-del kvs-db key))
  ([^KVStoreDbContext kvs-db key ^KVSerde key-serde]
  (proto/kv-del kvs-db key key-serde)))