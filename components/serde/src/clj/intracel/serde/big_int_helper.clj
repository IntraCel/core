(ns clj.intracel.serde.big-int-helper
  (:import [java.math BigInteger]
           [java.nio ByteBuffer]))

(defn ser-string-as-big-int-to-byte-buf [data]
  (if (string? data)
    (let [big-int     (BigInteger. data)
          big-int-arr (.toByteArray big-int)
          buf         ^ByteBuffer (ByteBuffer/allocateDirect (count big-int-arr))]
      (-> buf
          (.put big-int-arr)
          (.flip)))
    (throw (ex-info "[big-int-helper] Invalid data argument provided. Must be a valid string containing a number that can be converted by BigInteger."
                    {:cause #{:invalid-argument}}))))

(defn ser-big-int-to-byte-buf [data]
  (if (instance? java.math.BigInteger data)
    (let [big-int-arr (.toByteArray data)
          buf         ^ByteBuffer (ByteBuffer/allocateDirect (count big-int-arr))]
      (-> buf
          (.put big-int-arr)
          (.flip)))
    (throw (ex-info "[big-int-helper] Invalid data argument provided. Must be a valid string containing a number that can be converted by BigInteger."
                    {:cause #{:invalid-argument}}))))


(defn deser-big-int-from-byte-buf [data]
  (if (and (some? data) (instance? ByteBuffer data))
        ;;data is a ByteBuffer
    (let [bytes (byte-array (.capacity data))
              ;;DirectByteBuffers don't always have a backing array to call (.array) on.
              ;;Safest bet is to set the buffer to the beginning and call (.get <array>)
              ;;Where it will fill the array passed in with the values from the buffer.
          _     (.rewind data)
          _     (.get data bytes)]
      (BigInteger. bytes))
    (throw (ex-info "[big-int-buffer] Invalid ByteBuffer argument provided. Unable to deserialize to a BigInteger."
                    {:ex-info #{:invalid-argument}}))))
