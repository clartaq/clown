(ns clown.client.util.mru
  (:require ;[clown.client.util.dialogs :as dlg]
            [clown.client.util.vector-utils :as vu]
            [reagent.core :as r]))

(defn persist-new-mru
  "Send the mru portion of the preferences to the server."
  [aps]
  (let [mru (get-in @aps [:preferences :mru])]
    ((:send-message-fn @aps)
     (pr-str {:message {:command "hey-server/persist-new-preference-value"
                        :data    {:preference :mru
                                  :value      mru}}}))))

(defn push-on-mru
  "Update the recent file list in preferences with the name of the file
  just opened.  The new file name will be the first in the mru."
  [aps file-name]
  (let [old-mru (get-in @aps [:preferences :mru])
        new-mru (vu/prepend-uniquely old-mru file-name)
        mru-cursor (r/cursor aps [:preferences :mru])]
    ;(dlg/toggle-about-to-update-mru-modal)
    (swap! mru-cursor :assoc new-mru)
    (persist-new-mru aps)))


