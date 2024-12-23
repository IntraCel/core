[Getting Started](#getting-started)

# The Data Problem You Knew Was Coming
### Shortcuts and Technical Debt
Most greenfield projects never start out with the intent to include bad design. Experienced engineers have already spent way too much time dealing with the problems left behind by others. This time, it's going to be different! 
Often, however, what starts out as a small microservice working with occasional updates, quickly morphs into a data ingesting monster in a matter of months. No one wants to create the monster.
But time pressure is a huge part of business. Sometimes the deadline just can't be moved. When that happens, the engineer is forced to make compromises. If there were more time, maybe there could have been some research spikes done to figure out the best database to use. Data streaming is clearly becoming a standard but it can take time to build the right design from the ground up to support it. But shortcuts had to be taken, and maybe some tests had to be skipped. 
All of this adds up to technical debt. Tech debt is just like credit, buy now, pay later. Sometimes it can be ignored, but eventually, it starts to slow the development process down. Bugs start to take over the whole release cycle.Before you know it, there's almost no time for new features. In an article written by [Sonar](https://www.sonarsource.com/blog/new-research-from-sonar-on-cost-of-technical-debt/), a study made on 200 different projects revealed the annual cost for a project with one million lines of code (LoC) is $306,000. That is the equivalent of 5,500 hours of engineering time.

### Data Explosion
With the advent of Big Data and AI, our world is experiencing an explosion of data. In an article published by [G2](https://www.g2.com/articles/big-data-statistics), it is estimated that in 2025, the world will produce 463 ZB (Zeta Bytes, that's 463,000,000,000,000,000,000,000 [21 zeros] Bytes) every day at a worth of over $220 billion. 

Every 60 seconds .. 
![The Data Explosion](docs/images/Internet-in-60-Seconds.drawio.png "The Data Explosion")

The average company now wrangles with _400_ different data sources!


## Who is IntraCel For?
In the end, software is really about helping people solve real-world problems. 
IntraCel is a Clojure library that's meant to help engineers have powerful functional tools to make managing those problems easier. Clojure is functional, flexible, and fast and fits most software needs like a glove. 

The development team behind IntraCel has been developing in Clojure for almost a decade and wants to share the love. That's why the heart of IntraCel will always remain open source. We want the Clojure community to expand. Others need to see what they've been missing! 

IntraCel is an approachable library that encourages good design and lets you, the architect, keep your options open so that you can have the flexibility you need to grow your tech stack without the library getting in the way.

## All Design Decisions Come with a Cost
[![Reducing The Cost of Being Wrong](https://img.youtube.com/vi/RHbZk4qGazE/0.jpg)](https://www.youtube.com/watch?v=RHbZk4qGazE)

### Red Pill or Blue Pill?
In the movie, The Matrix, Neo was given a choice. The blue pill would let him go back to his regular life like nothing was wrong. The red pill would show him the truth he wouldn't be able to unsee.
Unfortunately, for software engineers building modern software, the blue pill means just avoiding the inevitable deluge of data. Software that isn't designed to support data-intensive tasks will have to deal with redesigning architecture **_AFTER_** it's been released. That's a one way ticket to pain!

Kent Beck, one of the creators of Agile, explains that the initial investment in building software pales in comparison to the cost of maintaining it. He explains that the cost to maintain it is directly related to the cost of coupling in the system. That is when a change to one component cascades a change to another component, which would cascade to another component, and so on. 

IntraCel engineers have taken the red pill. The concepts built into the library believe that good software design embraces decoupling wherever possible. While it comes with out-of-the-box capabilities built on embedded systems, it uses protocols and interfaces to decouple design from implementations. This makes it simpler to change over time and encourages good practices. IntraCel leverages Clojure's innate capabilities of concurrency and unmatched processing power to do data-intensive tasks quickly and reliably.

# Getting Started
## Latest Releases

## Setup

Add the [relevant dependency](#latest-releases) to your project:

```clojure
Leiningen: [clj.intracel/core               "x-y-z"] ; or
deps.edn:   clj.intracel/core {:mvn/version "x-y-z"}
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

## Create the KVStoreContext
The KV-Store is capable of hosting multiple embedded database instances in the process. To do this, first create the context that will host them.

```clojure 
(require '[clj.intracel.kv-store.interface :as kv-store])

(with-open [kvs-ctx (kv-store/create-kv-store-context {:intracel.kv-store/type :lmdb
                                                         :intracel.kv-store.lmdb/storage-path (str (System/getProperty "java.io.tmpdir") "/lmdb/")})]
    ;;Start using your context here
	)
```
1. Require the ```clj.intracel.kv-store.interface``` namespace. IntraCel is built in the popular polylith style monorepo so you'll see naming conventions that come from that architecture here (e.g. - API's within an interface namespace). The public interface to interact with the ```KVStoreContext``` resides here.
   API definitions and common data types can be found in the namespace: ```clj.intracel.api.interface.protocols```. The definition for the ```KVStoreContext``` can be found in: ```clj.intracel.api.interface/KVStoreContext```. It's basically a ```defrecord``` that implements Java's ```Closeable``` interface and contains the instance of hosting environment to spawn multiple database instances. 

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

## Create A Database Instance
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
3. We'll create a reference to the ```KVStoreContext``` again and bind it to  ```kvs-db-ctx``` in our let block. 
4. Next, we'll create a reference to a ```clj.intracel.api.interface.protocols/KVStoreDbContextApi```. This is a container that is used to generate database instances. In the context of IntraCel's kv-store, database instances could be considered like tables in a SQL database. They should have the same key type in order to perform the same operations on them.
This ```KVStoreDbContextApi``` instance is purpose built to support database instances that run on LMDB and accepts the reference to the ```KVStoreContext``` which it will use to help it generate database instances properly. 
We'll bind the instance to the ```kvs-db-ctx``` variable.
5. We'll create a database instance by calling teh ```kv-store/db``` function, using the reference to our ```KVStoreDbContextApi```. The second parameter is the name to give the database instance (```sg-1```). The third parameter gives the caller the ability to customize some of the behind-the-scenes implementation of the database instance. 
Under the hood, the database instance utilizes a core.async channel to ensure that a single thread is in charge of delivering writes. This is to ensure consistency as LMDB utilizes ACID transactions. The ```:ic-chan-opts/buf-size``` key lets the caller adjust the size of the buffered channel that receives data being written. Alternatively, the caller could provide its own channel implementation by using the ```:ic-chan-opts/replacement-chan``` key instead.
The final parameter is a vector containing start-up options for the database instance for LMDB. In this case, the ```:ic-db-flags/create-db-if-not-exists``` will start a new database file on the filesystem if this is the first time the database with the name provided is being used. 

## Write to a Database Instance - Sync
```clojure
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
4. Once data is written to the database instance, it can be retrieved using the ```kv-store/kv-get``` function. This function also takes the database instance as the first parameter. Its second is the key the caller is looking for in the database (```general```). Like its sibling, the ```kb-get``` function is also multi-arity and allows the caller to customize the SerDes it uses on both the key and the value. When not provided, it also defaults to a ```clj.intracel.serde.string-serde```.
<!--## Good Design Is About Planning Ahead for Unavoidable Growth

# IntraCel Arms You With Years of Architectural Experience

## IntraCel Prepares You For the Data Explosion

## IntraCel Is Built By Engineers Who Care About Cost -->


