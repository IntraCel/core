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

(defprotocol KVStoreDb 
  (start [ ]))