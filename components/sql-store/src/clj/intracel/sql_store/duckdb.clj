(ns clj.intracel.sql-store.duckdb
  (:require [clojure.java.io :as io]
            [clojure.string :as st]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [taoensso.timbre :as log])
  (:import [com.zaxxer.hikari HikariDataSource]
           [java.sql DriverManager]
           [java.util Properties]
           [org.duckdb DuckDBDriver DuckDBConnection]))

(def log-prefix "lmdb")

(defn create-sql-store-context [{:keys [intracel.sql-store.duckdb/storage-path]}]
  (let [path (if-not (st/blank? storage-path)
               (io/file storage-path)
               (throw (ex-info (format "[%s/create-sql-store-context] Unable to produce a new SQLStoreContext. Are you missing a :intracel.sql-store.duckdb/storage-path in ctx-opts?" log-prefix) {:cause :missing-storage-path})))
        _    (when-not (.exists path)
               (.mkdirs path))
        pool-path    (str storage-path
                          (if (st/ends-with? storage-path "/") "" "/")
                          "conn-pool")
        _            (log/infof "[new-sql-store-context] pool-path: %s" pool-path)
        pool         ^HikariDataSource (connection/->pool com.zaxxer.hikari.HikariDataSource
                                                          {:jdbcUrl (connection/jdbc-url {:dbtype "duckdb" :dbname pool-path})
                                                         ;;ignored on duckdb but needed for connection pool
                                                           :username "dbuser"
                                                         ;;ignored on duckdb but needed for connection pool
                                                           :password "dbpassword"})
        props        (-> (Properties.)
                         (.setProperty DuckDBDriver/JDBC_STREAM_RESULTS "true"))


        append-path (str storage-path
                         (if (st/ends-with? storage-path "/") "" "/")
                         "conn-appender")
        _ (log/infof "[new-sql-store-context] storage-path: %s" storage-path)
        appender-conn ^DuckDBConnection (DriverManager/getConnection (str "jdbc:duckdb:" append-path) props)]
    {:ctx {:pool          pool
           :appender-conn appender-conn}}))