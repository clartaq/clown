(ns clown.server.main
  (:require [clown.server.util.config :as config]
            [clown.server.db :as db]
            [clown.server.server :as server]
            [taoensso.timbre :as timbre :refer [trace debug info warn error
                                                tracef debugf infof warnf errorf]])
  (:gen-class))

(defn start-app!
  "Initialize and start the major program components. REPL convenience function."
  [& [args]]
  (info "Starting clown.")
  (config/start-config!)
  (db/start-db!)
  (server/start-server! args))

(defn stop-app!
  "Shut down the application. REPL convenience function."
  []
  (info "Stopping clown.")
  (server/stop-server!)
  (db/stop-db!)
  (config/stop-config!))

(defn dev-main [& [args]]
  (timbre/set-level! :info)
  (info "Starting dev-main.")
  (start-app! args))

(defn -main
  "Program entry point."
  [& args]
  (timbre/set-level! :info)
  (info "Starting main.")
  (start-app! args))
