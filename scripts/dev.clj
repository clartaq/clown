(require
  '[figwheel.main.api :as fw]
  '[clown.server.main :refer [dev-main]])

(fw/start {:mode :serve} "dev")
(dev-main)
