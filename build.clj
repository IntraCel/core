(ns build
  "IntraCel's build script liberally using Sean Corefield's HoneySQL build script and the clojure-polylith-realworld-exapmle-app.
   See https://github.com/seancorfield/honeysql/blob/develop/build.clj for the original.
   See https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app/blob/master/build.clj for the original.

  clojure -T:build uberjar :project intracel-core

  For more information, run:

  clojure -A:deps -T:build help/doc"
  (:refer-clojure :exclude [test])
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [clojure.tools.deps :as t]
            [clojure.tools.deps.util.dir :refer [with-dir]]
            [deps-deploy.deps-deploy :as dd] 
            [polylith.clj.core.api.interface :as api]
            [polylith.clj.core.git.interface :as git]
            [polylith.clj.core.version.interface :as version]
            ))

(def lib 'org.clojars.intracel-admin/intracel-core)
(defn- the-version [patch] (format "0.1.%s" patch))
(def version (or (System/getenv "VERSION") (format "0.1.%s" (b/git-count-revs nil))))
(def snapshot (the-version "9999-SNAPSHOT"))
(def class-dir "target/classes")

(defn- get-project-aliases []
  (let [edn-fn (juxt :root-edn :project-edn)]
    (-> (t/find-edn-maps)
        (edn-fn)
        (t/merge-edns)
        :aliases)))

(defn- ensure-project-root
  "Given a task name and a project name, ensure the project
  exists and seems valid, and return the absolute path to it."
  [task project]
  (let [project-root (str (System/getProperty "user.dir") "/projects/" project)]
    (when-not (and project
                   (.exists (io/file project-root))
                   (.exists (io/file (str project-root "/deps.edn"))))
      (throw (ex-info (str task " task requires a valid :project option") {:project project})))
    project-root))

(defn- lifted-basis
  "This creates a basis where source deps have their primary
   external dependencies lifted to the top-level, such as is
   needed by Polylith and possibly other monorepo setups."
  []
  (let [default-libs (:libs (b/create-basis))
        source-dep? #(not (:mvn/version (get default-libs %)))
        lifted-deps
        (reduce-kv (fn [deps lib {:keys [dependents] :as coords}]
                     (if (and (contains? coords :mvn/version) (some source-dep? dependents))
                       (assoc deps lib (select-keys coords [:mvn/version :exclusions]))
                       deps))
                   {}
                   default-libs)]
    (-> (b/create-basis {:extra {:deps lifted-deps}})
        (update :libs #(into {} (filter (comp :mvn/version val)) %)))))

(defn- latest-committed-sha
  "Get the latest committed SHA from current branch."
  []
  (let [branch (git/current-branch)]
    (git/latest-polylith-sha branch)))

(defn- pom-template [version]
  [[:description "IntraCel is your embedded data store keeper."]
   [:url "https://github.com/IntraCel/core"]
   [:licenses
    [:license
     [:name "Apache 2.0 Public License"]
     [:url "https://www.apache.org/licenses/LICENSE-2.0"]]]
   [:developers
    [:developer
     [:name "Jared Holmberg"]]]
   #_[:scm
      [:url "https://github.com/IntraCel/core"]
      [:connection "scm:git:https://github.com/IntraCel/core.git"]
      [:developerConnection "scm:git:ssh:git@github.com:IntraCel/core.git"]
      [:tag (str "v" version)]]])

(defn- latest-committed-sha
  "Get the latest committed SHA from current branch."
  []
  (let [branch (git/current-branch)]
    (git/latest-polylith-sha branch)))

(defn- projects-to-deploy
  "Returns the projects to deploy.
   Read more in the api/projects-to-deploy doc on how this works under the hood."
  []
  #_(filterv #{"intracel-core"}
           (api/projects-to-deploy "previous-release"))
  ["intracel-core"])

(defn jar
  "Builds a library jar for the specified project.

   Options:
   * :project - required, the name of the project to build,
   * :jar-file - optional, the path of the JAR file to build,
     relative to the project folder; can also be specified in
     the :jar alias in the project's deps.edn file; will
     default to target/PROJECT-thin.jar if not specified.

   Returns:
   * the input opts with :class-dir, :jar-file, :lib, :pom-file,
     and :version computed.

   Because we build JARs from Polylith projects, all the source
   code we want in the JAR comes from :local/root dependencies of
   the project and the actual dependencies are transitive to those
   :local/root dependencies, so we create a 'lifted' basis.

   Example: clojure -T:build jar :project poly"
  [{:keys [project jar-file] :as opts}]
  (let [project-root (ensure-project-root "jar" project)
        aliases (with-dir (io/file project-root) (get-project-aliases))]
    (b/with-project-root project-root
      (let [basis (lifted-basis)
            class-dir "target/classes"
            jar-file (or jar-file
                         (-> aliases :jar :jar-file)
                         (str "target/" project "-thin.jar"))
            ;;lib (symbol "polylith" (str "clj-" project))
            current-dir (System/getProperty "user.dir")
            current-rel #(str/replace % (str current-dir "/") "")
            directory? #(let [f (java.io.File. %)]
                          (and (.exists f) (.isDirectory f)))
            src+dirs (filter directory? (:classpath-roots basis))
            opts (merge opts
                        {:basis basis
                         :class-dir class-dir
                         :jar-file jar-file
                         :lib lib
                         :scm {:tag                 version
                               :name                "git"
                               :url                 "https://github.com/IntraCel/core"
                               :connection          "scm:git:https://github.com/IntraCel/core.git"
                               :developerConnection "scm:git:git@github.com:IntraCel/core.git"}
                         :src-pom "partial_pom.xml"
                         :pom-data (pom-template version)
                         :version version})]
        (b/delete {:path class-dir})
        (println "\nWriting pom.xml...")
        (b/write-pom opts)
        (println "Copying" (str (str/join ", " (map current-rel src+dirs)) "..."))
        (b/copy-dir {:src-dirs src+dirs
                     :target-dir class-dir})
        (println "Building jar" (str jar-file "..."))
        (b/jar opts)
        ;; we want the pom.xml file in the project folder for deployment:
        (b/copy-file {:src (b/pom-path {:class-dir class-dir
                                        :lib lib})
                      :target "pom.xml"})
        (b/delete {:path class-dir})
        (println "Jar is built.")
        (-> opts
            (assoc :pom-file (str project-root "/pom.xml"))
            ;; account for project root relative paths:
            (update :jar-file (comp #(.getCanonicalPath %) b/resolve-path)))))))




(defn- jar-opts [opts]
  (let [version (if (:snapshot opts) snapshot version)]
    (println "Version:" version)
    (assoc opts
           :lib       lib
           :version   version
           :jar-file  (format "target/%s-%s.jar" lib version)
           :basis     (b/create-basis {:project "../deps.edn"})
           :class-dir class-dir
           :target    "target"
           :src-dirs  ["src" "resources" "../../components" "../../bases"]
           :scm       {:connection          "scm:git:https://github.com/IntraCel/core.git"
                       :developerConnection "scm:git:git@github.com:IntraCel/core.git"
                       :tag                 version
                       :url                 "https://github.com/IntraCel/core"}
           :pom-data  (pom-template version))))

(defn uberjar
  "Builds an uberjar for the specified project.
  Options:
  * :project - required, the name of the project to build,
  * :uber-file - optional, the path of the JAR file to build,
    relative to the project folder; can also be specified in
    the :uberjar alias in the project's deps.edn file; will
    default to target/PROJECT.jar if not specified.
  Returns:
  * the input opts with :class-dir, :compile-opts, :main, and :uber-file
    computed.
  The project's deps.edn file must contain an :uberjar alias
  which must contain at least :main, specifying the main ns
  (to compile and to invoke)."
  [{:keys [project uber-file] :as opts}]
  (let [project-root (ensure-project-root "uberjar" project)
        aliases      (with-dir (io/file project-root) (get-project-aliases))
        main         (-> aliases :uberjar :main)]
    (binding [b/*project-root* project-root]
      (let [class-dir "target/classes"
            uber-file (or uber-file
                          (-> aliases :uberjar :uber-file)
                          (str "target/" project ".jar"))
            opts      (jar-opts (merge opts
                                       (if (some? main)
                                         {:main main}
                                         nil)))]
        (b/delete {:path "target"})
        (prn "Writing pom.xml...")
        (b/write-pom opts)
        (prn "Copying source...")
        (b/copy-dir {:src-dirs ["src" "resources" "../../components" "../../bases"]
                     :target-dir class-dir})
        (prn "Compiling...")
        (prn "Resolved src-dirs:" (map #(str (io/file %)) ["src" "resources" "components" "bases"]))
        (doseq [dir ["src" "resources" "components" "bases"]]
          (let [resolved-dir (io/file dir)]
            (prn "Checking directory:" resolved-dir)
            (prn "Exists?" (.exists resolved-dir))
            (prn "Files:" (file-seq resolved-dir))))
        (b/compile-clj {:basis (:basis opts)
                        :src-dirs ["components" "bases"]
                        :class-dir class-dir})
        (prn "Building uberjar...")
        (b/uber (merge opts
                       (if (some? uber-file)
                         {:uber-file uber-file}
                         nil)
                       {:basis (:basis opts)}))
        (println "Uberjar is built.")
        opts))))

(defn libjar
  "Builds an uberjar for the specified project.
  Options:
  * :project - required, the name of the project to build,
  * :uber-file - optional, the path of the JAR file to build,
    relative to the project folder; can also be specified in
    the :uberjar alias in the project's deps.edn file; will
    default to target/PROJECT.jar if not specified.
  Returns:
  * the input opts with :class-dir, :compile-opts, :main, and :uber-file
    computed.
  The project's deps.edn file must contain an :uberjar alias
  which must contain at least :main, specifying the main ns
  (to compile and to invoke)."
  [{:keys [project uber-file] :as opts}]
  (let [project-root (ensure-project-root "uberjar" project)
        aliases      (with-dir (io/file project-root) (get-project-aliases))
        main         (-> aliases :uberjar :main)]
    (binding [b/*project-root* project-root]
      (prn "Project root: " b/*project-root*)
      (let [class-dir "target/classes"
            uber-file (or uber-file
                          (-> aliases :uberjar :uber-file)
                          (str "target/" project ".jar"))
            opts      (jar-opts (merge opts
                                       (if (some? main)
                                         {:main main}
                                         nil)))]
        (b/delete {:path "target"})
        (prn "Writing pom.xml...")
        (b/write-pom opts)
        (prn "Copying source...")
        #_(b/copy-dir {:src-dirs ["../../components" "../../bases"]
                     :target-dir class-dir})
        (prn "Compiling...")
        (prn "Resolved src-dirs:" (map #(str (.getAbsolutePath (io/file %))) ["components" "bases"]))
        (doseq [dir ["components" "bases"]]
          (let [resolved-dir (io/file dir)]
            (prn "Checking directory:" (.getAbsolutePath resolved-dir))
            (prn "Exists?" (.exists resolved-dir))
            (prn "Files:" (mapv #(.getAbsolutePath %) (file-seq resolved-dir)))))
        
        (prn "basis:" (lifted-basis))
        (b/compile-clj {:basis (lifted-basis)
                        :src-dirs ["components" "bases"]
                        :class-dir class-dir})
        (prn "Building library jar...")
        (b/jar (merge opts
                      (if (some? uber-file)
                        {:uber-file uber-file}
                        nil)
                      {:basis (:basis opts)}))
        (println "Library jar is built.")
        opts))))

(defn- run-task [aliases]
  (println "\nRunning task for" (str/join "," (map name aliases)))
  (let [basis    (b/create-basis {:aliases aliases})
        combined (t/combine-aliases basis aliases)
        cmds     (b/java-command
                  {:basis      basis
                   :main      'clojure.main
                   :main-args (:main-opts combined)})
        {:keys [exit]} (b/process cmds)]
    (when-not (zero? exit) (throw (ex-info "Task failed" {})))))

(defn gen-doc-tests "Generate tests from doc code blocks." [opts]
  (run-task [:gen-doc-tests])
  opts)

(defn run-doc-tests
  "Generate and run doc tests.

  Optionally specify :aliases vector:
  [:1.10] -- test against Clojure 1.10.3 (the default)
  [:1.11] -- test against Clojure 1.11.0
  [:1.12] -- test against Clojure 1.12.0
  [:cljs] -- test against ClojureScript"
  [{:keys [aliases] :as opts}]
  (gen-doc-tests opts)
  (run-task (-> [:test :runner :test-doc]
                (into aliases)
                (into (if (some #{:cljs} aliases)
                        [:test-doc-cljs]
                        [:test-doc-clj]))))
  opts)

(defn test "Run basic tests." [opts]
  (run-task [:test :runner :1.11])
  (run-task [:test :runner :cljs])
  opts)





(defn ci
  "Run the CI pipeline of tests (and build the JAR).

  Default Clojure version is 1.10.3 (:1.10) so :elide
  tests for #409 on that version."
  [opts]
  (let [aliases [:cljs :elide :1.11 :1.12]
        opts    (jar-opts opts)]
    (b/delete {:path "target"})
    #_(doseq [alias aliases]
        (run-doc-tests {:aliases [alias]}))
    (doseq [alias aliases]
      (run-task [:test :runner alias]))
    (b/delete {:path "target"})
    (println "Writing pom.xml...")
    (b/write-pom opts)
    (println "Copying source...")
    (b/copy-dir {:src-dirs ["src"] :target-dir class-dir})
    (println "Building" (:jar-file opts) "...")
    (b/jar opts))
  opts)

(defn deploy "Deploy the JAR to Clojars." [opts]
  (let [{:keys [jar-file project] :as opts} (jar-opts opts)
        project-root (ensure-project-root "deploy" project)]
    (binding [b/*project-root* project-root] 
      (dd/deploy {:installer :remote :artifact (b/resolve-path jar-file)
                  :pom-file (b/pom-path (select-keys opts [:lib :class-dir]))})))
  opts)

(defn polydeploy
  "Create and deploy library JAR files for the Polylith project.

   Currently, creates 'poly'.

   You can do a dry run by passing :installer :local which will
   deploy the JARs into your local Maven cache instead of to Clojars."
  [opts]
  (let [projects (projects-to-deploy)]
    (when (empty? projects)
      (throw (ex-info "Cannot deploy projects. No projects have changed." {})))
    (doseq [project projects]
      (let [project-opts (assoc opts
                                :project project
                                :installer (get opts :installer :remote))]
        (println (str "Starting deployment for " project " project."))
        (-> project-opts
            (jar)
            (set/rename-keys {:jar-file :artifact})
            (dd/deploy))
        (println (str "Deployment completed for " project " project."))))))