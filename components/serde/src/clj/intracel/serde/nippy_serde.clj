(ns clj.intracel.serde.nippy-serde
  (:require [clj.intracel.api.interface.protocols :as proto]
            [taoensso.nippy :as nippy])
  (:import [java.nio ByteBuffer]))

(defrecord NippySerde []
  proto/KVSerde
  (serialize [this data]
    (if-not (some? data)
      (throw (ex-info "[nippy-serde] The data argument provided was nil."
                      {:cause #{:invalid-argument}}))
      (let [frozen-data (nippy/freeze data)
            buf         ^ByteBuffer (ByteBuffer/allocateDirect (count frozen-data))]
        (-> buf
            (.put frozen-data)
            (.flip)))))

  (deserialize [this data]
    (if (and (some? data) (instance? ByteBuffer data))
      ;;data is a ByteBuffer
      (let [bytes (byte-array (.capacity data))
            ;;DirectByteBuffers don't always have a backing array to call (.array) on.
            ;;Safest bet is to set the buffer to the beginning and call (.get <array>)
            ;;Where it will fill the array passed in with the values from the buffer.
            _     (.rewind data)
            _     (.get data bytes)]
        (nippy/thaw bytes))
      (throw (ex-info "[nippy-serde] The data argument provided is not a valid ByteBuffer."
                      {:cause #{:invalid-argument}})))))

(defn create []
  (map->NippySerde {}))