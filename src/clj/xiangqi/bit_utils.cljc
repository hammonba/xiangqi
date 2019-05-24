(ns xiangqi.bit-utils)

(defn bigint-bitshift-left
  "like bit-shift-left but for bigint."
  [x n]
  (loop [acc x n n]
    (if (zero? n)
      acc
      (let [nn (min n 62)]
        (recur (*' acc (bit-shift-left 1 nn)) (- n nn))))))

(defn bigint-bitshift-add-ignore-twos-complement
  "If left-most bit is 1, then number is negative
   and a simple add will not work; we have to
   - strip out the leading bit
   - bitshift left seperately
   - add the remainder of n"
  [acc [bitcount n]]
  (let [mask (bit-shift-left 1 (dec bitcount))]
    (-> acc
      (bigint-bitshift-left 1)
      inc
      (bigint-bitshift-left (dec bitcount))
      (+ (bit-and-not n mask)))))

(defn bigint-bitshift-add
  [acc [bitcount n :as nv]]
  (if (neg? n)
    (bigint-bitshift-add-ignore-twos-complement acc nv)
    (+ (bigint-bitshift-left acc bitcount) n)))

(defn encode-shortsseq-as-long
  "takes a sequence of shorts, encodes them within a long"
  [bitcount sseq]
  (reduce
    (fn [[bitcounter acc] v] [(+ bitcounter bitcount)
                              (+ (bit-shift-left acc bitcount) v)])
    [0 0]
    sseq))

(def encoding-vec (vec "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ"))
(def decoding-map (zipmap encoding-vec (range)))

(defn encodebigint-as-string
  "expresses bigint an an alphanumeric string.
   This converts the number into base-62, thereby shortening it"
  ([n] (encodebigint-as-string encoding-vec n))
  ([encoding-vec large-number]
   (let [radix (count encoding-vec)]
     (transduce
       (comp (take-while pos?)
         (map #(rem % radix))
         (map encoding-vec))
       (completing #(.append ^StringBuilder %1 ^char %2) str)
       (StringBuilder.)
       (iterate #(quot % radix) large-number)))))

(defn decodestring-into-bigint
  [s]
  (when s
    (let [radix (count decoding-map)]
      (transduce
        (map decoding-map)
        (completing (fn [acc v]
                      (+ v (* acc radix))))
        0N
        (reverse s)))))