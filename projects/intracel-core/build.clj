(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'com.github.intracel/intracel-core)
(def version (format "0.1.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))



;; delay to defer side effects (artifact downloads)
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"})
  (prn "Cleaned projects/intracel-core/target"))

(defn jar [_]
  (clean nil)
  
  (b/copy-dir {:src-dirs ["src" "resources" "components" "bases"]
               :target-dir class-dir})
  (b/compile-clj {:basis @basis
                  :src-dirs ["src" "resources" "components" "bases"]
                  :class-dir class-dir})

  
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis @basis
                :src-dirs ["src"]})
  
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn uberjar [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "components" "bases"]
               :target-dir class-dir})
  (b/compile-clj {:basis @basis
                  :src-dirs ["src" "components" "bases"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis @basis
           ;;:main 'your-main.namespace
           }))
