(ns clj.intracel.serde.long-serde
  (:require [clj.intracel.api.interface.protocols :as proto]
            [clj.intracel.serde.interface :as serde])
  (:import [java.nio ByteBuffer]))

(defonce NUM_BITS_IN_LONG 64)
(defonce NUM_BITS_IN_BYTE 8)
(defonce NUM_BYTES_IN_LONG (/ NUM_BITS_IN_LONG NUM_BITS_IN_BYTE))

(defrecord LongSerde []
  proto/KVSerde
  (serialize [this data]
    (cond (> data Long/MAX_VALUE)
          (throw (ex-info (format "[long-serde/serialize] Invalid argument. The data argument is larger than 64-bit integer allows. Value provided: %s, Max Allowed: %s" data Long/MAX_VALUE)
                          {:cause #{:invalid-argument}}))
          (< data Long/MIN_VALUE)
          (throw (ex-info (format "[long-serde/serialize] Invalid argument. The data argument is beyond the minimum value that a 64-bit integer allows. Value provided: %s, Allowed: %s" data Long/MIN_VALUE)
                          {:cause #{:invalid-argument}}))
          ;; In Java NIO, the ByteBuffer.flip() method is used to transition a buffer from its 
          ;; "writing mode" to its "reading mode" by essentially setting the buffer's limit to 
          ;; its current position and then resetting the position back to zero, effectively 
          ;; preparing the buffer to be read from the beginning of the data it has accumulated 
          ;; so far.
          ;; See https://docs.oracle.com/javase/6/docs/api/java/nio/Buffer.html#flip()
          :else (-> ^ByteBuffer (ByteBuffer/allocateDirect NUM_BYTES_IN_LONG)
                    (.putLong data)
                    (.flip))))

  (deserialize [this data]
    ;;data is a ByteBuffer
    (.getLong data))

  (serde-type [this]
    :long))

(defn create []
  (map->LongSerde {}))