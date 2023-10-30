(ns ig.command-executor
  (:require
   [ig.rest :as rest]
   [core.command :refer [CommandExecutor]]))

(deftype IgCommandExecutor [client]
  CommandExecutor
  (open-order! [this order]
    (client (rest/open-order order)))

  (close-order! [this order]
    (client (rest/close-order order))))
