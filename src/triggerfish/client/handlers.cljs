(ns triggerfish.client.handlers
  (:require [re-frame.core :refer [register-handler]]
            [triggerfish.shared.object-definitions :as obj]
            [triggerfish.client.sente-events :refer [chsk-send!]]))

(def initial-state
  {:objects {}
   :connections {}
   :positions {}
   :sidebar-open true
   :mode :insert
   :selected-outlet [nil nil]
   })

(register-handler
 :initialize
 (fn
   [db _]
   (merge db initial-state)))

(register-handler
 :patch-recv
 (fn [db [ev-id patch]]
   (assoc db :objects (dissoc patch :connections) :connections (:connections patch))))

;;Updates the app-db with the DOM position of an inlet or outlet
(register-handler
 :update-io-position
 (fn [db [ev-id obj-id inlet-or-outlet-name pos]]
   (assoc-in db [:positions [obj-id inlet-or-outlet-name]] pos)))

(register-handler
 :update-object-position
 (fn [db [ev-id obj-id pos]]
   (assoc-in db [:positions obj-id] pos)))

(register-handler
 :dissoc-position
 (fn [db [ev-id obj-id inlet-or-outlet-name]]
   (update-in db [:positions] dissoc [obj-id inlet-or-outlet-name])))

(register-handler
 :move-object
 (fn [db [ev-id obj-id x y]]
   (-> db
    (assoc-in [:objects obj-id :x-pos] x)
    (assoc-in [:objects obj-id :y-pos] y))))

(register-handler
 :select-create-object
 (fn [db [ev-id name]]
   (assoc db :selected-create-object name)))

(register-handler
 :optimistic-create
 (fn [db [ev-id obj-name x-pos y-pos]]
   ;;todo - guarantee id is unique
   (let [id (str "ghost" (rand-int 99999))]
     (assoc-in db [:objects id]
              (merge (obj-name obj/objects)
                     {:id id
                      :x-pos x-pos
                      :y-pos y-pos
                      :name obj-name
                      :optimistic true})))))

(defn get-connected-inlets
  [connections obj-id]
  (filter #(= obj-id (first (first %))) connections))

(defn get-connected-outlets
  [connections obj-id]
  (filter #(= obj-id (first (second %))) connections))

(register-handler
 :optimistic-delete
 (fn [db [ev-id obj-id]]
   (let [connections (:connections db)
         connections-to-remove (keys (merge (into {} (get-connected-inlets connections obj-id))
                                            (into {} (get-connected-outlets connections obj-id))))]
     (-> (reduce #(update-in %1 [:connections] dissoc %2) db connections-to-remove)
         (update-in [:objects] dissoc obj-id)))))

(register-handler
 :connect
 (fn [db [ev-id inlet-id inlet-name type]]
   (let [selected-outlet (:selected-outlet db)
         [outlet-id outlet-name outlet-type] selected-outlet]
     (if (and (not (nil? selected-outlet)) (= type outlet-type))
       (do
         (chsk-send! [:patch/connect {:in-id inlet-id :in-name inlet-name :out-id outlet-id :out-name outlet-name}])
         (assoc-in db [:connections [inlet-id inlet-name]] [outlet-id outlet-name]))
       (if (not (nil? (get (:connections db) [inlet-id inlet-name])))
           (do
             (chsk-send! [:patch/disconnect {:in-id inlet-id :in-name inlet-name}])
             (update-in db [:connections] dissoc [inlet-id inlet-name]))
           db)))))

(register-handler
 ;;select an outlet
 :select-outlet
 (fn [db [ev-id [id name type]]]
   (let [prev-outlet (:selected-outlet db)]
     (println prev-outlet)
     (if (not (= prev-outlet [id name type]))
       (assoc db :selected-outlet [id name type])
       (dissoc db :selected-outlet)))))

(register-handler
 :select-object
 (fn [db [ev-id obj-id]]
   (if (not (= obj-id (first (:selected-outlet db))))
     (assoc db :selected-outlet [obj-id nil nil])
     db)))

(register-handler
 :deselect-outlet
 (fn [db [ev-id [id name type]]]
   (dissoc db :selected-outlet)))

(register-handler
 :min-max
 (fn [db [ev-id obj-id current-val]]
   (assoc-in db [:minimized obj-id] (not current-val))))

(register-handler
 :set-mode
 (fn [db [ev-id mode]]
   (assoc db :mode mode)))

(register-handler
 :open-sidebar
 (fn [db [ev-id]]
   (assoc db :sidebar-open true)))

(register-handler
 :close-sidebar
 (fn [db [ev-id]]
   (assoc db :sidebar-open false)))

(register-handler
 :update-patch-size
 (fn [db [ev-id width height]]
   (assoc db :patch-size [width height])))
