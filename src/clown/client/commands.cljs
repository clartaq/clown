(ns clown.client.commands
  (:require
    [clown.client.util.clown-time :as ct]
    [clown.client.util.dom-utils :as du]
    [clown.client.util.empty-outline :refer [build-empty-outline
                                             empty-outline-file-name]]
    [clown.client.util.marker :as mrk]
    [clown.client.util.mru :refer [push-on-mru!]]
    [taoensso.timbre :as timbre :refer [tracef debugf infof warnf errorf
                                        trace debug info warn error]]))

(defn new-outline
  "Create a new outline and make it the current outline."
  [aps]
  ;; BUG HERE!!! This doesn't really seem to work as expected.
  ;; It's like the new undo-manager never gets attached to the
  ;; ratom. The stuff from the old ratom is still there. You can
  ;; load files, make changes, etc, and then back up through all
  ;; the file changes (without resetting outline title and
  ;; file name) all the way back to when the app was loaded.
  (swap! aps assoc :current-outline
         (build-empty-outline aps))
  (mrk/mark-as-clean! aps)
  (push-on-mru! aps (empty-outline-file-name)))


(defn save-outline-as-edn!
  "Tell the server to save this outline in EDN format after updating it with
  a new modification time."
  [aps]
  (debug "save-outline-as-edn!")
  (swap! aps assoc-in [:current-outline :modified] (ct/formatted-time-now))
  (mrk/mark-as-clean! aps)
  (debugf "    pr-str message: %s"
          (pr-str {:message {:command "hey-server/save-this-outline-as-edn"
                             :data    (:current-outline @aps)}}))
  ((:send-message-fn @aps)
   (pr-str {:message {:command "hey-server/save-this-outline-as-edn"
                      :data    (:current-outline @aps)}})))
