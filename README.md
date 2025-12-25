# IntraCel - Your Embedded Data Store Keeper
[Getting Started](#getting-started)


# Getting Started
## Latest Releases

## Setup

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.intracel-admin/intracel-core.svg)](https://clojars.org/org.clojars.intracel-admin/intracel-core)

Add the [relevant dependency](#latest-releases) to your project:

```clojure
Leiningen: [org.clojars.intracel-admin/core               "0.1.104"] ; or
deps.edn:   org.clojars.intracel-admin/core {:mvn/version "0.1.104"}
```

## JVM Settings
IntraCel has a Key/Value database called the KV-Store that runs on [LMDB](https://en.wikipedia.org/wiki/Lightning_Memory-Mapped_Database), an embedded database that uses memory mapping techiques to treat the computer's memory as a single address space that can be shared across multiple processes or threads while keeping a small footprint on the system. It's initial deployment was used in OpenLDAP, but its effecient design and low touch configuration made it a fit for many use cases. In fact LMDB can be found running in one of the most popular databases in the world where it was used to make the in-memory store Redis persist data on disk.

IntraCel works on top of the [lmdbjava](https://github.com/lmdbjava/lmdbjava) project, utilizing a well-built JNI integration with LMDB's C API. Since LMDB is an embedded database written in native code, the JVM_OPTS environment variable needs to be adjusted to support LMDB's memory mapping. A snippet is provided showing the proper settings.

### Export with Environment Variable

```bash
export JVM_OPTS="--add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED"
```

### Source Environment Variable with Shell Script
Within the root of the core project, there is a shell script called ```set-jvm-opts.sh``` that can be invoked from the shell when running tests just using the ```source``` command on *NIX based systems as seen in this example:
```bash
source ./set-jvm-opts.sh
```

### Set Environment Variable in Calva
For those who use Calva and VS Code and want to run IntraCel with LMDB, there is an example of how to get that setting to work in the ```core.code-workspace``` file that can ensure that your REPL will work properly:
```JSON
"calva.jackInEnv": {
	"JAVA_OPTS": "--add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED"
}
```

## KV-Store: Create the KVStoreContext
The KV-Store is an embedded Key/Value database that runs in the application's process. The KV-Store can host multiple database instances at a time. To use it, first create the context that will host them.

```clojure 
(require '[clj.intracel.kv-store.interface :as kv-store])

(with-open [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    ;;Start using your context here
	)
```
1. Require the ```clj.intracel.kv-store.interface``` namespace. IntraCel is built in the popular polylith style monorepo so you'll see naming conventions that come from that architecture here (e.g. - API's within an interface namespace). The public interface to interact with the ```KVStoreContext``` resides here.
   API definitions and common data types can be found in the namespace: ```clj.intracel.api.interface.protocols```. The definition for the ```KVStoreContext``` can be found in: ```clj.intracel.api.interface.protocols/KVStoreContext```. It's basically a ```defrecord``` that implements Java's ```Closeable``` interface and contains the instance of hosting environment to spawn multiple database instances. 

2. Since ```KVStoreContext``` implements ```Closeable```, it can be used inside a ```(with-open [])``` block to allow it to be closed automatically when it goes out of scope. 

3. The ```clj.intracel.kv-store.interface``` namespace in the example is given the alias ```kv-store``` here. We'll use this to keep things simpler. The ```kv-store``` interface provides developers with a constructor function to create the ```KVStoreContext``` with the ```(create-kv-store-context)``` function. This accepts a single parameter map called ```ctx-opts```. At present, the ```KVStoreContext``` has a single implementation built on LMDB so the first key in the map ```:intracel.kv-store/type``` is set to ```:lmdb```. The second key in the map ```:intracel.kv-store.lmdb/storage-path``` configures the ```KVStoreContext``` to know where to persist data to disk for database instances. In the example here, it's just using the default location for the temp directory on the operating system.

### Alternate Way to Create the KVStoreContext with Component
```clojure
(require '[com.stuarsierra.component :as component])
(require '[clj.intracel.kv-store.interfce :as kv-store])

(defrecord MyCtx [kvs-ctx]
    component/Lifecycle
    (start [this] this)
    (stop [this] 
        (when (some? kvs-ctx)
            (.stop kvs-ctx))
        this))

(defn create-my-ctx []
    ;;Or create a system and inject kvs-ctx into this component with (system-using)
    (map->MyCtx {:kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})})
)
```
Developers may want to keep the ```KVStoreContext``` in a long-lived application since it is strongly recommended only have one per JVM process. Instead of running it inside of a with block, it could easily be used in something like [a stateful component](https://github.com/stuartsierra/component) where the ```(.close)``` function could be called as the component's ```(stop [])``` function gets automatically invoked [see the Lifecycle protocol for more details](https://github.com/stuartsierra/component/blob/master/src/com/stuartsierra/component.cljc#L5).

## KV-Store: Create A Database Instance
```clojure
(require '[clj.intracel.kv-store.interfce :as kv-store])
(require '[clj.intracel.serde.interface :as kv-serdes])

(with-open [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    (try (let [kvs-db-ctx (kv-store/create-kv-store-db-context kvs-ctx :lmdb)]
           (let [dbi (kv-store/db kvs-db-ctx "sg-1" {:ic-chan-opts/buf-size 100} [:ic-db-flags/create-db-if-not-exists])]
             ;;Do interesting things here with your database instance
             ))
         (catch Exception e
           (prn "Error in test-kv-put: " (.getMessage e))
           (doseq [tr (.getStackTrace e)]
             (prn "Trace: " tr)))))
```

1. Like in previous examples, we'll alias the ```kv-store```. 
2. We're going to bring in a new namespace now. The ```kv-store``` only works with raw ```java.nio.ByteBuffer```s for its keys and values. The ```clj.intracel.serde.interface``` namespace contains Serializers and Deserializers (called SerDes) to move data in and out of the kv-store using Clojure data structures.
3. We'll create a reference to the ```KVStoreDBContext``` and bind it to  ```kvs-db-ctx``` in our let block. This is used to contruct database instances (dbi's).
4. Next, we'll create a reference to a ```clj.intracel.api.interface.protocols/KVStoreDbContextApi```. This is a container that is used to generate database instances. In the context of IntraCel's kv-store, database instances could be considered like tables in a SQL database. They should have the same key type in order to perform the same operations on them.
This ```KVStoreDbContextApi``` instance is purpose built to support database instances that run on LMDB and accepts the reference to the ```KVStoreDBContext``` which it will use to help it generate database instances properly. 
We'll bind the instance to the ```kvs-db-ctx``` variable.
5. We'll create a database instance by calling the ```kv-store/db``` function, using the reference to our ```KVStoreDbContextApi```. The second parameter is the name to give the database instance (```sg-1```). The third parameter gives the caller the ability to customize some of the behind-the-scenes implementation of the database instance. 
Under the hood, the database instance utilizes a core.async channel to ensure that a single thread is in charge of delivering writes. This is to ensure consistency as LMDB utilizes ACID transactions. The ```:ic-chan-opts/buf-size``` key lets the caller adjust the size of the buffered channel that receives data being written. Alternatively, the caller could provide its own channel implementation by using the ```:ic-chan-opts/replacement-chan``` key instead.
The final parameter is a vector containing start-up options for the database instance for LMDB. In this case, the ```:ic-db-flags/create-db-if-not-exists``` will start a new database file on the filesystem if this is the first time the database with the name provided is being used. 

## KV-Store: Write to a Database Instance - Sync
```clojure
(require '[clj.intracel.kv-store.interfce :as kv-store])

(with-open [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    (try (let [kvs-db-ctx (kv-store/create-kv-store-db-context kvs-ctx :lmdb)]
           (let [dbi (kv-store/db kvs-db-ctx "sg-1" {:ic-chan-opts/buf-size 100} [:ic-db-flags/create-db-if-not-exists])]
             (kv-store/kv-put dbi "general" "Jack O'Neil")
             (kv-store/kv-put dbi "doctor" "Daniel Jackson")
             (kv-store/kv-put dbi "major" "Samantha Carter")
             (kv-store/kv-put dbi "jafa" "Teal'c")
             (let [general (kv-store/kv-get dbi "general")]
               (when (= "Jack O'Neil" general)
                (prn "We found the general!")))))
         (catch Exception e
           (prn "Error in test-kv-put: " (.getMessage e))
           (doseq [tr (.getStackTrace e)]
             (prn "Trace: " tr)))))
```
1. We'll skip through the setup of the database instance as that is described in [Create A Database Instance](#create-a-database-instance). 
2. Focusing on the let block where we've created the binding to a ```dbi``` variable, we'll spend some time here.
The database instance implements the ```clj.intracel.api.interface.protocols/KVStoreDbiApi``` which supports both synchronous and asynchronous API calls. In this example we'll focus on the synchronous API for writes. 
3. With our ```sg-1``` database instance ready to go, let's put some entries into it by calling the ```kv-store/kv-put``` function. This function is passed the database instance reference as its first parameter. The next two parameters are a key and a value, much like doing a put operation on a traditional HashMap.
The ```kv-store/kv-put``` function is multi-arity and allows for the caller to customize the SerDes used for the key and the value. In this example, we're using the most basic arity which just defaults to a ```clj.intracel.serde.string-serde```.
For the curious, each of these synchronous calls put the key/value pairs provided onto the core.async channel referred to earlier within the context of a ```go``` block. The function blocks on a one-shot core.async channel waiting to hear an acknowledgment that the put completed. A snippet of that implementation on LMDB is provided for reference. 
```clojure 
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
```
4. Once data is written to the database instance, it can be retrieved using the ```kv-store/kv-get``` function. This function also takes the database instance as the first parameter. Its second is the key the caller is looking for in the database (```general```). Like its sibling, the ```kv-get``` function is also multi-arity and allows the caller to customize the SerDes it uses on both the key and the value. When not provided, it also defaults to a ```clj.intracel.serde.string-serde```.

## KV-Store: Write to a Database Instance - Async
The async functions on database instances can be really useful in data-intensive applications. They allow the caller to let Clojure schedule writes in a thread pool without waiting for each one to complete. 
```clojure
(require '[clj.intracel.kv-store.interfce :as kv-store])
(require '[clj.intracel.serde.interface :as kv-serdes])

(with-open [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    (try (let [kvs-db-ctx (kv-store/create-kv-store-db-context kvs-ctx :lmdb)
               ks         (kv-serdes/string-serde)
               vs         (kv-serdes/nippy-serde)]
           (let [dbi (kv-store/db kvs-db-ctx "sg-1" {:ic-chan-opts/buf-size 100} [:ic-db-flags/create-db-if-not-exists])]
             (let [async-chans [(kv-store/kv-put-async dbi "general" "Jack O'Neil" ks vs)
                                (kv-store/kv-put-async dbi "doctor" "Daniel Jackson" ks vs)
                                (kv-store/kv-put-async dbi "major" "Samantha Carter" ks vs)
                                (kv-store/kv-put-async dbi "jafa" "Teal'c" ks vs)]
                   responses   (reduce (fn [acc chan]
                                         (let [answer (<!! chan)]
                                           (conj acc answer)))
                                       []
                                       async-chans)]
               (let [keys-written (into (sorted-set) (mapv #(:key %) responses))
                     general (kv-store/kv-get dbi "general")]
                 (is (= (sorted-set "doctor" "general" "jafa" "major") keys-written))
                 (is (= "Jack O'Neil" general))))))
         (catch Exception e
           (prn "Error in test-kv-put: " (.getMessage e))
           (doseq [tr (.getStackTrace e)]
             (prn "Trace: " tr)))))
```
1. We'll skip through the setup of the database instance as that is described in [Create A Database Instance](#create-a-database-instance). 
2. In the let block where the ```kvs-db-ctx``` binding has been set up, we now have two SerDe instances we're using for Strings and Nippy encoded data.  
3. With our ```sg-1``` database instance ready to go, let's put some entries into it by calling the ```kv-store/kv-put-async``` function. This function is passed the database instance reference as its first parameter. The next two parameters are a key and a value, much like doing a put operation on a traditional HashMap.
This example uses the multi-arity form that allows for the caller to customize the SerDes used for the key and the value. By using the ```clj.intracel.serde.nippy-serde``` the caller can encode any EDN formatted data as the value.
For the curious, each of these asynchronous calls put the key/value pairs provided onto the core.async channel referred to earlier within the context of a ```go``` block. The function passes in a one-shot core.async channel to the consumer so that when the write completes, it can put it's response back on the one-shot channel. This channel gets returned immediately from the function and allows the core.async library to schedule execution of the go block on its thread pool. 
```clojure 
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
```
4. Since the async put functions have returned channels, we'll reduce on them and block on each one-shot response channel to make sure everything has been written first.
5. Once data is written to the database instance, it can be retrieved using the ```kv-store/kv-get``` function.  This function also takes the database instance as the first parameter. Its second is the key the caller is looking for in the database (```general```). Like its sibling, the ```kv-get``` function is also multi-arity and allows the caller to customize the SerDes it uses on both the key and the value. When not provided, it also defaults to a ```clj.intracel.serde.string-serde```.

## SQL-Store: Create the SQLStoreContext
The SQL-Store is an embedded SQL database which gives developers the ability to maintain multiple databases and tables. The ```clj.intracel.api.protocols.SQLStoreContext``` is the jumping off point to getting a database set up. 
```clojure
(require '[clj.intracel.sql-store.interface :as sql-store])

(with-open [sql-ctx (sql-store/create-sql-store-context 
                      {:intracel.sql-store/type :duckdb 
                       :intracel.sql-store.duckdb/storage-path (str (System/getProperty "java.io.tmpdir") "/duckdb")})]
      ;;Start using your context here
      )
```
1. Require in the `clj.intracel.sql-store.interface` namespace. We'll use `sql-store` to alias it.
2. Since ```SQLStoreContext``` implements ```Closeable```, it can be used inside a ```(with-open [])``` block to allow it to be closed automatically when it goes out of scope.
3. Create the ```SQLStoreContext``` by calling ```(sql-store/create-sql-store-context)```. This function expects the ctx-opts argument to be a map with specific keys.
4. The first key in the example is`:intracel.sql-store/type`. This tells intracel what type of embedded database to start up. Currently, the library supports a single database type of `:duckdb`. With its current set of interfaces, other embedded databases can easily be added.
5. The second key in the xample is the `intracel.sql-store.duckdb/storage-path`. This key is namespaced as a DuckDB specific setting for where the embedded database will persist changes.  

## SQL-Store: Create a Connection to the Database
Once a ```SQLStoreContext``` has been created, we'll use a database specific instance of the ```clj.intracel.api.interface.protocols.SQLStoreDbContextApi```. That's a moutful, but it's bascially an interface into a factory that can create connections to your database. The following example shows how to get a connection to DuckDB.
```clojure
(require '[clj.intracel.sql-store.interface :as sql-store])
(require '[next.jdbc :as jdbc])

(with-open [sql-ctx (sql-store/create-sql-store-context {:intracel.sql-store/type :duckdb
                                                           :intracel.sql-store.duckdb/storage-path (str (System/getProperty "java.io.tmpdir") "/duckdb")})]
    (is (not (nil? sql-ctx)))
    (try (let [sql-store-db-ctx (sql-store/create-sql-store-db-context sql-ctx :duckdb)]
           (let [db            (sql-store/db sql-store-db-ctx)
                 pool-ds       (get-in db [:sql-ctx :ctx :pool])
                 appender-conn (get-in db [:sql-ctx :ctx :appender-conn])]
                 (prn "db:" db)
                 (prn "schemas:")
                 (try 
                 (let [schemas (jdbc/execute! pool-ds ["SELECT schema_name FROM information_schema.schemata"])] 
                 (prn schemas))
                 (catch Exception ex 
                 (prn "failed to list schemas:" (.getMessage ex))))

                 (jdbc/execute! appender-conn
                     ["CREATE SCHEMA IF NOT EXISTS intracel"])

                 (jdbc/execute! appender-conn ["USE intracel"])
      
                 (prn "Tables in schema intracel:")
                 (let [tables (jdbc/execute! pool-ds ["SELECT table_name FROM information_schema.tables where table_schema = 'intracel'"])]
                 (prn tables))

                 (jdbc/execute! appender-conn 
                 ["CREATE OR REPLACE TABLE movies (title VARCHAR, year INT, rotten_tomatoes_score FLOAT)"]) 
                 (let [results (sql-store/bulk-load db "intracel.movies" 
                                [["Star Wars Episode V: The Empire Strikes Back" 1977 93.0]
                                  ["Ghostbusters" 1984 95.0]
                                  ["Inception" 2010 87.0]])]
                                  (prn "results:" results))
                                  (when (:loaded? results)
          (let [ghostbusters (jdbc/execute! appender-conn ["SELECT * FROM intracel.movies_villains WHERE title = 'Ghostbusters'"])]
            (prn "Favorite Ghost Action Movie: " ghostbuster)))
                  )
         (catch Exception e
           (prn "Error in Test: " (.getMessage e))
           (doseq [tr (.getStackTrace e)]
             (prn "Trace: " tr)))))
```
1. As in the previous example, we'll require the SQL Store interface and alias it with ```sql-store```.
2. We'll also bring in the excellent next.jdbc library by Sean Corfield. For instructions on how to set that up in your project see [documentation here](https://cljdoc.org/d/com.github.seancorfield/next.jdbc/1.3.994/doc/getting-started).
3. As in the previous example, we'll create a reference to our ```SQLStoreContext```.
4. Next, we'll bind an DuckDB specific instance of the ```SQLStoreDbContextApi``` to the the var ```sql-store-db-context```. This is our factory for producing container objects with connections to the database.
5. We'll pass the factory in the ```(sql-store/db)``` function to produce a binding to the ```db``` var. This contains our connection(s) to DuckDB. 
6. In the DuckDB implementation, there are actually two different connection objects to work with in the object.
7. The first is a next.jdbc pooled connection that is generally intended for performing most operations on the database, particularly queries. This will dole out preset JDBC compatible connections that can be used in threads to query tables. In the example, this is bound to the ```pool-ds``` (Pooled DataSource) by extracting the ```:pool``` key from the ```db``` var.
8. The second is a DuckDB specific connection of type ```org.duckdb.DuckDBConnection```. This is meant to allow the caller to utilize APIs specifically meant for efficient bulk loading into the database. In the example,  this is bound to the ```appender-conn``` var by extracting the ```:appender-conn``` key from the ```db``` var.

<!--## Good Design Is About Planning Ahead for Unavoidable Growth

# IntraCel Arms You With Years of Architectural Experience

## IntraCel Prepares You For the Data Explosion

## IntraCel Is Built By Engineers Who Care About Cost -->


