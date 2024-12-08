(ns clj.intracel.serde.byte-serde
  (:require [clj.intracel.api.interface.protocols :as proto])
  (:import [java.nio ByteBuffer]))

(defonce NUM_BITS_IN_BYTE 8)
(defonce NUM_BYTES_IN_BYTE (/ NUM_BITS_IN_BYTE NUM_BITS_IN_BYTE))

(defrecord ByteSerde []
  proto/KVSerde
  (serialize [this data]
    (let [buf ^ByteBuffer (ByteBuffer/allocateDirect NUM_BYTES_IN_BYTE)]
      (cond (> data Byte/MAX_VALUE)
            (throw (ex-info (format "[byte-serde/serialize] Invalid argument. The data argument is larger than an 8-bit integer allows. Value provided: %s, Max Allowed: %s" data Byte/MAX_VALUE)
                            {:cause #{:invalid-argument}}))
            (< data Byte/MIN_VALUE)
            (throw (ex-info (format "[byte-serde/serialize] Invalid argument. The data argument is beyond the minimum value of an 8-bit integer allows. Value provided: %s, Allowed: %s" data Byte/MIN_VALUE)
                            {:cause #{:invalid-argument}}))
      ;; In Java NIO, the ByteBuffer.flip() method is used to transition a buffer from its 
      ;; "writing mode" to its "reading mode" by essentially setting the buffer's limit to 
      ;; its current position and then resetting the position back to zero, effectively 
      ;; preparing the buffer to be read from the beginning of the data it has accumulated 
      ;; so far.
      ;; See https://docs.oracle.com/javase/6/docs/api/java/nio/Buffer.html#flip()
            :else (let [bytes (byte-array 1)
                        _     (aset-byte bytes 0 data)]
                    (-> buf
                      ;;Put a byte array with a single byte value into the ByteBuffer
                        (.put bytes)
                        (.flip))))))

  (deserialize [this data]
    ;;data is a ByteBuffer
    (.get data 0)))

(defn create []
  (map->ByteSerde {}))