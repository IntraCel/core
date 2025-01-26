(ns clj.intracel.sql-store.duckdb 
  (:require [clojure.java.io :as io]
            [clojure.string :as st]))

(def log-prefix "lmdb")

(defn create-sql-store-context [{:keys [intracel.sql-store.duckdb/storage-path]}]
  (let [path (if-not (st/blank? storage-path)
               (io/file storage-path)
               (throw (ex-info (format "[%s/create-sql-store-context] Unable to produce a new SQLStoreContext. Are you missing a :intracel.sql-store.duckdb/storage-path in ctx-opts?" log-prefix) {:cause :missing-storage-path})))]))