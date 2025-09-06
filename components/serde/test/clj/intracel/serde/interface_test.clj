(ns clj.intracel.serde.interface-test
  (:require [clojure.test :as test :refer :all]
            [clj.intracel.api.interface.protocols :as proto]
            [clj.intracel.serde.interface :as serde]
            [clj.intracel.serde.uint-128-serde :as u128])
  (:import [java.math BigInteger]
           [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]))

(deftest dummy-test
  (is (= 1 1)))

(deftest test-big-decimal-serde
  (testing "When big decimal (larger than 64-bit) is given that it produces a properly built ByteBuffer"
    ;;Double/MAX_VALUE is 1.79E308 so we'll go just a little larger than that
    (let [dec-val       (BigDecimal. "1.8")
          big-dec-val   (.pow dec-val 308)
          big-dec-serde (serde/big-decimal-serde)
          big-dec-buf   ^ByteBuffer (proto/serialize big-dec-serde big-dec-val)]
      (is (not (nil? big-dec-buf)))
      (let [des-dec (proto/deserialize big-dec-serde big-dec-buf)]
        (is (= des-dec big-dec-val))))))

(deftest test-big-int-serde
  (testing "When big integer (larger than 64-bit) is given that it produces a properly built ByteBuffer"
    (let [big-int       "92233720368547758070" ;;10x larger than a Long/MAX_VALUE 
          big-int-serde (serde/big-int-serde)
          int-buf   ^ByteBuffer (proto/serialize big-int-serde big-int)]
      (is (not (nil? int-buf)))
      (let [des-int (proto/deserialize big-int-serde int-buf)]
        (is (= (.toString des-int) big-int))))))

(deftest test-byte-serde
  (testing "When byte is given that it produces a properly built ByteBuffer"
    (let [pos-byte (byte 42)
          byte-serde (serde/byte-serde)
          byte-buf   ^ByteBuffer (proto/serialize byte-serde pos-byte)]
      (is (not (nil? byte-buf)))
      (let [des-int (proto/deserialize byte-serde byte-buf)]
        (is (= des-int pos-byte)))))
  (testing "When negative byte is used and it produces a properly built ByteBuffer"
    (let [neg-byte (int -127)
          byte-serde (serde/byte-serde)
          byte-buf   ^ByteBuffer (proto/serialize byte-serde neg-byte)]
      (is (not (nil? byte-buf)))
      (let [des-byte (proto/deserialize byte-serde byte-buf)]
        (is (= des-byte neg-byte)))))
  (testing "Boundary just one beyond capacity for an 8-bit integer and should throw an exception"
    (let [too-large-byte (+ Byte/MAX_VALUE 1)
          byte-serde (serde/byte-serde)]
      (is (thrown? clojure.lang.ExceptionInfo (proto/serialize byte-serde too-large-byte)))))
  (testing "Boundary just one beyond capacity for a negative 8-bit integer and should throw an exception"
    (let [too-neg-byte (- Byte/MIN_VALUE 1)
          byte-serde (serde/byte-serde)]
      (is (thrown? clojure.lang.ExceptionInfo (proto/serialize byte-serde too-neg-byte))))))

(deftest test-double-serde
  (testing "When double is given that it produces a properly built ByteBuffer"
    (let [pos-double   (double 42.3)
          double-serde (serde/double-serde)
          double-buf   ^ByteBuffer (proto/serialize double-serde pos-double)]
      (is (not (nil? double-buf)))
      (let [des-double (proto/deserialize double-serde double-buf)]
        (is (= des-double pos-double)))))
  (testing "When negative double is used and it produces a properly built ByteBuffer"
    (let [neg-double   (double -713.89)
          double-serde (serde/double-serde)
          double-buf   ^ByteBuffer (proto/serialize double-serde neg-double)]
      (is (not (nil? double-buf)))
      (let [des-double (proto/deserialize double-serde double-buf)]
        (is (= des-double neg-double)))))
  (testing "Boundary is beyond capacity for a 64-bit double and should throw an exception"
    (let [too-large-double (* 2.0 Double/MAX_VALUE)
          double-serde     (serde/double-serde)]
      (is (thrown? clojure.lang.ExceptionInfo (proto/serialize double-serde too-large-double))))))

