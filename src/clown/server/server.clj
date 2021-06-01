(ns clown.server.server
  (:gen-class)
  (:require [clown.server.util.config :refer [config]]
            [clown.server.handler :refer [all-routes]]
            [org.httpkit.server :as http-kit]
            [taoensso.timbre :refer [trace debug info warn error
                                     tracef debugf infof warnf errorf]]))

(defonce ^{:private true} web-server_ (atom nil))

(defn stop-server! []
  (info "Stopping web server.")
  (when-let [stop-fn @web-server_]
    (infof "stop-fn: %s" stop-fn)
    (stop-fn)))

(defn start-server! [& [args]]
  (stop-server!)
  ;; A port number to use is the only valid use of the args, so try to
  ;; convert the string argument to an integer and use it for the port.
  (infof "(:http-port config): %s" (:http-port config))
  (let [http-port (or (and args
                           (first args)
                           (integer? (first args))
                           (first args))
                      (:http-port config)
                      3000)
        [port-used stop-fn] (let [stop-fn (http-kit/run-server
                                           ;wrapped-routes
                                            all-routes
                                            {:port http-port :join? false})]
                              [(:local-port (meta stop-fn))
                               (fn [] (stop-fn :timeout 100))])
        uri (format "http://localhost:%s/" port-used)]

    (infof "http-port: %s" http-port)
    (infof "Web server is running at: %s" uri)
    (try
      (.browse (java.awt.Desktop/getDesktop) (java.net.URI. uri))
      (catch java.awt.HeadlessException _))

    (reset! web-server_ stop-fn)))


