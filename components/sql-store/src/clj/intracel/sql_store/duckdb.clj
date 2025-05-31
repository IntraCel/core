(ns clj.intracel.sql-store.duckdb
  (:require [clj.intracel.api.interface.protocols :as proto]
            [clojure.java.io :as io]
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
        store-name   "intracel-duckdb"
        pool-path    (str storage-path
                          (if (st/ends-with? storage-path "/") "" "/")
                          store-name)
        
        db-url       (connection/jdbc-url {:dbtype "duckdb" :dbname pool-path})
        _            (log/infof "[new-sql-store-context] jdbc-url: %s" db-url)
        _            (log/infof "[new-sql-store-context] pool-path: %s" pool-path)
        pool         ^HikariDataSource (connection/->pool com.zaxxer.hikari.HikariDataSource
                                                          {:jdbcUrl db-url
                                                           ;;ignored on duckdb but needed for connection pool
                                                           ;;:username "dbuser"
                                                           ;;ignored on duckdb but needed for connection pool
                                                           ;;:password "dbpassword"
                                                           })
        props         (Properties.)
        ;;_             (.setProperty props "username" "dbuser")
        ;;_             (.setProperty props "password" "dbpassword")
        ;;_             (.setProperty props DuckDBDriver/JDBC_STREAM_RESULTS "true")
        

        _ (log/infof "[new-sql-store-context] storage-path: %s" storage-path)
        appender-conn ^DuckDBConnection (DriverManager/getConnection db-url props)]
    {:ctx {:pool          pool
           :appender-conn appender-conn}}))

(defrecord DuckDbRec [sql-ctx]
  proto/SQLStoreApi
  (bulk-load [this table-name rows]
    (let [conn (get-in sql-ctx [:ctx :appender-conn])
          [schema table] (if (.contains table-name ".")
                           (st/split table-name #"\.")
                           [DuckDBConnection/DEFAULT_SCHEMA table-name])]
      (log/infof "[bulk-load] schema: %s, table: %s" schema table)
      (try (with-open [appender (.createAppender conn schema table)]
             (doseq [row rows]
               (.beginRow appender)
               (doseq [col row]
                 (.append appender col))
               (.endRow appender))
             {:loaded? true})

           (catch Exception ex
             {:loaded? false :msg (.getMessage ex)})))))



(defrecord DuckDBContext [sql-ctx]
  proto/SQLStoreDbContextApi
  (create-sql-db [this]
    (map->DuckDbRec {:sql-ctx sql-ctx})))

(defn create-sql-store-db-context [sql-ctx]
  (map->DuckDBContext {:sql-ctx sql-ctx}))