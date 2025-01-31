(ns clj.intracel.sql-store.interface-test
  (:require [clojure.test :as test :refer :all]
            [clj.intracel.sql-store.interface :as sql-store]
            [next.jdbc :as jdbc])
  (:import [java.sql DriverManager]))

(deftest dummy-test
  (is (= 1 1)))

(deftest test-sql-store-context-duckdb
  (testing "when connection pool is created with DuckDB that it's set up correctly."
    (with-open [sql-ctx (sql-store/create-sql-store-context {:intracel.sql-store/type :duckdb
                                                             :intracel.sql-store.duckdb/storage-path (str (System/getProperty "java.io.tmpdir") "/duckdb")})]
      (is (not (nil? sql-ctx)))
      (is (contains? sql-ctx :ctx))
      (let [ctx (:ctx sql-ctx)]
        (is (contains? ctx :pool))
        (is (contains? ctx :appender-conn))))))

(deftest connection-test
  (let [c (DriverManager/getConnection "jdbc:duckdb:/tmp/test-duckdb")]
    (is (not (nil? c)))))

(deftest test-context-can-create-db-instance
  (with-open [sql-ctx (sql-store/create-sql-store-context {:intracel.sql-store/type :duckdb
                                                           :intracel.sql-store.duckdb/storage-path (str (System/getProperty "java.io.tmpdir") "/duckdb")})]
    (is (not (nil? sql-ctx)))
    (try (let [sql-store-db-ctx (sql-store/create-sql-store-db-context sql-ctx :duckdb)]
           (is (not (nil? sql-store-db-ctx)))
           (let [db (sql-store/db sql-store-db-ctx)]
             (is (not (nil? db)))))
         (catch Exception e 
           (prn "Error in Test: " (.getMessage e))
           (doseq [tr (.getStackTrace e)]
             (prn "Trace: " tr))))))

(deftest test-bulk-loading 
  (with-open [sql-ctx (sql-store/create-sql-store-context {:intracel.sql-store/type :duckdb
                                                           :intracel.sql-store.duckdb/storage-path (str (System/getProperty "java.io.tmpdir") "/duckdb")})]
    (let [sql-store-db-ctx (sql-store/create-sql-store-db-context sql-ctx :duckdb)
          db               (sql-store/db sql-store-db-ctx)]
      (prn "db:" db)
      (jdbc/execute! (get-in db [:sql-ctx :ctx :pool])
                     ["CREATE OR REPLACE TABLE main.movies (title VARCHAR, year INT, rotten_tomatoes_score FLOAT)"])
      (let [results (sql-store/bulk-load db "movies" [["Star Wars Episode V: The Empire Strikes Back" 1977 93.0]
                                                      ["Ghostbusters" 1984 95.0]
                                                      ["Inception" 2010 87.0]])]
        (is (true? (:loaded? results)))
        ))))
