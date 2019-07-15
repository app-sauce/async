;; Copyright © App Sauce, LLC
;;
;; All rights reserved. This program and the accompanying materials are made
;; available under the terms of the Eclipse Public License v2.0 which
;; accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v20.html

(ns app-sauce.async.timer
  (:require
    [clojure.core.async :as async :refer [>! <!]]))


(defn every
  "Repeatedly sends a message to the channel with a delay of ms milliseconds
  between each message. Time blocked waiting to send does not reduce the
  subsequent delay. The timer will stop automatically when the channel is
  closed."
  [chan ms msg]
  (async/go
    (loop []
      (<! (async/timeout ms))
      (when (>! chan msg)
        (recur)))))


(defn once
  "Send a message to the channel after waiting ms milliseconds."
  [chan ms msg]
  (async/go
    (<! (async/timeout ms))
    (>! chan msg)))