(deftest test-float-serde
  (testing "When float is given that it produces a properly built ByteBuffer"
    (let [pos-float   (float 42.3)
          float-serde (serde/float-serde)
          float-buf   ^ByteBuffer (proto/serialize float-serde pos-float)]
      (is (not (nil? float-buf)))
      (let [des-float (proto/deserialize float-serde float-buf)]
        (is (= des-float pos-float)))))
  (testing "When negative float is used and it produces a properly built ByteBuffer"
    (let [neg-float   (float -713.89)
          float-serde (serde/float-serde)
          float-buf   ^ByteBuffer (proto/serialize float-serde neg-float)]
      (is (not (nil? float-buf)))
      (let [des-float (proto/deserialize float-serde float-buf)]
        (is (= des-float neg-float)))))
  (testing "Boundary is beyond capacity for a 32-bit float and should throw an exception"
    (let [too-large-float (* 2.0 Float/MAX_VALUE)
          float-serde     (serde/float-serde)]
      (is (thrown? clojure.lang.ExceptionInfo (proto/serialize float-serde too-large-float))))))


(deftest test-int-serde
  (testing "When integer is given that it produces a properly built ByteBuffer"
    (let [pos-int (int 42)
          int-serde (serde/int-serde)
          int-buf   ^ByteBuffer (proto/serialize int-serde pos-int)]
      (is (not (nil? int-buf)))
      (let [des-int (proto/deserialize int-serde int-buf)]
        (is (= des-int pos-int)))))
  (testing "When negative integer is used and it produces a properly built ByteBuffer"
    (let [neg-int (int -713)
          int-serde (serde/int-serde)
          int-buf   ^ByteBuffer (proto/serialize int-serde neg-int)]
      (is (not (nil? int-buf)))
      (let [des-int (proto/deserialize int-serde int-buf)]
        (is (= des-int neg-int)))))
  (testing "Boundary just one beyond capacity for a 32-bit integer and should throw an exception"
    (let [too-large-int (+ Integer/MAX_VALUE 1)
          int-serde (serde/int-serde)]
      (is (thrown? clojure.lang.ExceptionInfo (proto/serialize int-serde too-large-int)))))
  (testing "Boundary just one beyond capacity for a negative 32-bit integer and should throw an exception"
    (let [too-neg-int (- Integer/MIN_VALUE 1)
          int-serde (serde/int-serde)]
      (is (thrown? clojure.lang.ExceptionInfo (proto/serialize int-serde too-neg-int))))))

(deftest test-long-serde
  (testing "When long is given that it produces a properly built ByteBuffer"
    (let [pos-long   (long 42)
          long-serde (serde/long-serde)
          long-buf   ^ByteBuffer (proto/serialize long-serde pos-long)]
      (is (not (nil? long-buf)))
      (let [des-long (proto/deserialize long-serde long-buf)]
        (is (= des-long pos-long)))))
  (testing "When negative long is used and it produces a properly built ByteBuffer"
    (let [neg-long   (long -713)
          long-serde (serde/long-serde)
          long-buf   ^ByteBuffer (proto/serialize long-serde neg-long)]
      (is (not (nil? long-buf)))
      (let [des-long (proto/deserialize long-serde long-buf)]
        (is (= des-long neg-long)))))
  (testing "Boundary just one beyond capacity for a 32-bit integer and should throw an exception"
    (let [too-large-long (+ Long/MAX_VALUE BigInteger/ONE)
          long-serde     (serde/long-serde)]
      (is (thrown? clojure.lang.ExceptionInfo (proto/serialize long-serde too-large-long)))))
  (testing "Boundary just one beyond capacity for a negative 32-bit integer and should throw an exception"
    (let [too-neg-long (- Long/MIN_VALUE BigInteger/ONE)
          long-serde   (serde/int-serde)]
      (is (thrown? clojure.lang.ExceptionInfo (proto/serialize long-serde too-neg-long))))))

