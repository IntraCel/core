(ns clj.intracel.kv-store.interface-test
  (:require [clojure.core.async :refer [<!!]]
            [clojure.test :as test :refer :all]
            [clj.intracel.kv-store.interface :as kv-store]
            [clj.intracel.serde.interface :as kv-serdes]))

(deftest dummy-test
  (is (= 1 1)))

(deftest test-kv-store-context-lmdb
  (with-open [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    (is (not (nil? kvs-ctx)))))

(deftest test-context-can-create-db-instance
  (prn "JVM_OPTS:" (System/getenv "JVM_OPTS"))
  (with-open [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    (is (not (nil? kvs-ctx)))
    (try (let [kvs-db-ctx (kv-store/create-kv-store-db-context kvs-ctx :lmdb)]
           (is (not (nil? kvs-db-ctx)))
           (let [dbi (kv-store/db kvs-db-ctx "teal'c" nil [:ic-db-flags/create-db-if-not-exists])]
             (is (not (nil? dbi)))))
         (catch Exception e
           (prn "Error in Test: " (.getMessage e))
           (doseq [tr (.getStackTrace e)]
             (prn "Trace: " tr))))))

(deftest test-kv-put-and-kv-get-with-default-serdes
  (with-open [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    (is (not (nil? kvs-ctx)))
    (try (let [kvs-db-ctx (kv-store/create-kv-store-db-context kvs-ctx :lmdb)]
           (is (not (nil? kvs-db-ctx)))
           (let [dbi (kv-store/db kvs-db-ctx "sg-1" {:ic-chan-opts/buf-size 100} [:ic-db-flags/create-db-if-not-exists])]
             (is (not (nil? dbi)))
             (kv-store/kv-put dbi "general" "Jack O'Neil")
             (kv-store/kv-put dbi "doctor" "Daniel Jackson")
             (kv-store/kv-put dbi "major" "Samantha Carter")
             (kv-store/kv-put dbi "jafa" "Teal'c")
             (let [general (kv-store/kv-get dbi "general")]
               (is (= "Jack O'Neil" general)))))
         (catch Exception e
           (prn "Error in test-kv-put: " (.getMessage e))
           (doseq [tr (.getStackTrace e)]
             (prn "Trace: " tr))))))

(deftest test-kv-put-async-and-get-with-default-serdes
  (with-open [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    (is (not (nil? kvs-ctx)))
    (try (let [kvs-db-ctx (kv-store/create-kv-store-db-context kvs-ctx :lmdb)]
           (is (not (nil? kvs-db-ctx)))
           (prn "JVM_OPTS:" (System/getenv "JVM_OPTS"))
           (let [dbi (kv-store/db kvs-db-ctx "sg-1" {:ic-chan-opts/buf-size 100} [:ic-db-flags/create-db-if-not-exists])]
             (is (not (nil? dbi)))
             (let [async-chans [(kv-store/kv-put-async dbi "general" "Jack O'Neil")
                                (kv-store/kv-put-async dbi "doctor" "Daniel Jackson")
                                (kv-store/kv-put-async dbi "major" "Samantha Carter")
                                (kv-store/kv-put-async dbi "jafa" "Teal'c")]
                   responses   (reduce (fn [acc chan]
                                         (let [answer (<!! chan)]
                                           (conj acc answer)))
                                       []
                                       async-chans)]
               (is (= (count responses) (count async-chans)))
               (is (true? (every? #(:written? %) responses)))

               (let [keys-written (into (sorted-set) (mapv #(:key %) responses))
                     general (kv-store/kv-get dbi "general")]
                 (is (= (sorted-set "doctor" "general" "jafa" "major") keys-written))
                 (is (= "Jack O'Neil" general))))))
         (catch Exception e
           (prn "Error in test-kv-put: " (.getMessage e))
           (doseq [tr (.getStackTrace e)]
             (prn "Trace: " tr))))))

(deftest test-kv-put-with-nippy-string-key-and-nippy-value-serdes
  (with-open [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    (is (not (nil? kvs-ctx)))
    (try (let [kvs-db-ctx (kv-store/create-kv-store-db-context kvs-ctx :lmdb)
               ks         (kv-serdes/string-serde)
               vs         (kv-serdes/nippy-serde)]
           (is (not (nil? kvs-db-ctx)))
           (let [dbi (kv-store/db kvs-db-ctx "movie-reviews" {:ic-chan-opts/buf-size 100} [:ic-db-flags/create-db-if-not-exists])]
             (is (not (nil? dbi)))
             (kv-store/kv-put dbi
                              "Star Wars Episode I"
                              {:title "Star Wars Episode I : The Phantom Menance"
                               :domestic-revenue 431088295.00
                               :mpaa-rating "PG"
                               :tomatometer 0.52
                               :popcornmeter 0.59}
                              ks
                              vs)
             (kv-store/kv-put dbi
                              "Star Wars Episode IV"
                              {:title "Star Wars Episode IV : A New Hope"
                               :domestic-revenue 460998007.00
                               :mpaa-rating "PG"
                               :tomatometer 0.93
                               :popcornmeter 0.96}
                              ks
                              vs)
             (kv-store/kv-put dbi
                              "Star Wars Episode VII"
                              {:title "Star Wars Episode VII : The Force Awakens"
                               :domestic-revenue 936662225.00
                               :mpaa-rating "PG-13"
                               :tomatometer 0.93
                               :popcornmeter 0.84}
                              ks
                              vs)
             (let [highest-grossing-film (reduce (fn [acc film]
                                                   (prn "Processing film: " film)
                                                   (prn "Current Highest Grossing: " acc)
                                                   (if (> (:domestic-revenue film) (:highest acc))
                                                     (assoc acc 
                                                            :highest      (:domestic-revenue film)
                                                            :film-name    (:title film)
                                                            :film-details film)
                                                     acc)
                                                   )
                                                 {:highest      0.00
                                                  :film-name    ""
                                                  :film-details {}}
                                                 [(kv-store/kv-get dbi "Star Wars Episode I" ks vs)
                                                  (kv-store/kv-get dbi "Star Wars Episode IV" ks vs)
                                                  (kv-store/kv-get dbi "Star Wars Episode VII" ks vs)])]
               (is (= "Star Wars Episode VII : The Force Awakens" (:film-name highest-grossing-film)))
               (is (= 936662225.00 (:highest highest-grossing-film)))
               (is (seq (:film-details highest-grossing-film)))
               (is (= 0.93 (get-in highest-grossing-film [:film-details :tomatometer]))))))
         (catch Exception e
           (prn "Error in test-kv-put: " (.getMessage e))
           (doseq [tr (.getStackTrace e)]
             (prn "Trace: " tr))))))