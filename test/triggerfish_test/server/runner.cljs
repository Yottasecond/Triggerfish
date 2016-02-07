(ns ^:figwheel-always triggerfish-test.server.runner
  (:require [cljs.test :refer-macros [run-tests]]
            [triggerfish-test.server.patch]
            [triggerfish-test.server.objects]))

(run-tests 'triggerfish-test.server.patch)
(run-tests 'triggerfish-test.server.objects)
