;;;;
;;;; Functions that relate to the program database.
;;;;

(ns clown.server.db
  (:gen-class)
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [clojure.pprint :as pp]
    [clown.server.util.config :refer [config]]
    [clown.server.util.strings :as su]
    [taoensso.timbre :refer [trace debug info warn error
                             tracef debugf infof warnf errorf]])
  (:import (java.io File)
           (org.h2.jdbc JdbcClob)))

;;;-----------------------------------------------------------------------------
;;; Things that deal with the database file and connection.

;; The kerfuffle here is to get the directory from which the program
;; is running and create an absolute path as required for the H2 database.

(defn get-db-file-name []
  (str (-> (File. ".")
           .getAbsolutePath
           (su/remove-from-end "."))
       (:db-file-name-suffix config)))

;; Because H2 seems to append this to the name above.
(defn get-db-file-name-long []
  (str (get-db-file-name) (:db-long-file-name-suffix config)))

(defn get-h2-db-spec []
  {:classname   "org.h2.Driver"
   :subprotocol "h2:file"
   :subname     (get-db-file-name)
   :make-pool?  true})

;;;-----------------------------------------------------------------------------
;;; Things that deal with user preferences.

(defn initial-preferences
  "Return a map of the initial, default preference settings."
  []
  {:autosave_interval     1
   :default_new_note_name "A New Note"
   :default_new_note_text "Add the text of your new note here."
   :default_new_tag_label "A New Tag"
   :editor_editing_font   "Calibri"
   :outline_width         500
   :note_width            400
   :load-last-file        true
   :mru                   ["World_Domination_Plan.edn"]})

(defn- update-preference-map
  "Update the database with the new map of preferences."
  [m db]
  (let [opt-str (with-out-str (pp/pprint m))]
    (jdbc/update! db :preferences {:preferences_edn opt-str} ["preferences_id = ?" 1])))

(defn get-preference-map
  "Return the entire preferences map from the database."
  ([] (get-preference-map (get-h2-db-spec)))
  ([db]
   (-> db
       (jdbc/query ["select preferences_edn from preferences where preferences_id=1"])
       (first)
       (:preferences_edn)
       (edn/read-string))))

(defn get-preference-value
  "Return the value of the preference associated with the key or nil if there is
  no such key in the preferences table."
  ([k] (get-preference-value k (get-h2-db-spec)))
  ([k db]
   (k (get-preference-map db))))

(defn set-preference-value
  "Update the preferences in the database to include the key/value given,
  whether the key existed in the preferences map before or not, that is,
  a previous k/vu pair that was not already in the preferences will be added."
  ([k v] (set-preference-value k v (get-h2-db-spec)))
  ([k v db]
   (let [m (get-preference-map db)
         nm (merge m {k v})]
     (update-preference-map nm db))))

;;;-----------------------------------------------------------------------------
;;; Things that have to do with initial creation of the database.

(defn add-initial-preferences!
  [db]
  (info "Adding options.")
  (let [opts (initial-preferences)
        opts-str (with-out-str (pp/pprint opts))]
    (jdbc/insert! db :preferences {:preferences_edn opts-str}))
  (info "Done!"))

(defn create-tables
  [db-spec]
  (info "create-tables")
  (try (jdbc/db-do-commands
         db-spec false
         [(jdbc/create-table-ddl :preferences
                                 [[:preferences_id :integer :auto_increment :primary :key]
                                  [:preferences_edn :varchar]])])
       (catch Exception e (println e)))
  (info "create-tables: exit"))

(defn create-db
  "Create the database tables and initialize them with content for
  first-time use."
  [db-spec]
  (info "Creating database")
  (create-tables db-spec)
  (add-initial-preferences! db-spec)
  ;(add-full-text-search! db-spec)
  ;(init-admin-table! db-spec)
  ;(add-initial-users! db-spec)
  ;(add-initial-pages! db-spec)
  ;(add-initial-roles! db-spec)
  ;(add-initial-options! db-spec)
  ;(add-indices! db-spec)
  ;(complete-full-text-search! db-spec)
  )

(defn- db-exists?
  "Return true if the wiki database already exists."
  [db-name]
  (.exists ^File (clojure.java.io/as-file db-name)))

(defn stop-db! []
  (info "Stopping db."))

(defn start-db! []
  (info "Starting db.")
  (when-not (db-exists? (get-db-file-name-long))
    (info "Creating initial database.")
    (infof "get-db-file-name-long: %s" (get-db-file-name-long))
    (infof "get-db-file-name: %s" (get-db-file-name))
    (io/make-parents (get-db-file-name))
    (create-db (get-h2-db-spec))))