(deftest test-nippy-serde
  (testing "When a nippy is given that it produces a properly built ByteBuffer"
    (let [edn-data    {:table [{:rating 0.90 :name "Stargate SG-1" :ts (System/currentTimeMillis)}
                               {:rating 1.00 :name "Rogue One: A Star Wars Story" :ts (System/currentTimeMillis)}]}
          nippy-serde (serde/nippy-serde)
          buf         ^ByteBuffer (proto/serialize nippy-serde edn-data)]
      (is (not (nil? buf)))
      (is (instance? ByteBuffer buf))
      (let [des-edn        (proto/deserialize nippy-serde buf)]
        (is (= des-edn edn-data))))))

(deftest test-short-serde
  (testing "When short is given that it produces a properly built ByteBuffer"
    (let [pos-short (short 42)
          short-serde (serde/short-serde)
          short-buf   ^ByteBuffer (proto/serialize short-serde pos-short)]
      (is (not (nil? short-buf)))
      (let [des-short (proto/deserialize short-serde short-buf)]
        (is (= des-short pos-short)))))
  (testing "When negative byte is used and it produces a properly built ByteBuffer"
    (let [neg-short (int -32768)
          short-serde (serde/short-serde)
          short-buf   ^ByteBuffer (proto/serialize short-serde neg-short)]
      (is (not (nil? short-buf)))
      (let [des-short (proto/deserialize short-serde short-buf)]
        (is (= des-short neg-short)))))
  (testing "Boundary just one beyond capacity for an 8-bit integer and should throw an exception"
    (let [too-large-short (+ Short/MAX_VALUE 1)
          short-serde (serde/short-serde)]
      (is (thrown? clojure.lang.ExceptionInfo (proto/serialize short-serde too-large-short)))))
  (testing "Boundary just one beyond capacity for a negative 8-bit integer and should throw an exception"
    (let [too-neg-short (- Short/MIN_VALUE 1)
          short-serde (serde/short-serde)]
      (is (thrown? clojure.lang.ExceptionInfo (proto/serialize short-serde too-neg-short))))))

(deftest test-string-serde
  (testing "When a String is given that it produces a properly built ByteBuffer"
    (let [small-str        "Dr. Daniel Jackson"
          small-str-serde  (serde/string-serde)
          small-buf        ^ByteBuffer (proto/serialize small-str-serde small-str)]
      (is (not (nil? small-buf)))
      (let [des-str        (proto/deserialize small-str-serde small-buf)]
        (is (= des-str small-str)))))

  (testing "When a longer string is given that it produces a properly build ByteBuffer"
    (let [larger-str       "a;sdlkfa;lkjf; uue3ql ;auu af;sdklf;asduf up;alsdfu u3k1l4;521l3342o89 ;nA;F;ASDLFI IYYT651;1;L34K24U42L3L U ALDHFHSADFJ a;sdlkfa;lkjf; uue3ql ;auu af;sdklf;asduf up;alsdfu u3k1l4;521l3342o89 ;nA;F;ASDLFI IYYT651;1;L34K24U42L3L U ALDHFHSADFJ a;sdlkfa;lkjf; uue3ql ;auu af;sdklf;asduf up;alsdfu u3k1l4;521l3342o89 ;nA;F;ASDLFI IYYT651;1;L34K24U42L3L U ALDHFHSADFJ"
          larger-str-serde (serde/string-serde)
          larger-buf       ^ByteBuffer (proto/serialize larger-str-serde larger-str)]
      (is (not (nil? larger-buf)))
      (let [des-str        (proto/deserialize larger-str-serde larger-buf)]
        (is (= des-str larger-str))))))

