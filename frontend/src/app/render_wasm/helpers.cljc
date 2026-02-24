;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.render-wasm.helpers
  #?(:cljs (:require-macros [app.render-wasm.helpers])))

(def ^:private error-code
  "WASM error code constants (must match render-wasm/src/error.rs and mem.rs)."
  {:ok 0x00 :recoverable 0x01 :critical 0x02 :panic 0xFF})

(defmacro call
  "A helper for easy call wasm defined function in a module.
   Catches any exception thrown by the WASM function, reads the error code from
   WASM when available, and rethrows ex-info with a message based on the code."
  [module name & params]
  (let [fn-sym   (with-meta (gensym "fn-") {:tag 'function})
        e-sym    (gensym "e")
        code-sym (gensym "code")
        rec      (:recoverable error-code)
        crit     (:critical error-code)]
    `(let [~fn-sym (cljs.core/unchecked-get ~module ~name)]
       (try
         (~fn-sym ~@params)
         (catch :default ~e-sym
           (let [read-code# (cljs.core/unchecked-get ~module "_read_error_code")
                 ~code-sym (when read-code# (read-code#))
                 msg#      (cond
                             (= ~code-sym ~rec) "WASM error (recoverable)"
                             (= ~code-sym ~crit) "WASM error (critical)"
                             :else               "WASM error (critical)")]
             (throw (ex-info msg#
                             {:fn ~name
                              :message (.-message ~e-sym)
                              :error-code ~code-sym}
                             ~e-sym))))))))
