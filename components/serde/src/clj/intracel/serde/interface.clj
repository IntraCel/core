(ns clj.intracel.serde.interface
  (:require [clj.intracel.serde.string-serde :as string-serde]))

(defn string-serde [byte-capacity]
  (string-serde/create byte-capacity))
