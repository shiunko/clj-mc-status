(ns clj-mc-status.core
  (:import (java.net Socket)
           (java.io InputStream ByteArrayInputStream)
           (java.util Base64)
           (java.awt.image ColorConvertOp)
           (java.awt.color ColorSpace)
           (javax.imageio ImageIO))
  (:require
    [cheshire.core :refer :all]))

(defn read-var-int
  [^InputStream in]
  (loop [i 0
         data 0]
    (if-not (< i 5)
      data
      (let [ret-code (.read in)
            data (bit-or data (bit-shift-left (bit-and ret-code 0x7F) (* 7 i)))]
        (if (= ret-code -0x1)
          -1
          (if (not= (bit-and ret-code 0x80) 128)
            data
            (recur (inc i) data)))))))


(defn get-server-info
  [{:keys [^String host ^Integer port]}]
  (let [server (Socket. host port)
        out (.getOutputStream server)
        in (.getInputStream server)]
    (doto out
      (.write (int (+ 6 (.length host))))
      (.write 0)
      (.write 4)
      (.write (.length host))
      (.write (.getBytes host))
      (.write (bit-shift-right (bit-and port 0xFF00) 8))
      (.write (bit-and port 0x00FF))
      (.write 1)
      (.write 0x01)
      (.write 0x00))
    (let [packet-len (read-var-int in)]
      (if (< packet-len 11)
        (do
          (.close in)
          (.close out)
          (.close server)
          (format "packet length too small: %d" packet-len))
        (let [packet-type (.read in)
              json-len (read-var-int in)]
          (println (format "packet type: %d, with json size: %d." packet-type json-len))
          (if (< json-len 0)
            (do
              (.close in)
              (.close out)
              (.close server)
              (format "json length too small: %d" json-len))
            (let [buffer (byte-array (+ json-len 10))
                  bytes-read (loop [bytes-read 0]
                               (let [len (.read in buffer bytes-read (- json-len bytes-read))]
                                 (if-not (< bytes-read json-len)
                                   bytes-read
                                   (recur (+ bytes-read len)))))
                  data (String. buffer 0 bytes-read)]
              (.close in)
              (.close out)
              (.close server)
              (parse-string data true)
              ))
          ))
      )
    )
  )

(defn prn-pic
  [^String base64-pic]
  (let [decoder (Base64/getDecoder)
        pic (ImageIO/read (ByteArrayInputStream. (-> decoder (.decode base64-pic))))
        cs (ColorSpace/getInstance ColorSpace/CS_GRAY)
        op (ColorConvertOp. cs nil)
        gray-pic (-> op (.filter pic nil))
        w (.getWidth gray-pic)
        h (.getHeight gray-pic)
        threshold 127]
    (loop [i 0]
      (println "")
      (if (< i w)
        (do
          (loop [j 0]
            (if (< j h)
              (let [rgb (.getRGB gray-pic i j)
                    gray (bit-and (bit-shift-right rgb 16) 0xFF)]
                (if (< gray threshold)
                  (print "*")
                  (print " "))
                (recur (inc j))
                )))
          (recur (inc i)))))

    )
  )

(defn -main
  [& args]
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
      (catch Exception e
        (println server-info)
        ))
    ;(prn-pic favicon)
    )
  )