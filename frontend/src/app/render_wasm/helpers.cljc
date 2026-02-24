;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.render-wasm.helpers
  #?(:cljs (:require-macros [app.render-wasm.helpers])))

(def ^:export error-code
  "WASM error code constants (must match render-wasm/src/error.rs and mem.rs)."
  {0x01 :wasm-non-blocking 0x02 :wasm-critical})

(defmacro call
  "A helper for easy call wasm defined function in a module.
   Catches any exception thrown by the WASM function, reads the error code from
   WASM when available, and rethrows ex-info with :type (:wasm-non-blocking or
   :wasm-critical) in ex-data. The uncaught-error-handler in app.main.errors
   routes these to on-error so critical shows Internal Error, non-blocking shows toast."
  [module name & params]
  (let [fn-sym   (with-meta (gensym "fn-") {:tag 'function})
        e-sym    (gensym "e")
        code-sym (gensym "code")]
    `(let [~fn-sym (cljs.core/unchecked-get ~module ~name)]
       (try
         (~fn-sym ~@params)
         (catch :default ~e-sym
           (let [read-code# (cljs.core/unchecked-get ~module "_read_error_code")
                 ~code-sym (when read-code# (read-code#))
                 type#    (or (get app.render-wasm.helpers/error-code ~code-sym) :wasm-critical)
                 ex#      (ex-info (str "WASM error (type: " type# ")")
                                  {:fn ~name :type type# :message (.-message ~e-sym) :error-code ~code-sym}
                                  ~e-sym)]
             (throw ex#)))))))
