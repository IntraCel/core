(ns clj.intracel.kv-store.lmdb
  (:require [clj.intracel.api.interface.protocols :as proto]
            [clj.intracel.serde.interface :as serde]
            [clojure.core.async
             :as a
             :refer [>! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout]]
            [clojure.java.io :as io]
            [clojure.string :as st] 
            [taoensso.timbre :as log])
  (:import [clj.intracel.api.interface.protocols KVStoreDbContextApi]
           [java.io File]
           [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]
           [org.lmdbjava ByteBufferProxy Dbi DbiFlags Env EnvFlags PutFlags Txn])
  (:gen-class))
;; In order for LMDB Java to load into memory properly, the JVM_OPTS variable needs the 
;; following values set:
;; --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED
;; These have already been set in order to start Calva on VSCode with the proper settings 
;; in core.code-workspace under jackInEnv.

;; If you want to run poly test :dev, you'll need to source set-jvm-opts.sh first to
;; configure JVM_OPTS properly in your shell.

;; This component implements a basic API that can use the Lightning 
;; Memory-mapped Database (LMDB).
;; This is a unique Key Value store that is incredibly fast and requires little 
;; tuning.
;; History and Overivew: https://en.wikipedia.org/wiki/Lightning_Memory-Mapped_Database
;; Benchmarks:  http://www.lmdb.tech/bench/microbench/
;; https://dev.to/plaintextnerds/lmdb-faster-nosql-than-mongodb-ae6
;; Great quote from this article: 
;; "Yes, LMDB absolutely obliterates MongoDB, in almost every way measurable when dealing 
;; with high load."

(def UTF_8 StandardCharsets/UTF_8)
(def log-prefix "lmdb")
(def one-gigabyte 1073741824)
(def default-mem-size one-gigabyte)

(declare translate-dbi-flags configure-channel pre-process-key-for-get pre-process-key-and-value-for-put)



