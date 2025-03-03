(ns build 
  (:require [clojure.tools.build.api :as b]))

(def build-folder "target")

(defn clean [_]
  (b/delete {:path build-folder})
  (prn (format "Build folder %s cleaned" build-folder)))