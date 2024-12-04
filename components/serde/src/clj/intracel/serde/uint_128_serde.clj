(ns clj.intracel.serde.uint-128-serde
  (:require [clj.intracel.api.interface.protocols :as proto]
            [clj.intracel.serde.big-int-helper :as helper])
  (:import [java.math BigInteger]
           [java.nio ByteBuffer]))

(defonce MAX_UINT_128_VALUE (BigInteger. "340282366920938463463374607431768211455"))

(defrecord UInt128 [^BigInteger uint-128]
  ;; Object 
  ;; (equals [this other]
  ;;   (let [this-uint-128  (:uint-128 this)
  ;;         other-uint-128 (:uint-128 other)]
  ;;     (and (some? this-uint-128) 
  ;;          (some? other-uint-128) 
  ;;          (= (.compareTo this-uint-128 other-uint-128) 0))))
  )

(defn uint-128-from-big-int
  "Creates a valid 128-bit unsigned integer from an instance of a BigInteger. 
   This checks the validity of the value provided to see if it's positive and 
   within an unsigned 128-bit integer range.
   | Parameter     | Description |
   | --------------|-------------|
   | `big-int-val` | Must be an instance of a 0 < BigInteger < `MAX_UINT_128_VALUE` |
  
   Returns:
   A [[clj.intracel.serde.uint-128-serde/UInt128]] with the `:uint-128` field set to the value provided."
  [big-int-val]
  (if (or (nil? big-int-val)
          (not (instance? BigInteger big-int-val)))
    (throw (ex-info "[uint-128-serde] Invalid data argument provided. Must be a valid instance of BigInteger."
                    {:cause #{:invalid-argument}}))
    (if (or (< (.compareTo big-int-val BigInteger/ZERO) 0)
            (> (.compareTo big-int-val MAX_UINT_128_VALUE) 0))
      (throw (ex-info (format "[uint-128-serde] Invalid numerical argument provided. Must be a BigInteger > 0 and less than %s" (.toString MAX_UINT_128_VALUE))
                      {:cause #{:invalid-uint-range-argument}}))
      (map->UInt128 {:uint-128 big-int-val}))))

(defn uint-128-from-long
  "Creates a valid 128-bit unsigned integer using a provided `long` argument. 
   This is a convenience function that calls [[uint-128-from-big-int]].
   | Parameter     | Description |
   | --------------|-------------|
   | `long-val`    | Must be a positive `long` value that will be converted into a `BigInteger`|
   
   Returns: 
   A [[clj.intracel.serde.uint-128-serde/UInt128]] with the `:uint-128` field set to the value provided."
  [long-val]
  (uint-128-from-big-int (BigInteger/valueOf long-val)))

(defn uint-128-from-str
  "Creates a valid 128-bit unsigned integer using a provided `String` argument. 
     This is a convenience function that calls [[uint-128-from-big-int]].
     | Parameter     | Description |
     | --------------|-------------|
     | `str-val`     | Must be a numeric value within a String that will be converted into a `BigInteger`|
     
     Returns: 
     A [[clj.intracel.serde.uint-128-serde/UInt128]] with the `:uint-128` field set to the value provided."
  [str-val]
  (uint-128-from-big-int (BigInteger. str-val)))

(defn uint-128-from-hex-str
  "Creates a valid 128-bit unsigned integer using a numeric hexadecimal argument inside a `String`. 
   This is a convenience function that calls [[uint-128-from-big-int]].
   | Parameter     | Description |
   | --------------|-------------|
   | `str-val`     | Must be a numeric value within a String that will be converted into a `BigInteger`|
   
   Returns: 
   A [[clj.intracel.serde.uint-128-serde/UInt128]] with the `:uint-128` field set to the value provided."
  [hex-str-val]
  (uint-128-from-big-int (BigInteger. hex-str-val 16)))

(defrecord UInt128Serde []
  proto/KVSerde
  (serialize [this data]
    ;;data is a UInt128 with an instance of a BigInteger in the :uint-128 field
    (if (or (nil? data)
            (not (instance? UInt128 data)))
      (throw (ex-info "[uint-128-serde] Invalid argument provided for serialization. Must provide a valid UInt128. See clj.intracel.serde.uint-128-serde for functions to help create a UInt128."
                      {:cause #{:invalid-argument}}))
      (helper/ser-big-int-to-byte-buf (:uint-128 data))))

  (deserialize [this data]
    ;;data is a ByteBuffer that can be deserialized into a BigInteger
    (helper/deser-big-int-from-byte-buf data)))

(defn create []
  (map->UInt128Serde {}))
