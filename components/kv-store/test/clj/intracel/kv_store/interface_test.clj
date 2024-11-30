(ns clj.intracel.kv-store.interface-test
  (:require [clojure.test :as test :refer :all]
            [clj.intracel.kv-store.interface :as kv-store]))

(deftest dummy-test
  (is (= 1 1)))

(deftest test-kv-store-context-lmdb
  (let [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                   :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    (try (is (not (nil? kvs-ctx)))
         (.close (:ctx kvs-ctx)))))

(deftest test-context-can-create-db-instance
  (let [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                   :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    (try (is (not (nil? kvs-ctx)))
         (let [db (kv-store/db kvs-ctx "rogue-one" [:ic-db-flags/create-db-if-not-exists])])
         (finally (.close (:ctx kvs-ctx)))))