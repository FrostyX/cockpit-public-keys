(ns cockpit-ssh-keys.core
  (:require-macros [cljs.core.async.macros :refer [go]])
    (:require
      [reagent.core :as r]
      [reagent.dom :as d]
      [lambdaisland.fetch :as fetch]
      [goog.object :as gobj]))


;; https://api.github.com/search/users?q=frostyx
(def users
  (r/atom [{:login "FrostyX"
            :avatar_url "https://avatars.githubusercontent.com/u/2151771?v=4"
            :html_url "https://github.com/FrostyX"}
           {:login "frostyx-vk"
            :avatar_url "https://avatars.githubusercontent.com/u/115023317?v=4"
            :html_url "https://github.com/frostyx-vk"}
           {:login "Frostyx0"
            :avatar_url "https://avatars.githubusercontent.com/u/56936540?v=4"
            :html_url "https://github.com/Frostyx0"}
           {:login "Frostyxnova"
            :avatar_url "https://avatars.githubusercontent.com/u/90808223?v=4"
            :html_url "https://github.com/Frostyxnova"}]))

;; Searched login
(def login (r/atom ""))


(defn search-github-user [login]
  (-> (fetch/get (str "https://api.github.com/search/users?q=" login)
                 {:accept :json :content-type :json})
      (.then (fn [resp] (-> resp :body (gobj/get "items"))))
      (.then (fn [items]
               (gobj/forEach
                items
                (fn [val key items]
                  (swap! users conj
                         {:login (gobj/get val "login")
                          :avatar_url (gobj/get val "avatar_url")
                          :html_url (gobj/get val "html_url")})))))))


(defn render-form []
  [:form {}
   [:input {:type "text" :placeholder "Search your GitHub account"
            :name "login" :value @login
            :on-change #(reset! login (.-value (.-target %)))}]
   [:input {:type "button" :value "CLICK"
            :on-click #(search-github-user @login)}]])


(defn render-table []
  [:table {:role "grid"
           :class "ct-table pf-c-table pf-m-grid-md pf-m-compact"}
   [:thead
    [:tr
     [:th {:scope "col"} "Avatar"]
     [:th {:scope "col"} "Username"]
     [:th {:scope "col"} "Enable"]]]
   [:tbody {:role "rowgroup"}
    (for [user @users]
    [:tr
     [:td {} [:img {:src (:avatar_url user) :width 50 :height 50}]]
     [:td {} [:a {:href (:html_url user)} (:login user)]]
     [:td {} "Allow this user"]])]])


(defn home-page []
  [:div [:h2 "Welcome to Reagent"]
   (render-form)
   (render-table)])


;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))
