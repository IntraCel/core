(ns clj.intracel.examples.sql
  (:require [clj.intracel.sql-store.interface :as sql-store]
            [next.jdbc :as jdbc]))

(defn sql-example []
  (with-open [sql-ctx (sql-store/create-sql-store-context {:intracel.sql-store/type :duckdb
                                                           :intracel.sql-store.duckdb/storage-path (str (System/getProperty "java.io.tmpdir") "/duckdb-" (java.util.UUID/randomUUID))})]
    (let [sql-store-db-ctx (sql-store/create-sql-store-db-context sql-ctx :duckdb)
          db               (sql-store/db sql-store-db-ctx)
          pool-ds          (get-in db [:sql-ctx :ctx :pool])
          appender-conn    (get-in db [:sql-ctx :ctx :appender-conn])]
      (jdbc/execute! appender-conn ["CREATE SCHEMA IF NOT EXISTS movies"])
      (jdbc/execute! appender-conn ["USE movies"])
            
      )))