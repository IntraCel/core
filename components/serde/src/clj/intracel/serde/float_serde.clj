(ns clj.intracel.serde.float-serde
  (:require [clj.intracel.api.interface.protocols :as proto])
  (:import [java.nio ByteBuffer]))

(defonce NUM_BITS_IN_FLOAT 32)
(defonce NUM_BITS_IN_BYTE 8)
(defonce NUM_BYTES_IN_FLOAT (/ NUM_BITS_IN_FLOAT NUM_BITS_IN_BYTE))

(defrecord FloatSerde []
  proto/KVSerde
  (serialize [this data]
    (cond (> data Float/MAX_VALUE)
          (throw (ex-info (format "[float-serde/serialize] Invalid argument. The data argument is larger than 32-bit float allows. Value provided: %s, Max Allowed: %s" data Float/MAX_VALUE)
                          {:cause #{:invalid-argument}}))
          (and (< data 0)
               (> data Float/MIN_VALUE))
          (throw (ex-info (format "[float-serde/serialize] Invalid argument. The data argument is beyond the minimum value that a 32-bit float allows. Value provided: %s, Allowed: %s" data Float/MIN_VALUE)
                          {:cause #{:invalid-argument}}))
          ;; In Java NIO, the ByteBuffer.flip() method is used to transition a buffer from its 
          ;; "writing mode" to its "reading mode" by essentially setting the buffer's limit to 
          ;; its current position and then resetting the position back to zero, effectively 
          ;; preparing the buffer to be read from the beginning of the data it has accumulated 
          ;; so far.
          ;; See https://docs.oracle.com/javase/6/docs/api/java/nio/Buffer.html#flip()
          :else (-> ^ByteBuffer (ByteBuffer/allocateDirect NUM_BYTES_IN_FLOAT)
                    (.putFloat data)
                    (.flip))))

  (deserialize [this data]
    ;;data is a ByteBuffer
    (.getFloat data))

  (serde-type [this]
    :float))

(defn create []
  (map->FloatSerde {}))