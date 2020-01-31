(ns clown.client.commands
  (:require
    [clown.client.util.dom-utils :as du]
    [clown.client.util.empty-outline :refer [build-empty-outline
                                             empty-outline-file-name
                                             formatted-time-now]]
    (clown.client.util.marker :as mrk)
    [taoensso.timbre :as timbre :refer [tracef debugf infof warnf errorf
                                        trace debug info warn error]]))

(defn save-outline-as-edn!
  "Tell the server to save this outline in EDN format after updating it with
  a new modification time."
  [aps]
  (debug "save-outline-as-edn!")
  (swap! aps assoc-in [:current-outline :modified] (formatted-time-now))
  (mrk/mark-as-clean! aps)
  (debugf "    pr-str message: %s"
          (pr-str {:message {:command "hey-server/save-this-outline-as-edn"
                             :data    (:current-outline @aps)}}))
  ((:send-message-fn @aps)
   (pr-str {:message {:command "hey-server/save-this-outline-as-edn"
                      :data    (:current-outline @aps)}})))