(defrecord LmdbRec [cmd-chan dbi key-serde kvs-ctx max-key-size pre-get-hook-fn pre-put-hook-fn val-serde]
  proto/KVStoreDbiApi
  (start [kvs-db]
    (log/info "[kv-store](LmdbRec) Starting KV-Store Service.")

    (do (go
          (log/info "[kv-store](LmdbRec) Starting command channel listener.")
          (loop [c 0]
            (when (= (mod c 100) 0)
              (log/debug "[kv-store](LmdbRec) Listening in go loop."))
            (let [{:keys [ack-chan cmd key value k-serde v-serde]} (<! @cmd-chan)
                  [key value] (pre-process-key-and-value-for-put key value @pre-put-hook-fn)
                  ks      (or k-serde @key-serde)
                  vs      (or v-serde @val-serde)
                  _       (log/debugf "[kv-store](LmdbRec) Serializing key: %s, val: %s" key value)
                  key-buf (proto/serialize ks key)
                  val-buf (proto/serialize vs value)]
              (with-open [txn ^Txn (.txnWrite  (:ctx kvs-ctx))]
                (case cmd
                  :put    (do
                            (.put dbi txn key-buf val-buf (make-array PutFlags 1))
                            (log/debugf "[kv-put](LmdbRec) put call made for key: %s, awaiting completion of transaction." key))
                  :delete (do
                            (.delete dbi txn key-buf)
                            (log/debugf "[kv-delete](LmdbRec) delete call made for key: %s, awating completion of transaction." key)))
                (.commit txn)
                (when (some? ack-chan)
                  (>! ack-chan {:key key :written? true}))
                (log/debugf "[kv-store](LmdbRec) Committed key %s to KV-Store." key)))
            (recur (inc c))))
        (log/info "[kv-store](LmdbRec) KV-Store Service started.")
        kvs-db))
  (stop [kvs-db]
    (log/info "[kv-store](LmdbRec) Shutting down KV-Store Service.")
    (when (some? @cmd-chan)
      (log/info "[kv-store](LmdbRec) Shutting down command processing channel.")
      (close! @cmd-chan)
      (log/info "[kv-store](LmdbRec) Command processing channel closed."))
    (log/info "[kv-store](LmdbRec) KV-Store Service shut-down complete.")
    kvs-db)


  (set-key-serde [kvs-db key-serde]
    (reset! (:key-serde kvs-db) key-serde)
    kvs-db)

  (set-val-serde [kvs-db val-serde]
    (reset! (:val-serde kvs-db) val-serde)
    kvs-db)

  (set-pre-put-hook [kvs-db pre-fn]
    (reset! (:pre-put-hook-fn kvs-db) pre-fn)
    kvs-db)

  (kv-put [kvs-db key value]
    (log/debugf "[kv-put](LmdbRec) Putting key: %s with value: %s" key value)
    (let [one-shot-ack-chan (chan 1)]
      (go (log/debug "[kv-put](LmdbRec) Using go block to send key and value over command channel.")
          (>! @cmd-chan {:ack-chan one-shot-ack-chan
                         :cmd      :put
                         :key      key
                         :value    value}))
      (let [res (<!! one-shot-ack-chan)]
        (log/debugf "[kv-put](LmdbRec) Received acknowledgement of key written: %s" res)
        res)))

  (kv-put [kvs-db key value key-serde val-serde]
    (log/debugf "[kv-put](LmdbRec) Putting key: %s with value: %s" key value)
    (log/debugf "[kv-put](LmdbRec) Using provided key SerDe of type: %s and value SerDe of: %s" (type key-serde) (type val-serde))
    (let [one-shot-ack-chan (chan 1)]
      (go (log/debug "[kv-put](LmdbRec) Using go block to send key and value over command channel.")
          (>! @cmd-chan {:ack-chan one-shot-ack-chan
                         :cmd      :put
                         :key      key
                         :value    value
                         :k-serde  key-serde
                         :v-serde  val-serde}))
      (let [res (<!! one-shot-ack-chan)]
        (log/debugf "[kv-put](LmdbRec) Received acknowledgement of key written: %s" res)
        res)))

  (kv-put-async [kvs-db key value]
    (log/debugf "[kv-put-async](LmdbRec) Putting key: %s with value: %s" key value)
    (let [one-shot-ack-chan (chan 1)]
      (go (log/debug "[kv-put](LmdbRec) Using go block to send key and value over command channel.")
          (>! @cmd-chan {:ack-chan one-shot-ack-chan
                         :cmd      :put
                         :key      key
                         :value    value}))
      (log/debugf "[kv-put-async](LmdbRec) Returning async channel for consumer to listen for acknowledgement.")
      one-shot-ack-chan))

  (kv-put-async [kvs-db key value key-serde val-serde]
    (log/debugf "[kv-put-async](LmdbRec) Putting key: %s with value: %s" key value)
    (log/debugf "[kv-put-async](LmdbRec) Using provided key SerDe of type: %s and value SerDe of: %s" (type key-serde) (type val-serde))
    (let [one-shot-ack-chan (chan 1)]
      (go (log/debug "[kv-put](LmdbRec) Using go block to send key and value over command channel.")
          (>! @cmd-chan {:ack-chan one-shot-ack-chan
                         :cmd      :put
                         :key      key
                         :value    value
                         :k-serde  key-serde
                         :v-serde  val-serde}))
      (log/debugf "[kv-put-async](LmdbRec) Returning async channel for consumer to listen for acknowledgement.")
      one-shot-ack-chan))

  (set-pre-get-hook [kvs-db pre-fn]
    (reset! (:pre-get-hook-fn kvs-db) pre-fn)
    kvs-db)

  (kv-get [kvs-db key]
    (log/debugf "[kv-get](LmdbRec) Retrieving key %s from KV-Store." key)
    (log/debugf "[kv-get](LmdbRec) (:ctx kvs-ctx) type is: %s" (type (:ctx kvs-ctx)))
    (log/debugf "[kv-get](LmdbRec) (:ctx kvs-ctx) value is: %s" {:ctx kvs-ctx})
    (with-open [txn ^Txn (.txnRead (:ctx kvs-ctx))]
      (let [key-buf    (proto/serialize @key-serde (pre-process-key-for-get key @pre-get-hook-fn))
            found      (.get dbi txn key-buf)]
        (if (some? found)
          (proto/deserialize @val-serde found)
          nil))))

  (kv-get [kvs-db key k-serde v-serde]
    (log/debugf "[kv-get](LmdbRec) Retrieving key %s from KV-Store with provided SerDes." key)
    (let [ks         (or k-serde @(:key-serde kvs-db))
          vs         (or v-serde @(:val-serde kvs-db))]
      (with-open [txn ^Txn (.txnRead (:ctx kvs-ctx))]
        (let [key-buf (proto/serialize ks (pre-process-key-for-get key @pre-get-hook-fn))
              found   (.get dbi txn key-buf)]
          (if (some? found)
            (proto/deserialize vs found)
            nil)))))

  (kv-del [kvs-db key]
    (go (>! @cmd-chan {:cmd :delete
                       :key key})))

  (kv-del [kvs-db key k-serde]
    (let [ks (or k-serde (:key-serde kvs-db))]
      (go (>! @cmd-chan {:cmd    :delete
                         :key     key
                         :k-serde ks})))))

