;; Copyright © App Sauce, LLC
;;
;; All rights reserved. This program and the accompanying materials are made
;; available under the terms of the Eclipse Public License v2.0 which
;; accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v20.html

(ns app-sauce.async.worker
  (:require
    [clojure.core.async :as async]))


(defrecord Inject [value prom])

(defn unwrap
  [msg]
  (when (some? msg)
    (if (instance? Inject msg)
      msg
      {:value msg})))


(defn start
  "Start a worker and return associated core.async channels. Work is received
  on an 'input' channel (`:in`) and results are sent to an 'output' channel
  (`:out`). See the `dispatch` and `inject` functions for different ways of
  interacting with the worker.

  The caller can provide the output channel by specifying `outbox`. If there is
  no output consumer, pass nil for the outbox, so the worker will not get stuck
  attempting to send output.

  The work is performed by the callback function. That function will be
  provided two arguments: the current version of `data` and the value that was
  sent to the input channel. The callback function must return a vector:
  `nil`                        no change in `data` and no output
  `[new-data]`                 new value for `data` but no output
  `[new-data output-value]     new value for `data` and a value to output
  `[new-data output1 output2]  new value for `data` and multiple output values

  Additionally, when `inject` is used, the first value after `data` will be the
  result returned to the promise:
  `[new-data result]              a result for `inject` but no output
  `[new-data result output1 ...]  a result for `inject` along with output values

  The initial value of `data` should be passed as the `initial-data` parameter."
  ([name-str initial-data callback]
   (start name-str initial-data callback (async/chan)))

  ([name-str initial-data callback outbox]
   (let [inbox (async/chan)]
     (async/thread
       (.setName ^Thread (Thread/currentThread) (str "worker-" name-str))
       (loop [data initial-data]
         (when-some [{:keys [value prom]} (unwrap (async/<!! inbox))]
           (let [[new-data & results :as ret-val] (try (callback data value) (catch Throwable e [data e]))]
             (when outbox
               (doseq [msg (->> (if prom (rest results) results)
                                (filter some?))]
                 (async/>!! outbox msg)))
             (when prom
               (deliver prom (first results)))
             (if (some? ret-val)
               (recur new-data)
               (recur data)))))
       (when outbox
         (async/close! outbox)))
     (cond-> {:name name-str :in inbox}
       outbox (assoc :out outbox)))))


(defn stop!
  "Stops the worker thread and closes the associated channels."
  [this]
  (async/close! (:in this)))


(defn- wrap-value [value]
  [value])

(defn dispatch
  "Sends a message to the worker. May block. Returns true if the value could be
  delivered to the worker's input channel."
  [this value]
  (async/>!! (:in this) value))

(defn inject
  "Returns a promise with the result from the worker. May block. The value of
  the promise will be nil if the worker has already stopped."
  [this value]
  (let [prom (promise)]
    (when (async/>!! (:in this) (Inject. value prom))
      prom)))
