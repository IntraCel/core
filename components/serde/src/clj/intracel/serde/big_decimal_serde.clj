(ns clj.intracel.serde.big-decimal-serde
  (:require [clj.intracel.api.interface.protocols :as proto])
  (:import [java.math BigDecimal]
           [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]))

(def UTF_8 StandardCharsets/UTF_8)

(defrecord BigDecimalSerde []
  proto/KVSerde
  (serialize [this data]
    (let [dec-as-str (.toString data)
          buf ^ByteBuffer (ByteBuffer/allocateDirect (count dec-as-str))]
      ;; In Java NIO, the ByteBuffer.flip() method is used to transition a buffer from its 
      ;; "writing mode" to its "reading mode" by essentially setting the buffer's limit to 
      ;; its current position and then resetting the position back to zero, effectively 
      ;; preparing the buffer to be read from the beginning of the data it has accumulated 
      ;; so far.
      ;; See https://docs.oracle.com/javase/6/docs/api/java/nio/Buffer.html#flip()
      (-> buf
          (.put (.getBytes dec-as-str UTF_8))
          (.flip))))

  (deserialize [this data]
    ;;data is a ByteBuffer of the BigDecimal encoded as a string
    (let [dec-str (str (.decode UTF_8 data))]
      (BigDecimal. dec-str)))
  
  (serde-type [this]
    :big-decimal))

(defn create []
  (map->BigDecimalSerde {}))