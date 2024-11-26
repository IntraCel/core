(ns clj.intracel.api.kv-store
  "The clj.intracel.api.kv-store namespace defines the types and behaviors of the IntraCel KV-Store 
   for managing Key/Value pairs in a high-speed embedded database.  The KV-Store has operations 
   similar to a map with get and put functions. However, the KV-Store is persistent and can survive
   a restart of the application.
   The KV-Store utilizes Serializers and Deserializers (or SerDes for short) to process both Keys 
   and Values. Since the KV-Store mostly works with `java.nio.ByteBuffer`s to achieve high performance 
   it's necessary to convert the data into Clojure recognizable data structures.
   See the [[clj.intracel.api.serde]] namespace on how to perform serialization (for writes) and 
   deserialization (for reads).
   
   The namespace is broken out into two main parts: state and behaviors.
   State is maintained in the `KVStoreContext` defrecord while behavior is defined in the protocol 
   `KVStoreDb`.
   The `KVStoreContext` defrecord lets implementors of the API provide metadata in a map needed to start 
   up the KV-Store with the proper settings.
   The `KVStoreDb defines what actions the KV-Store can perform.`")

(defrecord KVStoreContext [ctx])

(defrecord KVStoreDbContext [^KVStoreContext kv-store-ctx])

(defprotocol KVSerde 
  (serialize [kv-serde item])
  (deserialize [kv-serde item]))

(defprotocol KVStoreDb
  (db [kvs-ctx db-name db-opts]
    "Returns a hosted embedded database. 
    
       | Parameter   | Description |
       | ------------|-------------|
       | `kvs-ctx`   | A reference to the `clj.intracel.api.kv-store/KVStoreContext` created at initialization. |
       | `db-name`   | The UTF-8 formatted name of the database to use. If this is the first time using it, the database instance will be created automatically. |
       | `db-opts`   | Map containing options to adjust the behavior of the database when initializing. 
       |             | * `:intracel/db-flags`: This tells the embedded database what options to use when creating 
       |             |                         the database. Multiple db-flags can be provided by passing in an array of the following: 
       |             |     * `:ic-db-flags/reverse-key`: Use reverse string keys. Keys are strings to be compared in reverse order, from the end of the  
       |             |                                   strings to the beginning. By default, keys are treated as strings and 
       |             |                                   compared from beginning to end.
       |             |                                   Correlates to [org.lmdbjava.DbiFlags/MDB_REVERSEKEY(0x02)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_REVERSEKEY), 
       |             |     * `:ic-db-flags/multi-value-keys`: Use sorted duplicates. 
       |             |                                    Duplicate keys may be used in the database. Or, from another perspective, 
       |             |                                    keys may have multiple data items, stored in sorted order. By default keys 
       |             |                                    must be unique and may have only a single data item.
       |             |                                    Correlates to [org.lmdbjava.DbiFlags/MDB_DUPSORT(0x04)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_DUPSORT), 
       |             |     * `:ic-db-flags/integer-keys`: Numeric keys in native byte order: either unsigned int or size_t. The keys 
       |             |                                    must all be of the same size.
       |             |                                    Correlates to [org.lmdbjava.DbiFlags/MDB_INTEGERKEY(0x08)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_INTEGERKEY), 
       |             |     * `:ic-db-flags/sort-fixed-sized-duplicate-items`: With {@link #MDB_DUPSORT}, sorted dup items have fixed size. 
       |             |                                                        This flag may only be used in combination with {@link #MDB_DUPSORT}. This 
       |             |                                                        option tells the library that the data items for this database are all the
       |             |                                                        same size, which allows further optimizations in storage and retrieval. 
       |             |                                                        When all data items are the same size, the {@link SeekOp#MDB_GET_MULTIPLE} 
       |             |                                                        and {@link SeekOp#MDB_NEXT_MULTIPLE} cursor operations may be used to 
       |             |                                                        retrieve multiple items at once. 
       |             |                                                        Correlates to [org.lmdbjava.DbiFlags/MDB_DUPFIXED(0x10)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_DUPFIXED), 
       |             |     * `:ic-db-flags/duplicates-are-binary-integers`: With {@link #MDB_DUPSORT}, dups are {@link #MDB_INTEGERKEY}-style integers. 
       |             |                                                      This option specifies that duplicate data items are binary integers, 
       |             |                                                      similar to {@link #MDB_INTEGERKEY} keys. 
       |             |                                                      Correlates to [org.lmdbjava.DbiFlags/MDB_INTEGERDUP(0x20)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_INTEGERDUP), 
       |             |     * `:ic-db-flags/compare-duplicates-as-reverse-strings`: With {@link #MDB_DUPSORT}, use reverse string dups. 
       |             |                                                             This option specifies that duplicate data items should be compared as 
       |             |                                                             strings in reverse order. 
       |             |                                                             Correlates to [org.lmdbjava.DbiFlags/MDB_REVERSEDUP(0x40)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_REVERSEDUP), 
       |             |     * `:ic-db-flags/create-db-if-not-exists`: Create the named database if it doesn't exist. 
       |             |                                               This option is not allowed in a read-only transaction or a read-only |
       |             |                                               environment. 
       |             |                                               Correlates to [org.lmdbjava.DbiFlagsMDB_CREATE(0x4_0000)(https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_CREATE)|
       Returns:
       A `clj.intracel.api.kv-store/KVStoreDbContext`")
  (set-key-serde [kvs-db key-serde]
    "Sets the key SerDe used for serializing to and deserializing from a [[clj.intracel.api.kv-store/KVStoreDbContext]]
                  
       | Parameter   | Description |
       | ------------|-------------|
       | `kvs-db`    | A reference to the `clj.intracel.api.kv-store/KVStoreDbContext` created in the `start` function. |
       | `key-serde` | The UTF-8 formatted name of the database to use. If this is the first time using it, the database instance will be created automatically. |
       | `db-opts`   | Map containing options to adjust the behavior of the database when initializing. 
       |             | * `:intracel/db-flags`: This tells the embedded database what options to use when creating 
       |             |                         the database. Multiple db-flags can be provided by passing in an array of the following: 
       |             |     * `:ic-db-flags/reverse-key`: Use reverse string keys. Keys are strings to be compared in reverse order, from the end of the  
       |             |                                   strings to the beginning. By default, keys are treated as strings and 
       |             |                                   compared from beginning to end.
       |             |                                   Correlates to [org.lmdbjava.DbiFlags/MDB_REVERSEKEY(0x02)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_REVERSEKEY), 
       |             |     * `:ic-db-flags/multi-value-keys`: Use sorted duplicates. 
       |             |                                    Duplicate keys may be used in the database. Or, from another perspective, 
       |             |                                    keys may have multiple data items, stored in sorted order. By default keys 
       |             |                                    must be unique and may have only a single data item.
       |             |                                    Correlates to [org.lmdbjava.DbiFlags/MDB_DUPSORT(0x04)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_DUPSORT), 
       |             |     * `:ic-db-flags/integer-keys`: Numeric keys in native byte order: either unsigned int or size_t. The keys 
       |             |                                    must all be of the same size.
       |             |                                    Correlates to [org.lmdbjava.DbiFlags/MDB_INTEGERKEY(0x08)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_INTEGERKEY), 
       |             |     * `:ic-db-flags/sort-fixed-sized-duplicate-items`: With {@link #MDB_DUPSORT}, sorted dup items have fixed size. 
       |             |                                                        This flag may only be used in combination with {@link #MDB_DUPSORT}. This 
       |             |                                                        option tells the library that the data items for this database are all the
       |             |                                                        same size, which allows further optimizations in storage and retrieval. 
       |             |                                                        When all data items are the same size, the {@link SeekOp#MDB_GET_MULTIPLE} 
       |             |                                                        and {@link SeekOp#MDB_NEXT_MULTIPLE} cursor operations may be used to 
       |             |                                                        retrieve multiple items at once. 
       |             |                                                        Correlates to [org.lmdbjava.DbiFlags/MDB_DUPFIXED(0x10)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_DUPFIXED), 
       |             |     * `:ic-db-flags/duplicates-are-binary-integers`: With {@link #MDB_DUPSORT}, dups are {@link #MDB_INTEGERKEY}-style integers. 
       |             |                                                      This option specifies that duplicate data items are binary integers, 
       |             |                                                      similar to {@link #MDB_INTEGERKEY} keys. 
       |             |                                                      Correlates to [org.lmdbjava.DbiFlags/MDB_INTEGERDUP(0x20)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_INTEGERDUP), 
       |             |     * `:ic-db-flags/compare-duplicates-as-reverse-strings`: With {@link #MDB_DUPSORT}, use reverse string dups. 
       |             |                                                             This option specifies that duplicate data items should be compared as 
       |             |                                                             strings in reverse order. 
       |             |                                                             Correlates to [org.lmdbjava.DbiFlags/MDB_REVERSEDUP(0x40)](https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_REVERSEDUP), 
       |             |     * `:ic-db-flags/create-db-if-not-exists`: Create the named database if it doesn't exist. 
       |             |                                               This option is not allowed in a read-only transaction or a read-only |
       |             |                                               environment. 
       |             |                                               Correlates to [org.lmdbjava.DbiFlagsMDB_CREATE(0x4_0000)(https://www.javadoc.io/static/org.lmdbjava/lmdbjava/0.9.0/org/lmdbjava/DbiFlags.html#MDB_CREATE)|
       Returns:
       A `clj.intracel.api.kv-store/KVStoreDbContext`")
  (set-val-serde [kvs-db val-serde])
  (put [kvs-db key value])
  (set-pre-get-hook [kvs-db pre-fn])
  (get [kvs-db key])
  (exec-pre-get [kvs-db])
  (delete [kvs-db key]))