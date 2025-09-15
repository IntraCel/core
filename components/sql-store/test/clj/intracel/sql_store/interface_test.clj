(ns clj.intracel.sql-store.interface-test
  (:require [clojure.test :as test :refer :all]
            [clj.intracel.sql-store.interface :as sql-store]
            [next.jdbc :as jdbc])
  (:import [java.sql DriverManager]
           [java.util UUID]))

(deftest dummy-test
  (is (= 1 1)))

(deftest test-sql-store-context-duckdb
  (testing "when connection pool is created with DuckDB that it's set up correctly."
    (with-open [sql-ctx (sql-store/create-sql-store-context {:intracel.sql-store/type :duckdb
                                                             :intracel.sql-store.duckdb/storage-path (str (System/getProperty "java.io.tmpdir") "/" (UUID/randomUUID) "/duckdb")})]
      (is (not (nil? sql-ctx)))
      (is (contains? sql-ctx :ctx))
      (let [ctx (:ctx sql-ctx)]
        (is (contains? ctx :pool))
        (is (contains? ctx :appender-conn))))))

(deftest connection-test
  (let [c (DriverManager/getConnection "jdbc:duckdb:/tmp/test-duckdb")]
    (is (not (nil? c)))))

(deftest test-context-can-create-db-instance
  (try (with-open [sql-ctx (sql-store/create-sql-store-context {:intracel.sql-store/type :duckdb
                                                                :intracel.sql-store.duckdb/storage-path (str (System/getProperty "java.io.tmpdir") "/" (UUID/randomUUID) "/duckdb")})]
         (is (not (nil? sql-ctx)))
         (let [sql-store-db-ctx (sql-store/create-sql-store-db-context sql-ctx :duckdb)]
           (is (not (nil? sql-store-db-ctx)))
           (let [db (sql-store/db sql-store-db-ctx)]
             (is (not (nil? db))))))
       (catch Exception e
         (prn "Error in Test: " (.getMessage e))
         (doseq [tr (.getStackTrace e)]
           (prn "Trace: " tr)))))

(deftest test-bulk-loading
  (with-open [sql-ctx (sql-store/create-sql-store-context {:intracel.sql-store/type :duckdb
                                                           :intracel.sql-store.duckdb/storage-path (str (System/getProperty "java.io.tmpdir") "/" (UUID/randomUUID) "/duckdb-appender")})]
    (let [sql-store-db-ctx (sql-store/create-sql-store-db-context sql-ctx :duckdb)
          db               (sql-store/db sql-store-db-ctx)
          pool-ds          (get-in db [:sql-ctx :ctx :pool])
          appender-conn    (get-in db [:sql-ctx :ctx :appender-conn])]
      (prn "db:" db)
      (prn "schemas:")
      (try
        (let [schemas (jdbc/execute! appender-conn ["SELECT schema_name FROM information_schema.schemata"])]
          (prn schemas))
        (catch Exception ex
          (prn "failed to list schemas:" (.getMessage ex))))

      ;; The DDL statements are using the same connection as the bulk-load fn here.
      ;; I haven't gotten to the bottom of this yet but am pointing it out because
      ;; if you attempt to crate the schema and use it on the pool-ds, the test 
      ;; will fail with: Table "intracel.movies" could not be found.
      (try (jdbc/execute! pool-ds
                          ["CREATE SCHEMA IF NOT EXISTS intracel"])
           (catch Exception ex
             (prn "failed to create schema:" (.getMessage ex))))

      ;;(jdbc/execute! appender-conn ["USE intracel"])

      (prn "Tables in schema intracel:")
      (try (let [tables (jdbc/execute! pool-ds ["SELECT table_name FROM information_schema.tables where table_schema = 'intracel'"])]
             (prn tables))
           (catch Exception ex
             (prn "failed to list tables:" (.getMessage ex))))

      (try (jdbc/execute! pool-ds
                          ["CREATE OR REPLACE TABLE intracel.movies (title VARCHAR, year INT, rotten_tomatoes_score FLOAT)"])
           (prn "Tables in schema intracel:")
           (try (let [tables (jdbc/execute! pool-ds ["SELECT table_name FROM information_schema.tables where table_schema = 'intracel'"])]
                  (prn tables))
                (catch Exception ex
                  (prn "failed to list tables:" (.getMessage ex))))
           (catch Exception ex
             (prn "failed to create table:" (.getMessage ex))))
      (let [results (sql-store/bulk-load db "intracel.movies" [["Star Wars Episode V: The Empire Strikes Back" (int 1977) (float 93.0)]
                                                               ["Ghostbusters" (int 1984) (float 95.0)]
                                                               ["Inception" (int 2010) (float 87.0)]])]
        (prn "results:" results)
        (is (true? (:loaded? results)))))))
