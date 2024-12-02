(ns clj.intracel.serde.interface-test
  (:require [clojure.test :as test :refer :all] 
            [clj.intracel.api.interface.protocols :as proto]
            [clj.intracel.serde.interface :as serde])
  (:import [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]))

(deftest dummy-test
  (is (= 1 1)))

(deftest test-string-serde
  (testing "When a String is given that it produces a properly built ByteBuffer"
    (let [small-str        "Dr. Daniel Jackson"
          small-str-serde  (serde/string-serde (count small-str))
          small-buf        ^ByteBuffer (proto/serialize small-str-serde small-str)] 
      (is (not (nil? small-buf)))
      (let [des-str        (proto/deserialize small-str-serde small-buf)]
        (is (= des-str small-str)))
      ))
  
  (testing "When a longer string is given that it produces a properly build ByteBuffer"
    (let [larger-str       "a;sdlkfa;lkjf; uue3ql ;auu af;sdklf;asduf up;alsdfu u3k1l4;521l3342o89 ;nA;F;ASDLFI IYYT651;1;L34K24U42L3L U ALDHFHSADFJ a;sdlkfa;lkjf; uue3ql ;auu af;sdklf;asduf up;alsdfu u3k1l4;521l3342o89 ;nA;F;ASDLFI IYYT651;1;L34K24U42L3L U ALDHFHSADFJ a;sdlkfa;lkjf; uue3ql ;auu af;sdklf;asduf up;alsdfu u3k1l4;521l3342o89 ;nA;F;ASDLFI IYYT651;1;L34K24U42L3L U ALDHFHSADFJ"
          larger-str-serde (serde/string-serde (count larger-str))
          larger-buf       ^ByteBuffer (proto/serialize larger-str-serde larger-str)]
      (is (not (nil? larger-buf)))
      (let [des-str        (proto/deserialize larger-str-serde larger-buf)]
        (is (= des-str larger-str))))))
