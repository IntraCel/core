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
           (let [dbi (kv-store/db kvs-db-ctx "teal'c" nil [:ic-db-flags/create-db-if-not-exists])]
             (is (not (nil? dbi)))))
         (catch Exception e
           (prn "Error in Test: " (.getMessage e))
           (doseq [tr (.getStackTrace e)]
             (prn "Trace: " tr))))))

(deftest test-kv-put-and-kv-get-with-default-serdes
  (with-open [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    (is (not (nil? kvs-ctx)))
    (try (let [kvs-db-ctx (kv-store/create-kv-store-db-context kvs-ctx :lmdb)]
           (is (not (nil? kvs-db-ctx)))
           (let [dbi (kv-store/db kvs-db-ctx "sg-1" {:ic-chan-opts/buf-size 100} [:ic-db-flags/create-db-if-not-exists])]
             (is (not (nil? dbi)))
             (kv-store/kv-put dbi "general" "Jack O'Neil")
             (kv-store/kv-put dbi "doctor" "Daniel Jackson")
             (kv-store/kv-put dbi "major" "Samantha Carter")
             (kv-store/kv-put dbi "jafa" "Teal'c")
             (let [general (kv-store/kv-get dbi "general")]
               (is (= "Jack O'Neil" general)))
             ))
         (catch Exception e
           (prn "Error in test-kv-put: " (.getMessage e))
           (doseq [tr (.getStackTrace e)]
             (prn "Trace: " tr))))))