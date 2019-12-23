;;;;
;;;; Read configuration items from a config.edn file. The particular file read
;;;; is determined by a map the project.clj under the relevant profile. So,
;;;; if you have different profiles for, say, development, production, test,
;;;; and so on, you can have different configuration files and different
;;;; configurations.
;;;;
;;;; This is a blatant rip-off of the much more capable yogthos/config
;;;; library.
;;;;

(ns clown.server.util.config
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [taoensso.timbre :refer [trace debug info warn error
                             tracef debugf infof warnf errorf]])
  (:import (java.io PushbackReader)))

;(defn- read-config-file
;  "Return the map of configuration items read from the edn configuration file.
;  Returns nothing if the files does not exist, otherwise throws in the event
;  of an error of some type."
;  [f]
;  (try
;    (when-let [url (or (io/resource f) (io/file f))]
;      (with-open [r (-> url io/reader PushbackReader.)]
;        (edn/read r)))
;    (catch java.io.FileNotFoundException _)
;    (catch Exception e
;      (warn (str "Failed to parse " f ":\n" (.getLocalizedMessage e))))))

(defn load-config
  "Generate a map of immutable configuration variables."
  []
  ;(read-config-file "config.edn")
  {:port                     3025
   :http-port                1597
   :db-file-name-suffix      "resources/public/db/database.db"
   :db-long-file-name-suffix ".mv.db"
   :task-type                "development"}
  )

(defonce
  ^{:doc "A map of environment variables."}
  config (load-config))

(defn- reload-config []
  (alter-var-root #'config (fn [_] (load-config))))

(defn start-config!
  "Start the configuration service."
  []
  (info "Starting config."))

(defn stop-config!
  "Stop the configuration service."
  []
  (info "Stopping config."))

(defn reset-config!
  "Reset the configuration service."
  []
  (info ("Resetting config")
        (stop-config!)
        (start-config!)))