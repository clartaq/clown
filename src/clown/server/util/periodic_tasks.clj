;; Largely stolen from:
;; https://github.com/Ruiyun/tools.timer/blob/master/src/ruiyun/tools/timer.clj

(ns clown.server.util.periodic-tasks
  (:import (java.util Date Timer TimerTask)))

(defn timer
  "Create and return a new timer."
  [name]
  (Timer. ^String name))

(defn cancel-task!
  "Cancel the task."
  [a-timer]
  (.cancel ^Timer a-timer))

(defn run-task!
  "Execute a timer task, then return the timer user passed or be auto created."
  [task & {:keys [^Timer by, ^Date at, ^long delay, ^long period, on-exception]}]
  (let [task  (proxy [TimerTask] []
                (run []
                  (if (nil? on-exception)
                    (task)
                    (try
                      (task)
                      (catch Exception e
                        (on-exception e))))))
        ^Timer timer (or by (timer "default"))
        ^long start-time (or at delay 0)]
    (if (nil? period)
      (.schedule timer task start-time)
      (.schedule timer task start-time period))
    timer))
