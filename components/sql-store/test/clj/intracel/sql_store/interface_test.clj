(ns clj.intracel.sql-store.interface-test
  (:require [clojure.test :as test :refer :all]
            [clj.intracel.sql-store.interface :as sql-store])
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