(deftest test-uint-128-serde
  (testing "When a `BigInteger` with a valid unsigned 128-bit value properly produces a ByteBuffer."
    (let [u128-val   (u128/uint-128-from-big-int (BigInteger. "340282366920938463463374607431768211454"))
          u128-serde (serde/u128-int-serde)
          buf        ^ByteBuffer (proto/serialize u128-serde u128-val)]
      (is (not (nil? buf)))
      (let [des-u128 (proto/deserialize u128-serde buf)]
        (is (= (.toString des-u128) (.toString (:uint-128 u128-val)))))))
  (testing "When a `long` with valid unsigned 128-bit value properly produces a ByteBuffer."
    (let [u128-val   (u128/uint-128-from-long Long/MAX_VALUE)
          u128-serde (serde/u128-int-serde)
          buf        ^ByteBuffer (proto/serialize u128-serde u128-val)]
      (is (not (nil? buf)))
      (let [des-u128 (proto/deserialize u128-serde buf)]
        (is (= (.toString des-u128) (.toString (:uint-128 u128-val)))))))
  (testing "When a `String` with valid unsigned 128-bit value properly produces a ByteBuffer."
    (let [u128-val   (u128/uint-128-from-str "340282366920938463463374607431768211454")
          u128-serde (serde/u128-int-serde)
          buf        ^ByteBuffer (proto/serialize u128-serde u128-val)]
      (is (not (nil? buf)))
      (let [des-u128 (proto/deserialize u128-serde buf)]
        (is (= (.toString des-u128) (.toString (:uint-128 u128-val)))))))
  (testing "When a `String` with valid unsigned 128-bit value properly produces a ByteBuffer."
    (let [u128-val   (u128/uint-128-from-hex-str "1A")
          u128-serde (serde/u128-int-serde)
          buf        ^ByteBuffer (proto/serialize u128-serde u128-val)]
      (is (not (nil? buf)))
      (let [des-u128 (proto/deserialize u128-serde buf)]
        (is (= (.toString des-u128) (.toString (:uint-128 u128-val)))))))
  (testing "When a value provided is greater than the max value allowed for a 128-bit unsigned integer."
    (let [too-large  (BigInteger. "340282366920938463463374607431768211456") ;;too large by 1
          u128-serde (serde/u128-int-serde)]
      (is (thrown? clojure.lang.ExceptionInfo (proto/serialize u128-serde too-large)))))
  (testing "When a value provide is negative which is not allowed for 128-bit unsigned integer."
    (let [neg  (BigInteger. "-1") ;;negative value
          u128-serde (serde/u128-int-serde)]
      (is (thrown? clojure.lang.ExceptionInfo (proto/serialize u128-serde neg))))))

(deftest test-serde-type
  (testing "When a valid serde is provided that it returns the correct keyword type."
    (let [big-dec-serde (serde/big-decimal-serde)
          big-int-serde (serde/big-int-serde)
          byte-serde    (serde/byte-serde)
          double-serde  (serde/double-serde)
          float-serde   (serde/float-serde)
          int-serde     (serde/int-serde)
          long-serde    (serde/long-serde)
          nippy-serde   (serde/nippy-serde)
          short-serde   (serde/short-serde)
          string-serde  (serde/string-serde)
          u128-serde    (serde/u128-int-serde)]
      (is (= :big-decimal (serde/serde-type big-dec-serde)))
      (is (= :big-int     (serde/serde-type big-int-serde)))
      (is (= :byte        (serde/serde-type byte-serde)))
      (is (= :double      (serde/serde-type double-serde)))
      (is (= :float       (serde/serde-type float-serde)))
      (is (= :int         (serde/serde-type int-serde)))
      (is (= :long        (serde/serde-type long-serde)))
      (is (= :nippy       (serde/serde-type nippy-serde)))
      (is (= :short       (serde/serde-type short-serde)))
      (is (= :string      (serde/serde-type string-serde)))
      (is (= :uint-128    (serde/serde-type u128-serde)))
      )))


