(ns clown.server.handler
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clown.server.util.config :refer [config]]
    [clown.server.db :as db]
    [compojure.core :refer [defroutes GET]]
    [compojure.route :refer [not-found]]
    [org.httpkit.server :refer [on-receive on-close send! with-channel]]
    [ring.middleware.resource :refer [wrap-resource]]
    [ring.middleware.reload :refer [wrap-reload]]
    [taoensso.timbre :refer [trace debug info warn error
                             tracef debugf infof warnf errorf]]))

(defn user-name
  "Return the user name."
  []
  (System/getProperty "user.name"))

(defn save-outline-as-edn
  "Save the outline as EDN to the first file in the MRU list. THIS IS
  NOT HOW IT SHOULD WORK IN PERPETUITY. IT HAS TO HANDLE NEW, NEVER
  BEFORE SAVED FILES TOO."
  [outline]
  (debug "save-outline-as-edn!")
  (debugf "    outline: %s" outline)
  (debugf "    pretty printed:\n %s" (pprint/write outline :stream nil))
  (let [prefs (db/get-preference-map)]
    (if-let [file-name (first (:mru prefs))]
      ;; Pretty print the map since the outline is supposed to be
      ;; human-readable too.
      (spit file-name (pprint/write {:outline outline} :stream nil))
      (warn "Trouble finding file name"))))

(def ws-client (atom nil))

(defn mesg-received [msg]
  (debugf "mesg-received: msg: %s" msg)
  (let [message-map (edn/read-string msg)
        command (get-in message-map [:message :command])
        data (get-in message-map [:message :data])]
    (debugf "command: %s" command)
    (debugf "data: %s" data)

    (cond

      (= command "hey-server/send-preferences")
      (send! @ws-client (pr-str {:message {:command "hey-client/accept-these-preferences"
                                           :data    (merge (db/get-preference-map)
                                                           {:user (user-name)})}}))

      (= command "hey-server/send-user-name")
      (send! @ws-client (pr-str {:message {:command "hey-client/accept-user-name"
                                           :data    (user-name)}}))

      (= command "hey-server/send-outline")
      (when-let [from-file (edn/read-string (slurp data))]
        ;(infof "from-file: %s" from-file)
        (send! @ws-client (pr-str {:message {:command "hey-client/accept-this-outline"
                                             :data    from-file}})))

      (= command "hey-server/persist-new-preference-value")
      (db/set-preference-value (:preference data) (:value data))

      (= command "hey-server/save-this-outline-as-edn")
      (save-outline-as-edn data))))

(defn websocket-handler [req]
  (with-channel req channel
                (debugf "channel: %s connected" channel)
                (reset! ws-client channel)
                (on-receive channel #'mesg-received)
                (on-close channel (fn [status]
                                    (reset! ws-client nil)
                                    (info channel "closed, status" status)))))

(defroutes routes
           (GET "/ws" [] websocket-handler)
           (GET "/" [] (io/resource "public/index.html"))
           (not-found "<p>Page not found.</p>"))

(def all-routes (if (= "development" (:task-type config))
                  (wrap-reload (wrap-resource routes "public"))
                  (wrap-resource routes "public")))
