(ns clj.intracel.api.interface.protocols
  (:import [java.io Closeable])
  (:gen-class))

(defprotocol KVSerde
  (serialize [kv-serde data]
    "Accepts data and produces a [java.nio.ByteBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html)
    | Parameter   | Description |
    | ------------|-------------|
    | `kv-serde`  | A reference to implementation of the `clj.intracel.api.interface.protocols/KVSerde` to perform the serialization. |
    | `data`      | The data that is meant to be serialized. Could be in any format but should be known to the developer so that the serialization works properly. ||
    Returns: `java.nio.ByteBuffer` to be persisted into the KV-Store. A failed attempt to serialize should result in a `clojure.lang.ExceptionInfo`")
  (deserialize [kv-serde data]
    "Accepts a [java.nio.ByteBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html) and produces it into the desired Clojure data format (e.g. - String, Map, Avro, etc.).
    | Parameter   | Description |
    | ------------|-------------|
    | `kv-serde`  | A reference to implementation of the `clj.intracel.api..interface.protocols/KVSerde` to perform the serialization. |
    | `data`      | The data that is meant to be deserialized. It will come from KV-Store in the form of a `java.nio.ByteBuffer` to be converted into the desired Clojure data format. ||
    Returns: 
    Data converted from a `java.nio.ByteBuffer` coming from the KV-Store into the desired Clojure data format. A failed attempt to deserialize should result in a `clojure.lang.ExceptionInfo`"))

(defrecord KVStoreContext [ctx]
  Closeable
  (close [_]
    (when (some? ctx)
      (.close ctx))))

(defprotocol KVStoreDbContextApi
  (db
    [kvs-db db-name]
    [kvs-db db-name chan-opts]
    [kvs-db db-name chan-opts db-opts]
    [kvs-db db-name chan-opts db-opts pre-del-hook-fn pre-get-hook-fn pre-put-hook-fn]
    "Returns a hosted embedded database that implements the [[clj.intracel.api.interface.protocols/KVStoreDbiApi]]. 
     All multi-arity versions of the kvs-db reference and the db-name.
     The 3-arity version allows the caller to customize the settings of the underlying channel used for writing to the database. 
     This can be a key setting to tune when a high volume of data is being processed. 
     The 4-arity version allows the caller to customize the settings of the underlying KV-Store.
     the 5-arity version allows the caller to set a function the hooks into the lifecyle of a get operation allowing the library to 
     pre-process the key provided before doing the actual lookup.
      
    | Parameter         | Description |
    | ------------------|-------------|
    | `kvs-db`          | A reference to the `clj.intracel.api.kv-store/KVStoreDb` created at initialization. |
    | `chan-opts`       | A map containing options for setting internal communications. To protect access to the database, 
    |                   | KV-Store uses a multi-producer, single-consumer core.async channel pattern so that a single thread safely performs puts to the database. 
    |                   | This allows the user to override the default `(chan (buffer 10000))` to any type of channel desired.
    |                   | Options are:
    |                   | * `:ic-chan-opts/buf-size`: Use a buffered channel with size provided. Defaults to 10,000.,
    |                   | * `:ic-chan-opts/replacement-chan`: Use the core.asyn channel provided instead of the default buffered channel.|
    | `db-name`         | The UTF-8 formatted name of the database to use. If this is the first time using it, the database instance will be created automatically. |
    | `db-opts`         | Vector containing options to adjust the behavior of the database when initializing. 
    |                   | 
    |                   | * `:ic-db-flags/reverse-key`: Use reverse string keys. Keys are strings to be compared in reverse order, from the end of the  
    |                   |                                   strings to the beginning. By default, keys are treated as strings and 
    |                   |                                   compared from beginning to end.
    |                   |                                   Correlates to [org.lmdbjava.DbiFlags/MDB_REVERSEKEY(0x02)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_REVERSEKEY), 
    |                   | * `:ic-db-flags/multi-value-keys`: Use sorted duplicates. 
    |                   |                                    Duplicate keys may be used in the database. Or, from another perspective, 
    |                   |                                    keys may have multiple data items, stored in sorted order. By default keys 
    |                   |                                    must be unique and may have only a single data item.
    |                   |                                    Correlates to [org.lmdbjava.DbiFlags/MDB_DUPSORT(0x04)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_DUPSORT), 
    |                   | * `:ic-db-flags/integer-keys`: Numeric keys in native byte order: either unsigned int or size_t. The keys 
    |                   |                                    must all be of the same size.
    |                   |                                    Correlates to [org.lmdbjava.DbiFlags/MDB_INTEGERKEY(0x08)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_INTEGERKEY), 
    |                   | * `:ic-db-flags/sort-fixed-sized-duplicate-items`: With {@link #MDB_DUPSORT}, sorted dup items have fixed size. 
    |                   |                                                        This flag may only be used in combination with {@link #MDB_DUPSORT}. This 
    |                   |                                                        option tells the library that the data items for this database are all the
    |                   |                                                        same size, which allows further optimizations in storage and retrieval. 
    |                   |                                                        When all data items are the same size, the {@link SeekOp#MDB_GET_MULTIPLE} 
    |                   |                                                        and {@link SeekOp#MDB_NEXT_MULTIPLE} cursor operations may be used to 
    |                   |                                                        retrieve multiple items at once. 
    |                   |                                                        Correlates to [org.lmdbjava.DbiFlags/MDB_DUPFIXED(0x10)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_DUPFIXED), 
    |                   | * `:ic-db-flags/duplicates-are-binary-integers`: With {@link #MDB_DUPSORT}, dups are {@link #MDB_INTEGERKEY}-style integers. 
    |                   |                                                      This option specifies that duplicate data items are binary integers, 
    |                   |                                                      similar to {@link #MDB_INTEGERKEY} keys. 
    |                   |                                                      Correlates to [org.lmdbjava.DbiFlags/MDB_INTEGERDUP(0x20)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_INTEGERDUP), 
    |                   | * `:ic-db-flags/compare-duplicates-as-reverse-strings`: With {@link #MDB_DUPSORT}, use reverse string dups. 
    |                   |                                                             This option specifies that duplicate data items should be compared as 
    |                   |                                                             strings in reverse order. 
    |                   |                                                             Correlates to [org.lmdbjava.DbiFlags/MDB_REVERSEDUP(0x40)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_REVERSEDUP), 
    |                   | * `:ic-db-flags/create-db-if-not-exists`: Create the named database if it doesn't exist. 
    |                   |                                               This option is not allowed in a read-only transaction or a read-only |
    |                   |                                               environment. 
    |                   |                                               Correlates to [org.lmdbjava.DbiFlagsMDB_CREATE(0x4_0000)(https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_CREATE)|
    | `pre-del-hook-fn` | A function that accepts a single `key` arg and returns the same `key` that will get serialized by the [[clj.intracel.api.kv-store/KVSerde]] used in the [[kv-del]] function. |
    | `pre-get-hook-fn` | A function that accepts a single `key` arg and returns a new `key` that will get serialized by the [[clj.intracel.api.kv-store/KVSerde]] used in the [[kv-get]] function. |
    | `pre-put-hook-fn` | A function that accepts a `key` arg, and a `value` arg and returns a new `key` and `value` in a two-element vector that will get serialized by the [[clj.intracel.api.kv-store/KVSerde]] used in the [[kv-get]] function. |
    Returns:
    An `clj.intracel.api.kv-store/KVStoreDb` that can be used in a builder pattern to compose a KV-Store database instance with the desired settings."))

(defprotocol KVStoreDbiApi
  (start [kvs-db]
    "This effectively acts as a constructor function to an instance of the [[clj.intracel.api.protocols.KVStoreDbiApi]].
     Any start-up logic or state that needs to be initialized would be set up in this function.
    
    Depends on: [[db]] 
    | Parameter   | Description |
    | ------------|-------------|
    | `kvs-db`    | A reference to the `clj.intracel.api.protocols/KVStoreDbiApi` created in the [[db]] function. ||
    Returns:
    A `clj.intracel.api.protocols/KVStoreDbiApi` that can be used in a builder pattern with the default `clj.intracel.api.kv-store/KVSerde` for keys configured.")

  (stop [kvs-db]
    "This shuts down the [[clj.intracel.api.protocols.KVStoreDbiApi]] instance.
     Any shut down logic or state that needs to be stopped would be done here.
        
    Depends on: [[db]] 
    | Parameter   | Description |
    | ------------|-------------|
    | `kvs-db`    | A reference to the `clj.intracel.api.protocols/KVStoreDbiApi` created in the [[db]] function. ||
    Returns:
    A `clj.intracel.api.protocols/KVStoreDbiApi` that can be used in a builder pattern with the default `clj.intracel.api.kv-store/KVSerde` for keys configured.")

  (set-key-serde [kvs-db key-serde]
    "Sets the default key SerDe used for serializing to and deserializing from a [[clj.intracel.api.protocols/KVStoreDbiApi]]

    Depends on: [[db]] 
    | Parameter   | Description |
    | ------------|-------------|
    | `kvs-db`    | A reference to the `clj.intracel.api.protocols/KVStoreDbiApi` created in the [[db]] function. |
    | `key-serde` | An implemenation of the [[clj.intracel.api.kv-store/KVSerde]] If nil, defaults to a [[clj.intracel.serde.interface.string-serde]]. ||
    Returns:
    A `clj.intracel.api.protocols/KVStoreDbiApi` that can be used in a builder pattern with the default `clj.intracel.api.kv-store/KVSerde` for keys configured.")
  (set-val-serde [kvs-db val-serde]
    "Sets the default value SerDe used for serializing to and deserializing from a [[clj.intracel.api.protocols/KVStoreDbiApi]]

    Depends on: [[db]] 
    | Parameter   | Description |
    | ------------|-------------|
    | `kvs-db`    | A reference to the `clj.intracel.api.protocols/KVStoreDbiApi` created in the [[db]] function. |
    | `val-serde` | An implemenation of the [[clj.intracel.api.kv-store/KVSerde]] If nil, defaults to a [[clj.intracel.serde.interface.string-serde]]. ||
    Returns:
    A `clj.intracel.api.protocols/KVStoreDbiApi` that can be used in a builder pattern with the default `clj.intracel.api.kv-store/KVSerde` for keys configured.")
  
  (set-pre-put-hook [kvs-db pre-fn] 
    "This enables the caller to customize the behavior performed when writing a key/value pair in [[kv-put]] and [[kv-put-async]] by allowing caller code to pre-process the key and value.
      This could be useful for performing transformations on the key and value or sending a copy of the data to something else.
      
      Depends on: [[db]] 
      | Parameter   | Description |
      | ------------|-------------|
      | `kvs-db`    | A reference to the `clj.intracel.api.protocols/KVStoreDbiApi` created in the [[db]] function. |
      | `pre-fn`    | A function that accepts a `key` arg, and a `value` arg and returns a new `key` and a new `value` in a two element vector that will get serialized by the [[clj.intracel.api.kv-store/KVSerde]] used in the [[kv-put]] and [[kv-put-async]] functions. ||
  
      Returns:
      A `clj.intracel.api.protocols/KVStoreDbiApi` that can be used in a builder pattern.")
  
  (kv-put
    [kvs-db key value]
    [kvs-db key value key-serde val-serde]
    "Puts a value into the KV-Store. If a value the same key exists, it will be replaced with the value argument provided.
    The 3 parameter version of [[kv-put]] uses the default SerDe (See [[set-key-serde]], [[set-val-serde]]).
    The 5 parameter version of [[kv-put]] allows the caller to supply a specific `clj.intracel.api.kv-store/KVSerde` for the key/value pair provided. 
    It's the caller's responsibility to use the mutli-arity [[kv-get]] function to ensure that the key and value get deserizlized properly.

    Depends on: [[db]] 
    | Parameter   | Description |
    | ------------|-------------|
    | `kvs-db`    | A reference to the `clj.intracel.api.protocols/KVStoreDbiApi` created in the [[db]] function. |
    | `key`       | Uses the default [[clj.intracel.api.kv-store/KVSerde]] to serialize the `key` to a [java.nio.ByteBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html). |
    | `value`     | Uses the default [[clj.intracel.api.kv-store/KVSerde]] to serailize the `value` to a [java.nio.ByteBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html). |
    | `key-serde` | An implemenation of the [[clj.intracel.api.kv-store/KVSerde]]. If nil, defaults to a [[clj.intracel.serde.interface.string-serde]]. This overrides the `clj.intracel.api.kv-store/KVSerde` provided in [[set-key-serde]]. Not available in 3 parameter version of this function.|
    | `val-serde` | An implemenation of the [[clj.intracel.api.kv-store/KVSerde]]. If nil, defaults to a [[clj.intracel.serde.interface.string-serde]]. This overrides the `clj.intracel.api-kv-store/KVSerde` provided in [[set-val-serde]]. Not available in 3 parameter version of this function.||
    Returns:
    A map containing the key `written?` set to true or false. If false, use the `msg` key to check the error message.")
  (kv-put-async
   [kvs-db key value]
   [kvs-db key value key-serde val-serde]
   "Puts a value into the KV-Store. If a value the same key exists, it will be replaced with the value argument provided.
    The 3 parameter version of [[kv-put]] uses the default SerDe (See [[set-key-serde]], [[set-val-serde]]).
    The 5 parameter version of [[kv-put]] allows the caller to supply a specific `clj.intracel.api.kv-store/KVSerde` for the key/value pair provided. 
    It's the caller's responsibility to use the mutli-arity [[kv-get]] function to ensure that the key and value get deserizlized properly.
  
    Depends on: [[db]] 
    | Parameter   | Description |
    | ------------|-------------|
    | `kvs-db`    | A reference to the `clj.intracel.api.protocols/KVStoreDbiApi` created in the [[db]] function. |
    | `key`       | Uses the default [[clj.intracel.api.kv-store/KVSerde]] to serialize the `key` to a [java.nio.ByteBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html). |
    | `value`     | Uses the default [[clj.intracel.api.kv-store/KVSerde]] to serailize the `value` to a [java.nio.ByteBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html). |
    | `key-serde` | An implemenation of the [[clj.intracel.api.kv-store/KVSerde]]. If nil, defaults to a [[clj.intracel.serde.interface.string-serde]]. This overrides the `clj.intracel.api.kv-store/KVSerde` provided in [[set-key-serde]]. Not available in 3 parameter version of this function.|
    | `val-serde` | An implemenation of the [[clj.intracel.api.kv-store/KVSerde]]. If nil, defaults to a [[clj.intracel.serde.interface.string-serde]]. This overrides the `clj.intracel.api-kv-store/KVSerde` provided in [[set-val-serde]]. Not available in 3 parameter version of this function.||
    Returns:
    A core.async channel that consumers can use to listen for acknowledgements. A response will be a map containing the `written?` set to true or false. If false, use the `msg` key to check the error message.")
  
  (set-pre-get-hook [kvs-db pre-fn]
    "This enables the caller to customize the behavior performed when doing a key look-up in [[kv-get]] by allowing caller code to pre-process the key.
    This could be useful for specific keys that could represent sets of data. For example, a CIDR, a wildcard to represent matching to multiple values.
    Potentially, this could also defer to a pre-key processor that checks a specialized in-memory data struction like a Bloom Filter.
    
    Depends on: [[db]] 
    | Parameter   | Description |
    | ------------|-------------|
    | `kvs-db`    | A reference to the `clj.intracel.api.protocols/KVStoreDbiApi` created in the [[db]] function. |
    | `pre-fn`    | A function that accepts a single `key` arg and returns a new `key` that will get serialized by the [[clj.intracel.api.kv-store/KVSerde]] used in the [[kv-get]] function. ||

    Returns:
    A `clj.intracel.api.protocols/KVStoreDbiApi` that can be used in a builder pattern.")
  (kv-get
    [kvs-db key]
    [kvs-db key key-serde val-serde]
    "Puts a value into the KV-Store. If a value the same key exists, it will be replaced with the value argument provided.
     The 2 parameter version of [[kv-get]] uses the default SerDe (see [[set-key-serde]], [[set-val-serde]]).
     The 4 parameter version of [[kv-get]] allows the caller to supply a specific `clj.intracel.api.kv-store/KVSerde` to serialize the key properly on look-up and deserialize the key and value properly on retrieval.

    Depends on: [[db]] 
    | Parameter   | Description |
    | ------------|-------------|
    | `kvs-db`    | A reference to the `clj.intracel.api.protocols/KVStoreDbiApi` created in the [[db]] function. |
    | `key`       | Uses the default [[clj.intracel.api.kv-store/KVSerde]] to serialize the `key` to a [java.nio.ByteBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html). |
    | `key-serde` | An implemenation of the [[clj.intracel.api.kv-store/KVSerde]]. If nil, defaults to a [[clj.intracel.serde.interface.string-serde]]. This overrides the `clj.intracel.api.kv-store/KVSerde` provided in [[set-key-serde]]. Not available in the 2 parameter version of this function.|
    | `val-serde` | An implemenation of the [[clj.intracel.api.kv-store/KVSerde]]. If nil, defaults to a [[clj.intracel.serde.interface.string-serde]]. This overrides the `clj.intracel.api-kv-store/KVSerde` provided in [[set-val-serde]]. Not available in the 2 parameter version of this function.||
    
    Returns:
    A map containing the key `written?` set to true or false. If false, use the `msg` key to check the error message.")
  
  (set-pre-del-hook [kvs-db pre-fn]
    "This enables the caller to customize the behavior performed when doing a key removal in [[kv-del]] by allowing caller code to pre-process the key.
    This could be useful for performing actions on related data before the key gets removed.. For example, a user may have related addresses in another 
    database that could be removed to perform proper clean-up.
    
    Depends on: [[db]] 
    | Parameter   | Description |
    | ------------|-------------|
    | `kvs-db`    | A reference to the `clj.intracel.api.protocols/KVStoreDbiApi` created in the [[db]] function. |
    | `pre-fn`    | A function that accepts a single `key` arg and returns the same `key` that will get serialized by the [[clj.intracel.api.kv-store/KVSerde]] used in the [[kv-del]] function. ||

    Returns:
    A `clj.intracel.api.protocols/KVStoreDbiApi` that can be used in a builder pattern.")
  
  (kv-del
    [kvs-db key]
    [kvs-db key key-serde]
    "Removes a key and its corresponding value from the KV-Store using the provided KVSerde. 
     The 2 parameter version of [[kv-del]] uses the default SerDe (see [[set-key-serde]], [[set-val-serde]]).
     The 3 parameter version of [[kv-del]] allows the caller to supply a specific `clj.intracel.api.kv-store/KVSerde` to serialize the key properly on look-up.
  
    Depends on: [[db]] 
    | Parameter   | Description |
    | ------------|-------------|
    | `kvs-db`    | A reference to the `clj.intracel.api.protocols/KVStoreDbiApi` created in the [[db]] function. |
    | `key`       | Uses the default [[clj.intracel.api.kv-store/KVSerde]] to serialize the `key` to a [java.nio.ByteBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html). |
    | `key-serde` | An implemenation of the [[clj.intracel.api.kv-store/KVSerde]]. If nil, defaults to a [[clj.intracel.serde.interface.string-serde]]. This overrides the `clj.intracel.api.kv-store/KVSerde` provided in [[set-key-serde]]||
    Returns:
    A map containing the key `deleted?` set to true or false. If false, use the `msg` key to check the error message."))

;; The next.jdbc library from Sean Corefield supports the DuckDB driver.
;; The library puts the driver into a connection pool which is perfect 
;; for most cases. 
;; However, if you want to bulk load data, DuckDB has a special class 
;; called an Appender which is reallh efficient but requires the DuckDB
;; specific driver. 
;; We'll used the SQLStoreContext for DuckDB to hold a connection pool 
;; and a reference to a second connection with the DuckDB driver so that 
;; both are available. The `ctx` var will be used as a map in that case
;; so each can be accessed easily when needed.
(defrecord SQLStoreContext [ctx]
  Closeable
  (close [_]
    (when (some? (:pool ctx))
      (.close (:pool ctx)))
    (when (some? (:appender-conn ctx))
      (.close (:appender-conn ctx)))))

(defprotocol SQLStoreDbContextApi 
  (create-sql-db [sql-db-ctx]
    "Constructor function that produces a database specific record object containing the 
     [[clj.intracel.api.interface.protocols/SQLStoreContext]] reference in a field called `sql-ctx`. 
     This constructor enables multiple implementations for different embedded SQL-Stores.
     
     Depends on:  
     | Parameter    | Description |
     | -------------|-------------|
     | `sql-db-ctx` | A reference to the `clj.intracel.api.interface.protocols/SQLStoreDbContextApi` created at initialization. ||
     Returns:
     A record that implements the [[clj.intracel.api.interface.protocols/SQLStoreApi]]"))

(defprotocol SQLStoreApi 
  (bulk-load [sql-db table-name rows]
    "Bulk loads rows into the SQL-Store. The sql-db object should be a record implementing [[clj.intracel.api.interface.protocols/SQLStoreApi]] and containing a field called 
    `sql-ctx` containing a reference to a [[clj.intracel.api.interface.protocols/SQLStoreContext]]. 
     Call the `create-sql-db` to create this reference before calling this function.
      
    Depends on: [[sql-db]] 
    | Parameter    | Description |
    | -------------|-------------|
    | `sql-db`     | A reference to the `clj.intracel.api.interface.protocols/SQLStoreApi` created record. |
    | `table-name` | The name of the table to bulk load. |
    | `rows`       | A vector of vectors containing values for the columns to insert into the table. These must be in the same order of the columns as defined in the table's schema definition.||
    Returns:
    A map containing the key `loaded?` set to true or false. If false, use the `msg` key to check the error message."))