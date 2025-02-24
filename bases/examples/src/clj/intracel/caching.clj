(ns clj.intracel.caching
  (:require [clj.intracel.kv-store.interface :as kv-store]))

(defn expensive-operation [key] 
  (Thread/sleep 1000)
  (str "Expensive Operation for key: " key))

(defn check-cache-or-run-expensive-op [dbi key]
  (let [value (kv-store/kv-get dbi key)]
    (if (nil? value)
      (let [expensive-value (expensive-operation key)]
        (kv-store/kv-put dbi key expensive-value)
        expensive-value)
      value)))

(defn cache-example []
  (with-open [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb-cache/")})]
    (let [kvs-db-ctx (kv-store/create-kv-store-db-context kvs-ctx :lmdb)
          dbi        (kv-store/db kvs-db-ctx "sci-fi" nil [:ic-db-flags/create-db-if-not-exists])]
      (kv-store/kv-put dbi "stargate" "SG-1")
      (kv-store/kv-put dbi "star-trek" "Enterprise")
      (kv-store/kv-put dbi "star-wars" "Millennium Falcon")
      (kv-store/kv-put dbi "dr-who" "Tardis")
      (prn "Checking for cached value: stargate" (time (check-cache-or-run-expensive-op dbi "stargate"))) 
      (prn "Checking for non-cached value: babylon-5" (time (check-cache-or-run-expensive-op dbi "babylon-5")))
      (prn "Checking for cached-value: star-trek" (time (check-cache-or-run-expensive-op dbi "star-trek")))
      (prn "checking for cached-value: babylong-5" (time (check-cache-or-run-expensive-op dbi "babylon-5")))
      )))

(comment 
  ;;This is a simple example of caching using a key-value store
  ;;The easiest way to run this is to use the REPL
  ;;If you've attached to the REPL, you can run the function below to see how to use kv-store as a cache:
  (cache-example) 
  )