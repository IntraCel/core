(ns clj.intracel.serde.int-serde
  (:require [clj.intracel.api.interface.protocols :as proto])
  (:import [java.nio ByteBuffer]))

(defonce NUM_BITS_IN_INTEGER 32)
(defonce NUM_BITS_IN_BYTE 8)
(defonce NUM_BYTES_IN_INTEGER (/ NUM_BITS_IN_INTEGER NUM_BITS_IN_BYTE))

(defrecord IntSerde []
  proto/KVSerde
  (serialize [this data]
    (let [buf ^ByteBuffer (ByteBuffer/allocate NUM_BYTES_IN_INTEGER)]
      (cond (> data Integer/MAX_VALUE)
            (throw (ex-info (format "[int-serde/serialize] Invalid argument. The data argument is larger than a 32-bit integer allows. Value provided: %s, Max Allowed: %s" data Integer/MAX_VALUE)
                            {:cause #{:invalid-argument}}))
            (< data Integer/MIN_VALUE)
            (throw (ex-info (format "[int-serde/serialize] Invalid argument. The data argument is beyond the minimum value of a 32-bit integer allows. Value provided: %s, Allowed: %s" data Integer/MIN_VALUE)
                            {:cause #{:invalid-argument}}))
            ;; In Java NIO, the ByteBuffer.flip() method is used to transition a buffer from its 
            ;; "writing mode" to its "reading mode" by essentially setting the buffer's limit to 
            ;; its current position and then resetting the position back to zero, effectively 
            ;; preparing the buffer to be read from the beginning of the data it has accumulated 
            ;; so far.
            ;; See https://docs.oracle.com/javase/6/docs/api/java/nio/Buffer.html#flip()
            :else (-> buf
                      (.putInt data)
                      (.flip)))))

  (deserialize [this data]
    ;;data is a ByteBuffer
    (.getInt data))

  (serde-type [this]
    :int))

(defn create []
  (map->IntSerde {}))