(ns clj.intracel.serde.double-serde
  (:require [clj.intracel.api.interface.protocols :as proto])
  (:import [java.nio ByteBuffer]))

(defonce NUM_BITS_IN_DOUBLE 64)
(defonce NUM_BITS_IN_BYTE 8)
(defonce NUM_BYTES_IN_DOUBLE (/ NUM_BITS_IN_DOUBLE NUM_BITS_IN_BYTE))

(defrecord DoubleSerde []
  proto/KVSerde
  (serialize [this data]
    (cond (> data Double/MAX_VALUE)
          (throw (ex-info (format "[double-serde/serialize] Invalid argument. The data argument is larger than 64-bit double allows. Value provided: %s, Max Allowed: %s" data Double/MAX_VALUE)
                          {:cause #{:invalid-argument}}))
          (and (< data 0)
               (> data Double/MIN_VALUE))
          (throw (ex-info (format "[double-serde/serialize] Invalid argument. The data argument is beyond the minimum value that a 64-bit double allows. Value provided: %s, Allowed: %s" data Double/MIN_VALUE)
                          {:cause #{:invalid-argument}}))
          ;; In Java NIO, the ByteBuffer.flip() method is used to transition a buffer from its 
          ;; "writing mode" to its "reading mode" by essentially setting the buffer's limit to 
          ;; its current position and then resetting the position back to zero, effectively 
          ;; preparing the buffer to be read from the beginning of the data it has accumulated 
          ;; so far.
          ;; See https://docs.oracle.com/javase/6/docs/api/java/nio/Buffer.html#flip()
          :else (-> ^ByteBuffer (ByteBuffer/allocateDirect NUM_BYTES_IN_DOUBLE)
                    (.putDouble data)
                    (.flip))))

  (deserialize [this data]
  ;;data is a ByteBuffer
    (.getDouble data)))

(defn create []
  (map->DoubleSerde {}))