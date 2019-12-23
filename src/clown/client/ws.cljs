(ns clown.client.ws

  "Functions to handle WebSocket connections.

  This is a very simple WebSocket interface handling only a single connection
  and only string data.

  Usage: Call 'start-ws!' with a map containing three functions to be used
  when the WebSocket is opened, an error occurs, or a message is received
  from the server. Returns a copy of the input map merged with a key and
  function that can be used to send messages to the server.

  When nil is used for an input argument, a default function is used instead.
  The default functions just log the event."

  )

(def ^{:private true} connection-atom (atom nil))

(defn- send-to-server
  "Send the string to the server."
  [s]
  (.send @connection-atom s))

(defn- default-on-open-fn
  "A default 'on-open' function if the caller does not supply one. Just prints
  a debug message that the WebSocket was opened."
  [e]
  (println "ws/default-on-open-fn")
  (println "WebSocket opened: e: " (str e)))

(defn- default-on-error-fn
  "A default 'on-error' function if the caller does not supply one. Just prints
  a log message that it was called."
  [e]
  (println "ws/default-on-error-fn: error data: " (.-data e)))

(defn- default-on-message-fn
  "A default 'on-message' function if the caller does not supply one. Just
  prints a log message with the contents of the message."
  [e]
  (println "ws/default-on-message-fn: message info: " (.-data e)))

(defn- start-connection!
  [m]
  (reset! connection-atom
          (js/WebSocket. (str "ws://" (.-host (.-location js/window)) "/ws")))
  (set! (.-onopen @connection-atom) (or (:on-open-fn m)
                                        default-on-open-fn))
  (set! (.-onerror @connection-atom) (or (:on-error-fn m)
                                         default-on-error-fn))
  (set! (.-onmessage @connection-atom) (or (:on-message-fn m)
                                           default-on-message-fn)))

(defn start-ws!
  "Start up the connection to the WebSocket. Return the original map of
  with the 'send-message' function merged into it."
  [m]
  (start-connection! m)
  (merge {:send-message-fn send-to-server} m))