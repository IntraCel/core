(ns clj.intracel.serde.interface
  "The SerDe interface is used to move data in and out of the KV-Store.
   Most of these are usable for both key and value SerDes.
   For SerDes like `big-int-serde`, `big-decimal-serde`, `nippy-serde`, and `string-serde` 
   it is recommended to use caution when serializing keys as there is a 511 byte limit
   in the underlying LMDB embedded database.
   To ease concerns a 511 byte limit supports most of the hashing algorithms easily:
   * MD5 - 16 bytes
   * RIPEMD - 20 bytes
   * SHA256 - 32 bytes
   * SHA384 - 48 bytes
   * SHA512 - 64 bytes
   * WHIRLPOOL - 64 bytes
   * xxhash - 8 bytes by default but a 128-bit xxhash is 16 bytes"
  (:require [clj.intracel.serde.big-decimal-serde :as big-decimal-serde]
            [clj.intracel.serde.big-int-serde :as big-int-serde]
            [clj.intracel.serde.byte-serde :as byte-serde]
            [clj.intracel.serde.double-serde :as double-serde]
            [clj.intracel.serde.float-serde :as float-serde]
            [clj.intracel.serde.int-serde :as int-serde]
            [clj.intracel.serde.long-serde :as long-serde]
            [clj.intracel.serde.nippy-serde :as nippy-serde]
            [clj.intracel.serde.short-serde :as short-serde]
            [clj.intracel.serde.string-serde :as string-serde]
            [clj.intracel.serde.uint-128-serde :as u128-int-serde]
            [clj.intracel.serde.interface :as serde]
            [clj.intracel.api.interface.protocols :as proto :refer [KVSerde]]))

(defn big-decimal-serde
  "Creates a [[clj.intracel.api.interface.protocols/KVSerde]] that can handle large, high-precision fractions represented as `java.math.BigDecimal`.

  Returns: 
  A [[clj.intracel.api.protocols/KVSerde]] that can process `java.math.BigDecimal`s"
  []
  (big-decimal-serde/create))

(defn big-int-serde
  "Creates a [[clj.intracel.api.interface.protocols/KVSerde]] that can handle large integers represented as `java.math.BigInteger`.
   
  Returns: 
  A [[clj.intracel.api.protocols/KVSerde]] that can process `java.math.BigIntegers`s"
  []
  (big-int-serde/create))

(defn byte-serde 
  "Creates a [[clj.intracel.api.interface.protocols/KVSerde]] that can handle a single 8-bit signed integer.
  Min: -128
  Max: 127
  
  Returns: 
  A [[clj.intracel.api.protocols/KVSerde]] that can process a single 8-bit integer."
  []
  (byte-serde/create))

(defn double-serde 
  "Creates a [[clj.intracel.api.interface.protocols/KVSerde]] that can handle a 64-bit signed integer.
  Min: -9223372036854775808
  Max: 9223372036854775807
    
  Returns: 
  A [[clj.intracel.api.protocols/KVSerde]] that can process a 64-bit signed integer."
  []
  (double-serde/create))

(defn float-serde 
  "Creates a [[clj.intracel.api.interface.protocols/KVSerde]] that can handle a 32-bit signed floating point number.
  Min: 1.4E-45
  Max: 3.4028235E38
      
  Returns: 
  A [[clj.intracel.api.protocols/KVSerde]] that can process a 32-bit signed floating point number."
  []
  (float-serde/create))

(defn int-serde 
  "Creates a [[clj.intracel.api.interface.protocols/KVSerde]] that can handle a 32-bit signed integer.
  Min: -2147483648
  Max: 2147483647
        
  Returns: 
  A [[clj.intracel.api.protocols/KVSerde]] that can process a 32-bit signed integer."
  []
  (int-serde/create))

(defn long-serde 
  "Creates a [[clj.intracel.api.interface.protocols/KVSerde]] that can handle a 64-bit signed integer.
  Min: -9223372036854775808
  Max: 9223372036854775807
        
  Returns: 
  A [[clj.intracel.api.protocols/KVSerde]] that can process a 64-bit signed integer."
  []
  (long-serde/create))

(defn nippy-serde 
  "Creates a [[clj.intracel.api.interface.protocols/KVSerde]] that can handle EDN formatted data and convert it to/from nippy.
        
  Returns: 
  A [[clj.intracel.api.protocols/KVSerde]] that can process valid EDN formatted data."
  []
  (nippy-serde/create))

(defn short-serde 
  "Creates a [[clj.intracel.api.interface.protocols/KVSerde]] that can handle a 16-bit signed integer.
  Min: -32768
  Max: 32767
        
  Returns: 
  A [[clj.intracel.api.protocols/KVSerde]] that can process a 16-bit signed integer."
  []
  (short-serde/create))

(defn string-serde 
  "Creates a [[clj.intracel.api.interface.protocols/KVSerde]] that can handle a Clojure string.
          
  Returns: 
  A [[clj.intracel.api.protocols/KVSerde]] that can process a Clojure string."
  []
  (string-serde/create))

(defn u128-int-serde 
  "Creates a [[clj.intracel.api.interface.protocols/KVSerde]] that can handle a 128-bit unsigned integer.
            
  Returns: 
  A [[clj.intracel.api.protocols/KVSerde]] that can process a 128-bit unsigned integer."
  []
  (u128-int-serde/create))

(defn serde-type 
  "Returns the keyword type of the serde provided.
  | Parameter | Description |
  | --------- |-------------|
  | `serde`   | A valid instance of a `clj.intracel.api.protocols/KVSerde` |
     
  Returns: A keyword representing the type of the  `clj.intracel.api.protocols/KVSerde` provided."
  [serde] 
  (if (and (some? serde)
           (satisfies? KVSerde serde))
    (serde/serde-type serde)
    (throw (ex-info "[serde-type] Invalid argument provided. Must provide a valid instance of a clj.intracel.api.protocols/KVSerde."
                    {:cause #{:invalid-argument}}))))