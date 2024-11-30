(ns clj.intracel.kv-store.interface-test
  (:require [clojure.test :as test :refer :all]
            [clj.intracel.kv-store.interface :as kv-store]))

(deftest dummy-test
  (is (= 1 1)))

(deftest test-kv-store-context-lmdb
  (with-open [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    (is (not (nil? kvs-ctx)))))

(deftest test-context-can-create-db-instance
  (with-open [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    (is (not (nil? kvs-ctx)))
    (try (let [kvs-db-ctx (kv-store/create-kv-store-db-context kvs-ctx :lmdb)]
           (is (not (nil? kvs-db-ctx)))
           (let [dbi (kv-store/db kvs-db-ctx "teal'c" [:ic-db-flags/create-db-if-not-exists])]
             (is (not (nil? dbi)))))
         (catch Exception e 
           (prn "Error in Test: " (.getMessage e))
           (doseq [tr (.getStackTrace e)]
             (prn "Trace: " tr))))))