(ns clj-mc-status.core-test
  (:require [clojure.test :refer :all]
            [clj-mc-status.core :refer :all]))

(defn get-status-test
  []
  (let [server-info (get-server-info {:host "dns1.zthc.net"
                                      :port 40098})
        ;favicon (-> server-info :favicon (.replaceAll "data:image/png;base64," "") (.replaceAll "\n" ""))
        ]
    (try
      (println (format "server description: %s\nplayer[%d/%d]-%s\nmods%s"
                       (-> server-info :description :text)
                       (-> server-info :players :online)
                       (-> server-info :players :max)
                       (vec (for [info (-> server-info :players :sample)]
                              (:name info)))
                       (vec (for [mods (-> server-info :modinfo :modList)]
                              (format "%s-%s" (:modid mods) (:version mods))))
                       ))
      true
      (catch Exception e
        (println server-info)
        false
        ))
    ;(prn-pic favicon)
    )
  )

(deftest a-test
  (testing "FIXME, I fail."
    (is (get-status-test))))
