(ns clj.intracel.serde.big-int-serde
  (:require [clj.intracel.api.interface.protocols :as proto]
            [clj.intracel.serde.big-int-helper :as helper])
  (:import [java.math BigInteger]
           [java.nio ByteBuffer]))

(defrecord BigIntSerde []
  proto/KVSerde
  (serialize [this data]
    (helper/ser-string-as-big-int-to-byte-buf data))

  (deserialize [this data]
    (helper/deser-big-int-from-byte-buf data)))


(defn create []
  (map->BigIntSerde {}))