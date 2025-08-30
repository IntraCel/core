(ns clj.intracel.serde.string-serde
  (:require [clj.intracel.api.interface.protocols :as proto]
            [clj.intracel.serde.interface :as serde])
  (:import [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]))

(def UTF_8 StandardCharsets/UTF_8)

(defrecord StringSerde []
  proto/KVSerde
  (serialize [this data]
    (let [bytes (.getBytes data)
          byte-capacity (count bytes)
          buf ^ByteBuffer (ByteBuffer/allocateDirect byte-capacity)]
      ;; In Java NIO, the ByteBuffer.flip() method is used to transition a buffer from its 
      ;; "writing mode" to its "reading mode" by essentially setting the buffer's limit to 
      ;; its current position and then resetting the position back to zero, effectively 
      ;; preparing the buffer to be read from the beginning of the data it has accumulated 
      ;; so far.
      ;; See https://docs.oracle.com/javase/6/docs/api/java/nio/Buffer.html#flip()
      (-> buf
          (.put (.getBytes data UTF_8))
          (.flip))))

  (deserialize [this data]
    (str (.decode UTF_8 data)))

  (serde-type [this]
    :string))

(defn create []
  (map->StringSerde {}))