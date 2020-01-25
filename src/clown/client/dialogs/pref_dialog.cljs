;;;;
;;;; This namespace contains the preferences dialog for the application.
;;;;

(ns clown.client.dialogs.pref-dialog
  (:require [clown.client.dialogs.util :as dlgu]
            [clown.client.util.dom-utils :as du]
            [reagent.core :as r]))

(defn de-activate-glass-pane
  "De-activate (make invisible) the glass pane."
  []
  (du/set-style-property-value "modal-overlay" "display" "none"))

(defn activate-glass-pane
  "Activate (make visible) the glass pane, preventing interaction with any
  elements below it in the z-stack."
  []
  (du/set-style-property-value "modal-overlay" "display" "block"))

;; Data from mostly failed attempt to create a parameterized confirm/cancel
;; type dialog. Parts are still used here.
(def confirm-cancel-dialog-params
  {:id-basis                  "confirm-cancel-basis"
   :header                    "Preferences"
   :message-text              ["Confirm or Cancel this dialog."
                               "Press One of the Buttons Below"]
   :confirm-button-text       "Save"
   :cancel-button-text        "Cancel"
   :confirm-button-popup-text "Save all changes and close this dialog."
   :cancel-button-popup-text  "Abandon all changes and close this dialog."})

(defn save-confirmed-changes!
  "Save preferences changes both in the local program state and on the server."
  [aps original-values working-copy-ratom]
  (println "save-confirmed-changes!")
  (let [pc (r/cursor aps [:preferences])
        pref-keys (keys original-values)]
    (doseq [k pref-keys]
      (when (not= (k original-values) (k @working-copy-ratom))
        (swap! pc assoc k (k @working-copy-ratom))
        ((:send-message-fn @aps)
         (pr-str {:message {:command "hey-server/persist-numeric-preference-value"
                            :data    {:preference k
                                      :value      (k @working-copy-ratom)}}}))))))

(defn prefs-dialog
  "Layout the preferences dialog and handlers."
  [aps settings]
  (fn [aps settings]
    (when (:show-prefs-dialog @aps)
      (activate-glass-pane)
      (let [modal-id (dlgu/id-basis->id (:id-basis settings))
            original-values (:original-values @aps)
            working-copy (:working-copy @aps)
            load-file-r (r/cursor working-copy [:load-last-file])
            interval-r (r/cursor working-copy [:autosave_interval])
            o-width-r (r/cursor working-copy [:outline_width])
            n-width-r (r/cursor working-copy [:note_width])
            close-dlg (fn []
                        (de-activate-glass-pane)
                        (swap! aps dissoc :show-prefs-dialog))]

        [:div {:class "modal"
               :id    modal-id
               :role  "dialog"}
         [:header {:class "modal-header"}
          [:section {:class "modal-header-left"} (:header settings)]
          [:section {:class "modal-header-right"}
           [:input {:type     "button"
                    :class    "tree-demo--button"
                    :id       (str "close- " (:id-basis settings) "-button")
                    :value    "Close"
                    :title    (:cancel-button-popup-text settings)
                    :on-click close-dlg}]]]

         [:form {:class "modal-guts"}
          [:field-set {:class "prefs--container"}

           ;; The checkbox to select whether or not to load the last
           ;; document viewed the next time the program is loaded.
           [:div {:class "prefs--row"}
            [:label {:class "prefs--label"} "Load Last Document:"]
            [:input {:type      "checkbox"
                     :name      "load-last-file"
                     :id        "load-last-file"
                     :checked   (if @load-file-r true false)
                     :on-change #(let [chk-state (du/event->target-checked %)]
                                   (reset! load-file-r chk-state))}]
            (dlgu/quarter-rem-spacer)
            (dlgu/half-rem-spacer)
            [:label {:class "prefs--help-char"
                     :title (str "Whether to load the last document viewed the "
                                 "next time the program is started.")} "?"]]

           ;; Autosave interval.
           [:div {:class "prefs--row"}
            [:label {:class "prefs--label"} "Autosave Interval:"]
            [:input {:type      "number"
                     :class     "prefs--text-editor"
                     :step      1
                     :value     @interval-r
                     :on-change #(reset! interval-r (js/parseInt (du/event->target-value %)))}]
            (dlgu/half-rem-spacer)
            [:label {:class "prefs--help-char"
                     :title (str "The number of idle seconds to wait for before "
                                 "saving the document. A value of 0 means no "
                                 "automatic saves will be done.")} "?"]]
           [:p {:class "prefs--help-text"
                :style {:color "red"}}
            "The \"Autosave\" function is not ready yet."]

           ;; Column width for outliner.
           [:div {:class "prefs--row"}
            [:label {:class "prefs--label"} "Outliner Column Width:"]
            [:input {:type      "number"
                     :class     "prefs--text-editor"
                     :step      1
                     :value     @o-width-r
                     :on-change #(reset! o-width-r (js/parseInt (du/event->target-value %)))}]
            (dlgu/half-rem-spacer)
            [:label {:class "prefs--help-char"
                     :title (str "The width of the outliner column, in pixels. "
                                 "This can also be set by dragging the right border "
                                 "of the column with the mouse.")} "?"]]

           ;; Column width for notes.
           [:div {:class "prefs--row"}
            [:label {:class "prefs--label"} "Note Column Width:"]
            [:input {:type      "number"
                     :class     "prefs--text-editor"
                     :step      1
                     :value     @n-width-r
                     :on-change #(reset! n-width-r (js/parseInt (du/event->target-value %)))}]
            (dlgu/half-rem-spacer)
            [:label {:class "prefs--help-char"
                     :title (str "The width of the note column, in pixels. "
                                 "This can also be set by dragging the right border "
                                 "of the column with the mouse.")} "?"]]]]

         ;; The footer with the cancel/confirm buttons.
         [:div {:class "modal-footer"}
          [:section {:class "tree-demo--button-area"}
           [:input {:type     "button"
                    :class    "tree-demo--button button-bar-item"
                    :id       "pref-dialog-cancel-button-id"
                    :value    (:cancel-button-text settings)
                    :title    (:cancel-button-popup-text settings)
                    :on-click close-dlg}]
           [:input {:type     "button"
                    :class    "tree-demo--button button-bar-item"
                    :id       (dlgu/id-basis->cancel-id (:id-basis settings))
                    :value    (:confirm-button-text settings)
                    :title    (:confirm-button-popup-text settings)
                    :on-click #(do (save-confirmed-changes! aps original-values working-copy)
                                   (close-dlg))
                    :disabled (= original-values @working-copy)}]]]]))))

(defn layout-prefs-dialog
  "Layout a parameterized preferences dialog."
  [aps]
  [prefs-dialog aps confirm-cancel-dialog-params])
