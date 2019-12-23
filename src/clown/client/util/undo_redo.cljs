(ns clown.client.util.undo-redo)

(enable-console-print!)

(defprotocol UndoProtocol
  "A protocol track changes to an atom, allowing undo and redo! operations on that atom."
  (init [this] "Initialize the state of thing implementing this protocol.")
  (num-undos [this] "Return the number of states that can be undone.")
  (num-redos [this] "Return the number of states that can be redone.")
  (can-undo? [this] "Return true if a state change can be undone.")
  (can-redo? [this] "Return true if a state change can be redone.")
  (undo! [this] "Reverse one recorded state change. Return the restored state.")
  (redo! [this] "Re-apply a recorded state change. Return the re-applied state.")
  (clear-undo! [this] "Empty the redo! stack.")
  (clear-redo! [this] "Empty the redo! stack.")
  (pause-tracking! [this] "Pause recording state changes.")
  (resume-tracking! [this] "Resume recording state changes.")
  (stop-tracking! [this] "Stop tracking changes. Disconnect the watching function. Forget all saved state changes."))

(defrecord UndoManager [id tracked-atom undo-stack redo-stack paused watch-fn]

  UndoProtocol

  (init [this]
    (swap! undo-stack conj @tracked-atom)
    (add-watch tracked-atom id watch-fn)
    this)

  (num-undos [_] (count @undo-stack))

  (num-redos [_] (count @redo-stack))

  (can-undo? [this] (> (num-undos this) 1))

  (can-redo? [this] (pos? (num-redos this)))

  (undo! [this]
    (when (can-undo? this)
      (let [current-state (peek @undo-stack)
            _ (swap! undo-stack pop)
            restored-state (peek @undo-stack)]
        (swap! redo-stack conj current-state)
        (pause-tracking! this)
        (reset! tracked-atom restored-state)
        (resume-tracking! this)
        restored-state)))

  (redo! [this]
    (when (can-redo? this)
      (let [state-to-restore (peek @redo-stack)]
        (swap! redo-stack pop)
        (swap! undo-stack conj state-to-restore)
        (pause-tracking! this)
        (reset! tracked-atom state-to-restore)
        (resume-tracking! this)
        state-to-restore)))

  (clear-undo! [_] (reset! undo-stack []))

  (clear-redo! [_] (reset! redo-stack []))

  (pause-tracking! [_] (reset! paused true))

  (resume-tracking! [_] (reset! paused false))

  (stop-tracking! [this]
    (remove-watch tracked-atom id)
    (clear-undo! this)
    (clear-redo! this)))

(defn undo-manager
  "Return an initialized UndoManager object for the tracked atom."
  [tracked-atom]
  (let [id (keyword (str "undo-manager-tracker-" (goog/getUid @tracked-atom)))
        undo-stack (atom [])
        redo-stack (atom [])
        paused (atom false)
        watching-fn (fn [key atom old-state new-state]
                      ;(prn "-- Atom Changed --")
                      ;(prn "key: " key)
                      ;(prn "atom: " atom)
                      ;(prn "old-state: " old-state)
                      ;(prn "new-state: " new-state)
                      ;(prn "paused: " @paused)
                      ;(let [ae (.-activeElement js/document)]
                      ;  (when (= "TEXTAREA" (.-tagName ae))
                      ;    (let [sel-start (.-selectionStart ae)
                      ;          sel-end (.-selectionEnd ae)]
                      ;      (println "sel-start: " sel-start)
                      ;      (println "sel-end: " sel-end))))
                      (when-not @paused
                        (reset! redo-stack [])
                        (reset! paused true)
                        (swap! undo-stack conj new-state)
                        (reset! paused false)))]
    (init (UndoManager. id tracked-atom undo-stack redo-stack paused watching-fn))))
