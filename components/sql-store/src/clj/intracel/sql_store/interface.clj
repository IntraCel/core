(ns clj.intracel.sql-store.interface
  "The `clj.intracel.sql-store.interface` namespace defines a polylith interface for the SQL-Store.
   Generally, this is te namespace users of the SQL-Store will put in their [[require]] statement
   to use the component."
  (:require [clj.intracel.api.interface.protocols :as proto]
            [clj.intracel.sql-store.duckdb :as duckdb]
            [taoensso.timbre :as log]))

(declare new-sql-store-context)

(defn create-sql-store-context
  "Starts up the SQL-Store context which helps to bootstrap the embedded database.
  | Parameter   | Description |
  | ------------|-------------|
  | `ctx-opts`  | Map containing options to adjust the behavior of the database at start-up.|
  |             | | :Key:                                            | :Description: | 
  |             | | -------------------------------------------------| --------------|
  |             | | `:intracel.sql-store/type`                       | Used to determine which type of data store to create. Currently only option is `:duckdb`.| 
  |             | | `:intracel.sql-store.duckdb/storage-path`        | Local filesystem path where the data will be persisted to disk.||
    
    Returns:
    A [[clj.intracel.api.interface.protocols/SQLtoreContext]] with the :ctx field set to a map containing a connection pool and native DuckDB connection."
  [ctx-opts]
  (proto/map->SQLStoreContext (new-sql-store-context ctx-opts)))

(defmulti new-sql-store-context
  "Polymorphic constructor for producing a specific implementation that will be assigned ot the `:ctx` field in the [[clj.api.interface.protocols/SQLStoreContext]].
  This function requires the ctx-opts map to contain the `:intracel.sql-store/type` field and will determine the correct implementation to generate based on the value provided.
  | Parameter   | Description |
  | ------------|-------------|
  | `ctx-opts`  | Map containing options to adjust the behavior of the database at start-up.|
  |             | | :Key:                                            | :Description: | 
  |             | | -------------------------------------------------| --------------|
  |             | | `:intracel.sql-store/type`                       | Used to determine which type of data store to create. Currently only option is `:lmdb`.| 
  |             | | `:intracel.sql-store.duckdb/storage-path`        | Local filesystem path where the data will be persisted to disk.||
  Returns:
  A data structure to be assigned to the `:ctx` field in the [[clj.intracel.api.interface.protocols/KVStoreContext]]"
  (fn [ctx-opts]
    (if (contains? ctx-opts :intracel.sql-store/type)
      (:intracel.sql-store/type ctx-opts)
      (throw (ex-info "[new-sql-context] Unable to produce a new SQLStoreContext. Are you missing a :intracel.sql-store/type in ctx-opts?" {:cause :illegal-argument})))))

(defmethod new-sql-store-context :duckdb [ctx-opts]
  ;;returns a map with :ctx set
  (duckdb/create-sql-store-context ctx-opts))

(defmulti new-sql-store-db-context (fn [sql-ctx sql-store-type]
                                     sql-store-type))

(defmethod new-sql-store-db-context :duckdb [sql-ctx sql-store-type]
  (log/debugf "[new-sql-store-db-context] Creating DB specific context for: %s" sql-store-type)
  (duckdb/create-sql-store-db-context sql-ctx))

(defn create-sql-store-db-context [sql-ctx sql-store-type]
  (new-sql-store-db-context sql-ctx sql-store-type))

(defn db
  "Constructor function for an embedded SQL-Store. See [[clj.intracel.api.interface.protocols/SQLStoreApi]]
   
  Depends on: [[cl.intracel.api.interface.protocols/SQLStoreDbContextApi]].
  | Parameter    | Description |
  | -------------|-------------|
  | `sql-db-ctx` | A reference to the [[clj.intracel.api.interface.protocols/SQLStoreDbContextApi]].||
  Returns:
  A record that implements the [[clj.intracel.api.interface.protocols/SQLStoreApi]] with a field called `sql-ctx` that references the [[clj.intracel.api.interface.protocols/SQLStoreContext]] provided in the `sql-db-ctx` parameter."
  [sql-db-ctx]
  (proto/create-sql-db sql-db-ctx))
