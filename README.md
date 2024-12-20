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
First, require the ```clj.intracel.kv-store.interface``` namespace. IntraCel is built in the popular polylith style monorepo so you'll see naming conventions that come from that architecture here (e.g. - API's within an interface namespace). The public interface to interact with the ```KVStoreContext``` resides here.

API definitions and common data types can be found in the namespace: ```clj.intracel.api.interface.protocols```. The definition for the ```KVStoreContext``` can be found in: ```clj.intracel.api.interface/KVStoreContext```. It's basically a ```defrecord``` that implements Java's ```Closeable``` interface and contains the instance of hosting environment to spawn multiple database instances. 

Since ```KVStoreContext``` implements ```Closeable```, it can be used inside a ```(with-open [])``` block to allow it to be closed automatically when it goes out of scope. Developers may want to keep the ```KVStoreContext``` in a long-lived application since it is strongly recommended only have one per JVM process. Instead of running it inside of a with block, it could easily be used in something like [a stateful component](https://github.com/stuartsierra/component) where the ```(.close)``` function could be called as the component's ```(stop [])``` function gets automatically invoked [see the Lifecycle protocol for more details](https://github.com/stuartsierra/component/blob/master/src/com/stuartsierra/component.cljc#L5).

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

The ```clj.intracel.kv-store.interface``` namespace in the example is given the alias ```kv-store``` here. We'll use this to keep things simpler. The ```kv-store``` interface provides developers with a constructor function to create the ```KVStoreContext``` with the ```(create-kv-store-context)``` function. This accepts a single parameter map called ```ctx-opts```. At present, the ```KVStoreContext``` has a single implementation built on LMDB so the first key in the map ```:intracel.kv-store/type``` is set to ```:lmdb```. The second key in the map ```:intracel.kv-store.lmdb/storage-path``` configures the ```KVStoreContext``` to know where to persist data to disk for database instances. In the example here, it's just using the default location for the temp directory on the operating system.




<!--## Good Design Is About Planning Ahead for Unavoidable Growth

# IntraCel Arms You With Years of Architectural Experience

## IntraCel Prepares You For the Data Explosion

## IntraCel Is Built By Engineers Who Care About Cost -->


