(ns clj.intracel.serde.interface
  (:require [clj.intracel.serde.big-int-serde :as big-int-serde]
            [clj.intracel.serde.int-serde :as int-serde]
            [clj.intracel.serde.long-serde :as long-serde]
            [clj.intracel.serde.nippy-serde :as nippy-serde]
            [clj.intracel.serde.string-serde :as string-serde]
            [clj.intracel.serde.uint-128-serde :as u128-int-serde]))

(defn big-int-serde []
  (big-int-serde/create))

(defn int-serde []
  (int-serde/create))

(defn long-serde []
  (long-serde/create))

(defn nippy-serde []
  (nippy-serde/create))

(defn string-serde [byte-capacity]
  (string-serde/create byte-capacity))

(defn u128-int-serde []
  (u128-int-serde/create))




