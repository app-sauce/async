;; Copyright © App Sauce, LLC
;;
;; All rights reserved. This program and the accompanying materials are made
;; available under the terms of the Eclipse Public License v2.0 which
;; accompanies this distribution, and is available at
;; http://www.eclipse.org/legal/epl-v20.html

(ns app-sauce.async.pipeline
  (:require
    [clojure.core.async :as async]
    [app-sauce.async.worker :as worker]
    ))


(defn start
  [workers]
  (reduce-kv #(assoc %1 %2 (update %3 :out async/mult)) {} workers))

(defn stop!
  [pipeline]
  (doseq [worker (vals pipeline)]
    (worker/stop! worker)))


(defn call
  [this worker msg]
  (some-> (get this worker)
          (worker/call msg)))

(defn dispatch
  [this worker msg]
  (some-> (get this worker)
          (worker/dispatch msg)))



(defn pipe
  ([from-mult to-chan]
   (async/tap from-mult to-chan))

  ([from-mult to-chan pred]
   (let [filtered (async/chan 1 (filter pred))]
     (pipe from-mult filtered)
     (async/pipe filtered to-chan))))


(defn consume!
  ([pipeline from to]
   (let [from-mult (some-> pipeline from :out)
         to-chan (if (keyword? to) (-> pipeline to :in) to)]
     (pipe from-mult to-chan)))

  ([pipeline from to pred]
   (let [from-mult (some-> pipeline from :out)
         to-chan (if (keyword? to) (-> pipeline to :in) to)]
     (pipe from-mult to-chan pred))))
