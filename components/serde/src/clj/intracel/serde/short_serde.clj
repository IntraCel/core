(ns clj.intracel.serde.short-serde
  (:require [clj.intracel.api.interface.protocols :as proto])
  (:import [java.nio ByteBuffer]))

(defonce NUM_BITS_IN_BYTE 8)
(defonce NUM_BITS_IN_SHORT 16)
(defonce NUM_BYTES_IN_SHORT (/ NUM_BITS_IN_SHORT NUM_BITS_IN_BYTE))

(defrecord ShortSerde []
  proto/KVSerde
  (serialize [this data]
    (let [buf ^ByteBuffer (ByteBuffer/allocateDirect NUM_BYTES_IN_SHORT)]
      (cond (> data Short/MAX_VALUE)
            (throw (ex-info (format "[short-serde/serialize] Invalid argument. The data argument is larger than a 16-bit integer allows. Value provided: %s, Max Allowed: %s" data Short/MAX_VALUE)
                            {:cause #{:invalid-argument}}))
            (< data Short/MIN_VALUE)
            (throw (ex-info (format "[short-serde/serialize] Invalid argument. The data argument is beyond the minimum value of a 16-bit integer allows. Value provided: %s, Allowed: %s" data Short/MIN_VALUE)
                            {:cause #{:invalid-argument}}))
      ;; In Java NIO, the ByteBuffer.flip() method is used to transition a buffer from its 
      ;; "writing mode" to its "reading mode" by essentially setting the buffer's limit to 
      ;; its current position and then resetting the position back to zero, effectively 
      ;; preparing the buffer to be read from the beginning of the data it has accumulated 
      ;; so far.
      ;; See https://docs.oracle.com/javase/6/docs/api/java/nio/Buffer.html#flip()
            :else (-> buf
                      (.putShort data)
                      (.flip)))))

  (deserialize [this data]
    ;;data is a ByteBuffer
    (.getShort data))
  
  (serde-type [this]
    :short))

(defn create []
  (map->ShortSerde {}))