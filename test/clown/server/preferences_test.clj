;;;
;;; This namespace contains tests related to the use of user preferences.
;;;

(ns clown.server.preferences-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clown.server.db :refer :all]
            [clown.server.util.strings :as su]
            [taoensso.timbre :refer [trace debug info warn error
                                     tracef debugf infof warnf errorf]])
  (:import (java.io File)))

(println "clown-preferences-test")
;-------------------------------------------------------------------------------
; Functions to set up the test database.
;-------------------------------------------------------------------------------

; The kerfuffle here is to get the directory from which the program
; is running and create an absolute path as required for the H2 database.

(defn get-test-db-file-name []
  (str (-> (File. ".")
           .getAbsolutePath
           (su/remove-from-end "."))
       "test/data/db/testdatabase.db"))

; Because H2 seems to append this to the name above.
(defn get-test-db-file-name-long []
  (str (get-test-db-file-name) ".mv.db"))

(defn get-test-db-spec []
  {:classname   "org.h2.Driver"
   :subprotocol "h2:file"
   :subname     (get-test-db-file-name)
   :make-pool?  true})

;(defn- add-test-page-from-file!
;  "Add a page to the database based on the information in a Markdown file
;  containing YAML front matter."
;  [file-name db]
;  (let [file (File. (str "test/data/" file-name))
;        m (su/load-markdown-from-file file)]
;    (add-page-from-map m "CWiki" db)))
;
;(defn add-test-pages!
;  "Read the pages used for testing from su and add them to the test database."
;  [db]
;  (info "Adding test pages.")
;  (mapv #(add-test-page-from-file! % db) ["A_Dummy_Test_Page.md"
;                                          "NoContentHere.md"
;                                          "NoDatesOrTags.md"
;                                          "NoMeta.md"
;                                          "Test_Page_for_Tags.md"])
;  (info "Done!"))

(defn- create-test-db
  "Create the database tables and initialize them with content for
  first-time use."
  [db]
  (create-tables db)
  (add-initial-preferences! db))

(defn- init-test-db!
  "Initialize the database. Will create the database and
  tables."
  [db short-db-file-name long-db-file-name]
  (info "Creating initial test database.")
  (io/delete-file (io/file long-db-file-name) true)
  (io/make-parents short-db-file-name)
  (create-test-db db))

;-------------------------------------------------------------------------------
; Test fixtures.
;-------------------------------------------------------------------------------

(defn one-time-setup []
  (info "one time setup")
  (init-test-db! (get-test-db-spec) (get-test-db-file-name) (get-test-db-file-name-long))
  (info "setup complete"))

(defn one-time-teardown []
  (info "one time teardown")
  ;(io/delete-file (io/file test-db-file-name-long) true)
  (info "teardown complete")
  )

(defn once-fixture [f]
  (one-time-setup)
  ;(println (type f))
  (f)
  (one-time-teardown))


(defn setup []
  ;(println "setup")
  )

(defn teardown []
  ;(println "teardown")
  )

(defn each-fixture [f]
  (setup)
  ;(println (type f))
  (f)
  (teardown))

(use-fixtures :once once-fixture)
(use-fixtures :each each-fixture)

;-------------------------------------------------------------------------------
; Tests
;-------------------------------------------------------------------------------

(deftest get-preference-value-test
  (testing "Testing the get-option-value function."
    (let [db-spec (get-test-db-spec)]
      (is (nil? (get-preference-value :random_value db-spec)))
      (is (= 500 (get-preference-value :outline_width db-spec)))
      (is (= "A New Note" (get-preference-value :default_new_note_name db-spec)))
      (is (= 1 (get-preference-value :autosave_interval db-spec))))))

(deftest set-preference-value-test
  (testing "Testing the set-option-value function."
    (let [db-spec (get-test-db-spec)
          scal-str "Scralubrious"
          scal-key (keyword scal-str)]
      (set-preference-value :wiki_name "ClajWIKI" db-spec)
      (is (= "ClajWIKI" (get-preference-value :wiki_name  db-spec)))
      (set-preference-value scal-key scal-str db-spec)
      (is (= scal-str (get-preference-value scal-key db-spec))))))