(defn- pre-process-key-for-get [key pre-get-hook-fn]
  (log/debugf "[pre-process-key-for-get] key: %s" key)
  (log/debugf "[pre-process-key-for-get] pre-get-hook-fn type: %s" (type pre-get-hook-fn))
  (let [pre-key-fn (if (some? pre-get-hook-fn)
                     pre-get-hook-fn
                     nil)]
    (if (some? pre-key-fn)
      (let [transformed-key (pre-key-fn key)]
        (if (nil? transformed-key)
          (throw (ex-info "[pre-get-hook-fn] The pre-get-hook-fn provided returned an unusable key for performing a kv-get operation on the KV-Store."
                          {:cause #{:invalid-response-from-pre-get-hook-fn}}))
          transformed-key))
      key)))

(defn- pre-process-key-and-value-for-put [key value pre-put-hook-fn]
  (log/debugf "[pre-process-key-and-value-for-put] key: %s, value: %s" key value)
  (log/debugf "[pre-process-key-and-value-for-put] pre-put-hook-fn type: %s" (type pre-put-hook-fn))
  (let [pre-put-fn (if (some? pre-put-hook-fn)
                     pre-put-hook-fn
                     nil)]
    (if (some? pre-put-fn)
      (let [[transformed-key transformed-value] (pre-put-fn key value)]
        (if (or (nil? transformed-key)
                (nil? transformed-value))
          (throw (ex-info "[pre-put-hook-fn] The pre-put-hook-fn provided returned an unusable key or value for performing a kv-put operation on the KV-Store."
                          {:cause #{:invalid-response-from-pre-put-hook-fn}}))
          [transformed-key transformed-value]))
      [key value])))


(defn create-kv-store-context [{:keys [intracel.kv-store.lmdb/storage-path
                                       intracel.kv-store.lmdb/keyspace-max-mem-size
                                       intracel.kv-store.lmdb/num-dbs]}]
  (let [path              (if-not (st/blank? storage-path)
                            (io/file storage-path)
                            (throw (ex-info (format "[%s/create-kv-store-context] Unable to produce a new KVStoreContext. Are you missing a :intracel.kv-store.lmdb/storage-path in ctx-opts?" log-prefix) {:cause :missing-storage-path})))
        _                 (.mkdirs path)
        _                 (log/debugf "[%s/create-kv-store-context] - path is: %s" log-prefix storage-path)
        map-size          (or keyspace-max-mem-size default-mem-size)
        _                 (log/debugf "[%s/create-kv-store-context] - map-size is: %s bytes" log-prefix map-size)
        db-instance-count (or num-dbs 1)
        _                 (log/debugf "[%s/create-kv-store-context] - db-instance-count is: %s" log-prefix db-instance-count)
        env-flags         (make-array EnvFlags 1)
        env               ^Env (try (-> (Env/create)
                                        ;; LMDB needs to know how large our DB may become. Over-estimating is OK
                                        ;; This sets the map size in bytes
                                        (.setMapSize map-size)
                                        ;; LMDB needs to know how many DBs (Dbi) we want to store in this Env
                                        (.setMaxDbs db-instance-count)
                                        ;; Open the Env. The same path can be concurrently opened and used in 
                                        ;; different processes, but do not open the same path twice in the same 
                                        ;; process at the same time.
                                        (.open path env-flags))
                                    (catch Exception e
                                      (log/errorf "[%s/create-kv-store-context] Error: %s" log-prefix (.getMessage e))
                                      (throw (ex-info (format "[%s/create-kv-store-context] Error: %s" log-prefix (.getMessage e)) {:cause :unable-to-start-lmdb-env}))))]
    {:ctx env}))

(defrecord LmdbContext [kvs-ctx db-instances]
  KVStoreDbContextApi
  (db [this db-name]
    (proto/db this db-name nil nil nil nil))

  (db [this db-name chan-opts]
    (proto/db this db-name chan-opts nil nil nil))

  (db [this db-name chan-opts db-opts]
    (proto/db this db-name chan-opts db-opts nil nil))

  (db [this db-name chan-opts db-opts pre-get-hook-fn pre-put-hook-fn]
    (let [max-key-size (.getMaxKeySize (:ctx kvs-ctx))
          dbi (if (contains? @(:db-instances this) db-name)
                (get @(:db-instances this) db-name)
                (let [dbi-flags (translate-dbi-flags db-opts)
                      _         (log/debugf "[db] dbi-flags type: %s, count: %s, values: %s" (type dbi-flags) (count dbi-flags) (into [] dbi-flags))
                      new-db    ^Dbi (.openDbi (:ctx kvs-ctx) db-name dbi-flags)
                      instance  (map->LmdbRec {:cmd-chan        (atom (configure-channel chan-opts))
                                               :dbi             new-db
                                               :key-serde       (atom (serde/string-serde))
                                               :kvs-ctx         kvs-ctx
                                               :max-key-size    max-key-size
                                               :pre-get-hook-fn (atom pre-get-hook-fn)
                                               :pre-put-hook-fn (atom pre-put-hook-fn)
                                               :val-serde       (atom (serde/string-serde))})
                      _         (log/debug "[db] Starting Database Instance")
                      started   (proto/start instance)]
                  (swap! (:db-instances this) assoc db-name started)
                  instance))]
      dbi)))

(defn create-kv-db-context [kvs-ctx]
  (map->LmdbContext {:db-instances (atom {})
                     :kvs-ctx kvs-ctx}))

(defn translate-dbi-flags [db-opts]
  (log/debugf "[translate-dbi-flags] db-opts: %s" db-opts)
  (into-array DbiFlags (mapv (fn [db-opt]
                               (log/debugf "[translate-dbi-flags] db-opt: %s" db-opt)
                               (cond (= db-opt :ic-db-flags/reverse-key)                           DbiFlags/MDB_REVERSEKEY
                                     (= db-opt :ic-db-flags/multi-value-keys)                      DbiFlags/MDB_DUPSORT
                                     (= db-opt :ic-db-flags/integer-keys)                          DbiFlags/MDB_INTEGERKEY
                                     (= db-opt :ic-db-flags/sort-fixed-sized-duplicate-items)      DbiFlags/MDB_DUPFIXED
                                     (= db-opt :ic-db-flags/duplicates-are-binary-integers)        DbiFlags/MDB_INTEGERDUP
                                     (= db-opt :ic-db-flags/compare-duplicates-as-reverse-strings) DbiFlags/MDB_REVERSEDUP
                                     (= db-opt :ic-db-flags/create-db-if-not-exists)               DbiFlags/MDB_CREATE))
                             db-opts)))

(defn configure-channel [chan-opts]
  (log/debugf "[configure-channel] chan-opts: %s" chan-opts)
  (if (and (map? chan-opts)
           (seq chan-opts))
    (cond (contains? chan-opts :ic-chan-opts/replacement-chan) (:ic-chan-opts/replacement-chan chan-opts)
          (contains? chan-opts :ic-chan-opts/buf-size)         (chan (buffer (:ic-chan-opts/buf-size chan-opts))))
    (chan (buffer 10000))))

(comment
  (def path (io/file (str (System/getProperty "java.io.tmpdir") "/lmdb/")))
  (.mkdirs path)
  (prn "path data type: " (type path))
  (def one-gigabyte 1073741824)
  (def map-size one-gigabyte)
  (def env-flags (make-array EnvFlags 1))
  (prn "env-flags: " env-flags)
  (prn "env-flags count: " (count env-flags))
  (def dbs 3)
  ;; This tells LMDB options to use when creating the database.
  ;; Use reverse string keys.
  ;; Keys are strings to be compared in reverse order, from the end of the 
  ;; strings to the beginning. By default, keys are treated as strings and 
  ;; compared from beginning to end.
  ;; MDB_REVERSEKEY(0x02),
  ;;
  ;; Use sorted duplicates. 
  ;; Duplicate keys may be used in the database. Or, from another perspective,
  ;; keys may have multiple data items, stored in sorted order. By default keys
  ;; must be unique and may have only a single data item.
  ;; MDB_DUPSORT(0x04),
  ;; 
  ;; Numeric keys in native byte order: either unsigned int or size_t. The keys
  ;; must all be of the same size.
  ;; MDB_INTEGERKEY(0x08),
  ;;
  ;; With {@link #MDB_DUPSORT}, sorted dup items have fixed size.
  ;; This flag may only be used in combination with {@link #MDB_DUPSORT}. This
  ;; option tells the library that the data items for this database are all the
  ;; same size, which allows further optimizations in storage and retrieval.
  ;; When all data items are the same size, the {@link SeekOp#MDB_GET_MULTIPLE}
  ;; and {@link SeekOp#MDB_NEXT_MULTIPLE} cursor operations may be used to
  ;; retrieve multiple items at once.
  ;; MDB_DUPFIXED(0x10),
  ;;
  ;; With {@link #MDB_DUPSORT}, dups are {@link #MDB_INTEGERKEY}-style integers.
  ;; This option specifies that duplicate data items are binary integers,
  ;; similar to {@link #MDB_INTEGERKEY} keys.
  ;; MDB_INTEGERDUP(0x20),
  ;;
  ;; With {@link #MDB_DUPSORT}, use reverse string dups.
  ;; This option specifies that duplicate data items should be compared as
  ;; strings in reverse order.
  ;; MDB_REVERSEDUP(0x40),
  ;;
  ;; Create the named database if it doesn't exist.
  ;; This option is not allowed in a read-only transaction or a read-only
  ;; environment.
  ;; MDB_CREATE(0x4_0000);
  (def dbi-flags (into-array DbiFlags [DbiFlags/MDB_CREATE]))

  ;; This line will fail to evaluate if JAVA_OPTS have not been set as described above.
  (def byte-buf-proxy ByteBufferProxy/PROXY_SAFE)
  ;; LMDB always needs and Env. An Env owns a physical on-disk storage file.
  ;; One Env can store multiple databases (e.g. - sorted maps)
  (def env ^Env (-> (Env/create)
               ;; LMDB needs to know how large our DB may become. Over-estimating is OK
               ;; This sets the map size in bytes
                    (.setMapSize map-size)
               ;; LMDB needs to know how many DBs (Dbi) we want to store in this Env
                    (.setMaxDbs dbs)
               ;; Open the Env. The same path can be concurrently opened and used in 
               ;; different processes, but do not open the same path twice in the same 
               ;; process at the same time.
                    (.open path env-flags)))
  (def customer-db "customers")
  ;; We need a Dbi for each DB. A Dbi roughly equates to a sorted map. The
  ;; MDB_CREATE flag causes the DB to be created if it doesn't already exist.
  (def db ^Dbi (.openDbi env customer-db dbi-flags))
  ;; We want to store some data, so we will need a direct ByteBuffer.
  ;; Note that LMDB keys cannot exceed maxKeySize bytes (511 bytes by default) .
  ;; Values can be larger.
  (def db-key ^ByteBuffer (ByteBuffer/allocateDirect (.getMaxKeySize env)))
  (def db-val ^ByteBuffer (ByteBuffer/allocateDirect 700))

  ;; In Java NIO, the ByteBuffer.flip() method is used to transition a buffer from its 
  ;; "writing mode" to its "reading mode" by essentially setting the buffer's limit to 
  ;; its current position and then resetting the position back to zero, effectively 
  ;; preparing the buffer to be read from the beginning of the data it has accumulated 
  ;; so far.
  ;; See https://docs.oracle.com/javase/6/docs/api/java/nio/Buffer.html#flip()
  (-> db-key
      (-> (.put (.getBytes "greeting" UTF_8))
          (.flip)))
  (-> db-val
      (-> (.put (.getBytes "Hello World" UTF_8))
          (.flip)))

  (def val-size (.remaining db-val))
  ;; Now store it. Dbi.put () internally begins and commits a transaction (Txn) .
  (.put db db-key db-val)


  ;; To fetch any data from LMDB we need a Txn. A Txn is very important in
  ;; LmdbJava because it offers ACID characteristics and internally holds a
  ;; read-only key buffer and read-only value buffer. These read-only buffers
  ;; are always the same two Java objects, but point to different LMDB-managed
  ;; memory as we use Dbi (and Cursor) methods. These read-only buffers remain
  ;; valid only until the Txn is released or the next Dbi or Cursor call. If
  ;; you need data afterwards, you should copy the bytes to your own buffer.

  (with-open [txn (.txnRead env)]
    (let [found (.get db txn db-key)]
      (prn "Found: " (str (.decode UTF_8 found)))))

  (.close env))