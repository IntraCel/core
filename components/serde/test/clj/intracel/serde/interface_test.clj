(ns clj.intracel.serde.interface-test
  (:require [clojure.test :as test :refer :all]
            [clj.intracel.api.interface.protocols :as proto]
            [clj.intracel.serde.interface :as serde]
            [clj.intracel.serde.uint-128-serde :as u128]
            [clj.intracel.serde.uint-128-serde :as u128-int-serde])
  (:import [java.math BigInteger]
           [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]))

(deftest dummy-test
  (is (= 1 1)))

(deftest test-big-int-serde
  (testing "When big integer (larger than 32-bit) is given that it produces a properly built ByteBuffer"
    (let [big-int       "92233720368547758070" ;;10x larger than a Long/MAX_VALUE 
          big-int-serde (serde/big-int-serde)
          int-buf   ^ByteBuffer (proto/serialize big-int-serde big-int)]
      (is (not (nil? int-buf)))
      (let [des-int (proto/deserialize big-int-serde int-buf)]
        (is (= (.toString des-int) big-int))))))

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

(deftest test-string-serde
  (testing "When a String is given that it produces a properly built ByteBuffer"
    (let [small-str        "Dr. Daniel Jackson"
          small-str-serde  (serde/string-serde (count small-str))
          small-buf        ^ByteBuffer (proto/serialize small-str-serde small-str)]
      (is (not (nil? small-buf)))
      (let [des-str        (proto/deserialize small-str-serde small-buf)]
        (is (= des-str small-str)))))

  (testing "When a longer string is given that it produces a properly build ByteBuffer"
    (let [larger-str       "a;sdlkfa;lkjf; uue3ql ;auu af;sdklf;asduf up;alsdfu u3k1l4;521l3342o89 ;nA;F;ASDLFI IYYT651;1;L34K24U42L3L U ALDHFHSADFJ a;sdlkfa;lkjf; uue3ql ;auu af;sdklf;asduf up;alsdfu u3k1l4;521l3342o89 ;nA;F;ASDLFI IYYT651;1;L34K24U42L3L U ALDHFHSADFJ a;sdlkfa;lkjf; uue3ql ;auu af;sdklf;asduf up;alsdfu u3k1l4;521l3342o89 ;nA;F;ASDLFI IYYT651;1;L34K24U42L3L U ALDHFHSADFJ"
          larger-str-serde (serde/string-serde (count larger-str))
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
        (is (= (.toString des-u128) (.toString (:uint-128 u128-val)))))
      ))
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
      (is (thrown? clojure.lang.ExceptionInfo (proto/serialize u128-serde neg)))))
  
  )

