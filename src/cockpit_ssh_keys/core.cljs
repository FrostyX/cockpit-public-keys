(ns cockpit-ssh-keys.core
  (:require-macros [cljs.core.async.macros :refer [go]])
    (:require
     [clojure.string :as str]
     [reagent.core :as r]
     [reagent.dom :as d]
     [lambdaisland.fetch :as fetch]
     [goog.object :as gobj]))


;; https://api.github.com/search/users?q=frostyx
(def users
  (r/atom [{:login "FrostyX"
            :avatar_url "https://avatars.githubusercontent.com/u/2151771?v=4"
            :html_url "https://github.com/FrostyX"
            :url "https://api.github.com/users/FrostyX"}
           {:login "frostyx-vk"
            :avatar_url "https://avatars.githubusercontent.com/u/115023317?v=4"
            :html_url "https://github.com/frostyx-vk"
            :url "https://api.github.com/users/frostyx-vk"}
           {:login "Frostyx0"
            :avatar_url "https://avatars.githubusercontent.com/u/56936540?v=4"
            :html_url "https://github.com/Frostyx0"
            :url "https://api.github.com/users/Frostyx0"}
           {:login "Frostyxnova"
            :avatar_url "https://avatars.githubusercontent.com/u/90808223?v=4"
            :html_url "https://github.com/Frostyxnova"
            :url "https://api.github.com/users/Frostyxnova"}]))


;; Searched login
(def login (r/atom ""))

;; List of authorized keys for the current user
(def authorized-keys (r/atom []))

(def dialog-for-user (r/atom nil))
(def full-user (r/atom nil))
(def pubkey (r/atom nil))


(defn search-github-user [login]
  (-> (fetch/get (str "https://api.github.com/search/users?q=" login)
                 {:accept :json :content-type :json})
      (.then (fn [resp] (-> resp :body (gobj/get "items"))))
      (.then (fn [items]
               (js/console.log items)
               (gobj/forEach
                items
                (fn [val key items]
                  (swap! users conj
                         {:login (gobj/get val "login")
                          :avatar_url (gobj/get val "avatar_url")
                          :html_url (gobj/get val "html_url")
                          :url (gobj/get val "html_url")})))))))


(defn query-full-github-user [user]
  (-> (fetch/get (:url user) {:accept :json :content-type :json})
      (.then (fn [resp] (-> resp :body (js->clj :keywordize-keys true))))
      (.then (fn [user]
               (reset! full-user user)))))


(defn query-pubkey-from-github [user]
  (-> (fetch/get (str (:url user) "/keys") {:accept :json :content-type :json})
      (.then (fn [resp] (-> resp :body (js->clj :keywordize-keys true))))
      (.then (fn [keys]
               (reset! pubkey (:key (first keys)))))))


(defn get-pubkeys [user]
  (let [path (str (:home user) "/.ssh/authorized_keys")]
    (-> (js/cockpit.file path)
        (.read)
        (.then (fn [content tag]
                 content))
        (.catch (fn [error]
                 (js/console.log "Error:")
                 (js/console.log error))))))


(defn parse-pubkeys [text]
  (str/split text #"\n"))


(defn current-user []
  (-> (js/cockpit.user)
      (.then (fn [user]
               (js->clj user :keywordize-keys true)))))


(defn load-pubkeys []
  (-> (current-user)
      (.then get-pubkeys)
      (.then parse-pubkeys)
      (.then (fn [keys]
               (reset! authorized-keys keys)))))


(defn render-form []
  [:form {}
   [:input {:type "text" :placeholder "Search your GitHub account"
            :name "login" :value @login
            :on-change #(reset! login (.-value (.-target %)))}]
   [:input {:type "button" :value "CLICK"
            :on-click #(search-github-user @login)}]])


;; https://api.github.com/users/FrostyX
(defn render-modal-profile-body []
  (when (not @full-user)
    (query-full-github-user @dialog-for-user))

  (when (not @pubkey)
    (query-pubkey-from-github @dialog-for-user))

  (let [user @full-user]
    [:div
     [:img {:src (:avatar_url user) :width 150 :height 150}]
     [:p (:name user)]
     [:p [:i {:class ""}] (:login user)]
     [:p [:i {:class "pf-icon pf-icon-enterprise"}] (:company user)]
     [:p [:i {:class "fas fa-external-link-alt"}] (:blog user)]
     [:p [:i {:class "fas fa-map-marker"}] (:location user)]
     [:p [:i {:class ""}] (:login user)]
     [:p [:i {:class "pf-icon pf-icon-repository"}] (:public_repos user)]
     [:p (:public_gists user)]
     [:p [:i {:class "fas fa-user-friends"}] (:followers user)]
     [:p (:following user)]
     [:p [:i {:class "fas fa-key"}] @pubkey]
     [:p (:html_url user)]]))


(defn close-modal-profile []
  (reset! dialog-for-user nil)
  (reset! full-user nil)
  (reset! pubkey nil))


(defn render-modal-profile []
  (when @dialog-for-user
    [:div {:class "pf-c-backdrop"}
     [:div {:class "pf-l-bullseye"}
      [:div {:class "pf-c-modal-box pf-m-sm"
             :role "dialog"
             :aria-modal "true"
             :aria-labelledby "modal-title-modal-basic-example-modal"
             :aria-describedby "modal-description-modal-basic-example-modal"}

       [:header {:class "pf-c-modal-box__header"}
        [:h1 {:class "pf-c-modal-box__title"
              :id "modal-title-modal-basic-example-modal"}
         "Authorize this user?"]]
       [:div {:class "pf-c-modal-box__body"
              :id "modal-description-modal-basic-example-modal"}
        (render-modal-profile-body)]

       [:footer {:class "pf-c-modal-box__footer"}
        [:button {:class "pf-c-button pf-m-primary" :type "button"} "Add SSH key"]
        [:button {:class "pf-c-button pf-m-link"
                  :type "button"
                  :on-click #(close-modal-profile)}
         "Cancel"]]]]]))


(defn render-table []
  [:table {:role "grid"
           :class "ct-table pf-c-table pf-m-grid-md pf-m-compact"}
   [:thead
    [:tr
     [:th {:scope "col"} "Avatar"]
     [:th {:scope "col"} "Username"]
     [:th {:scope "col"} "Enable"]
     [:th {:scope "col"} "GitHub URL"]]]
   [:tbody {:role "rowgroup"}
    (for [user @users]
      [:tr
       [:td {} [:img {:src (:avatar_url user) :width 50 :height 50}]]
       [:td {} [:a {:href (:html_url user)} (:login user)]]
       [:td {} [:button {:on-click #(reset! dialog-for-user user)}
                "Allow this user"]]
       [:td {} [:a {:href (:html_url user)} (:html_url user)]]])]])


(defn home-page []
  (load-pubkeys)

  [:div [:h2 "Welcome to Reagent"]
   (render-modal-profile)
   (render-form)
   (render-table)])


;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))
