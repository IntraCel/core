(ns clj.intracel.examples.sql
  (:require [clj.intracel.sql-store.interface :as sql-store]
            [next.jdbc :as jdbc]))

(defn sql-example []
  (with-open [sql-ctx (sql-store/create-sql-store-context {:intracel.sql-store/type :duckdb
                                                           :intracel.sql-store.duckdb/storage-path (str (System/getProperty "java.io.tmpdir") "/duckdb-" (java.util.UUID/randomUUID))})]
    (let [sql-store-db-ctx (sql-store/create-sql-store-db-context sql-ctx :duckdb)
          db               (sql-store/db sql-store-db-ctx) 
          appender-conn    (get-in db [:sql-ctx :ctx :appender-conn])]
      (jdbc/execute! appender-conn ["CREATE SCHEMA IF NOT EXISTS cinema"])
      (jdbc/execute! appender-conn ["USE cinema"])
      (jdbc/execute! appender-conn ["CREATE TABLE IF NOT EXISTS movie_villains (id INT PRIMARY KEY, name TEXT, movie TEXT, threat_level FLOAT)"])
      (let [results (sql-store/bulk-load db "cinema.movie_villains" [[1 "Darth Vader" "Rogue One: A Star Wars Story" 100.0]
                                                                     [2 "Zhoul"       "Ghostbusters"                 85.5]
                                                                     [3 "Stay Puft"   "Ghostbusters"                 90.0]
                                                                     [4 "Sauron"      "Lord of the Rings"            95.0]
                                                                     [5 "Agent Smith" "The Matrix"                   80.0]])]
        (prn "Bulk Load Results: " results)
        (when (:loaded? results)
          (let [ghostbuster-villains (jdbc/execute! appender-conn ["SELECT * FROM cinema.movie_villains WHERE movie = 'Ghostbusters'"])]
            (prn "Ghostbuster Villains: " ghostbuster-villains))))))) 
      
(comment 
  (sql-example)
  )