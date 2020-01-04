(ns clown.client.commands
  (:require
    [clown.client.util.dom-utils :as du]
    [clown.client.util.empty-outline :refer [build-empty-outline
                                             empty-outline-file-name
                                             formatted-time-now]]
    [taoensso.timbre :as timbre :refer [tracef debugf infof warnf errorf
                                        trace debug info warn error]]))

(defn save-outline-as-edn!
  "Tell the server to save this outline in EDN format after updating it with
  a new modification time."
  [{:keys [root-ratom evt]}]
  (debug "save-outline-as-edn!")
  (du/prevent-default evt)
  (swap! root-ratom assoc-in [:current-outline :modified] (formatted-time-now))
  (debugf "    pr-str message: %s"
          (pr-str {:message {:command "hey-server/save-this-outline-as-edn"
                             :data    (:current-outline @root-ratom)}}))
  ((:send-message-fn @root-ratom)
   (pr-str {:message {:command "hey-server/save-this-outline-as-edn"
                      :data    (:current-outline @root-ratom)}})))
