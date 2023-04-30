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
                          :url (gobj/get val "url")})))))))


(defn query-full-github-user [user]
  (-> (fetch/get (:url user) {:accept :json :content-type :json})
      (.then (fn [resp] (-> resp :body (js->clj :keywordize-keys true))))
      (.then (fn [user]
               (reset! full-user user)))))


(defn query-pubkey-from-github [user]
  (-> (fetch/get (str (:url user) "/keys") {:accept :json :content-type :json})
      (.then (fn [resp] (-> resp :body (js->clj :keywordize-keys true))))
      (.then (fn [keys] (:key (first keys))))
      (.then (fn [key]
               (when key
                 (reset! pubkey (str key " github.com/" (:login user))))))))


(defn get-pubkeys [user]
  (let [path (str (:home user) "/.ssh/authorized_keys")]
    (-> (js/cockpit.file path)
        (.read)
        (.then (fn [content tag]
                 content))
        (.catch (fn [error]
                 (js/console.log "Error:")
                 (js/console.log error))))))


(defn current-user []
  (-> (js/cockpit.user)
      (.then (fn [user]
               (js->clj user :keywordize-keys true)))))


(defn close-modal-profile []
  (reset! dialog-for-user nil)
  (reset! full-user nil)
  (reset! pubkey nil))


(defn append-pubkey [key]
  (-> (current-user)
      (.then (fn [user]
         (let [path (str (:home user) "/.ssh/authorized_keys")]
           (-> (js/cockpit.file path)
               (.modify (fn [content] (str/join "\n" [content key])))
               (.then (fn [&args] (close-modal-profile)))
               (.catch (fn [error]
                         (js/console.log "Error:")
                         (js/console.log error)))))))))


(defn delete-pubkey [key]
  (-> (current-user)
      (.then (fn [user]
         (let [path (str (:home user) "/.ssh/authorized_keys")]
           (-> (js/cockpit.file path)
               (.modify (fn [content] (str/replace content key "")))
               (.then (fn [&args] (close-modal-profile)))
               (.catch (fn [error]
                         (js/console.log "Error:")
                         (js/console.log error)))))))))


(defn parse-pubkeys [text]
  (str/split text #"\n"))


(defn load-pubkeys []
  (-> (current-user)
      (.then get-pubkeys)
      (.then parse-pubkeys)
      (.then (fn [keys]
               (reset! authorized-keys keys)))))


(defn render-form []
  [:div {:class "pf-c-toolbar"}
   [:div {:class "pf-c-toolbar__content accounts-toolbar-header"}
    [:div {:class "pf-c-toolbar__content-section"}
     [:div {:class "pf-c-toolbar__item"}
      [:div {:class "pf-c-text-input-group"}

       [:div {:class "pf-c-text-input-group__main pf-m-icon"}
        [:span {:class "pf-c-text-input-group__text"}
         [:span {:class "pf-c-text-input-group__icon"}
          [:i {:class "fas fa-search", :aria-hidden "true"}]]
         [:input {:type "text"
                  :id "search"
                  :class "pf-c-text-input-group__text-input"
                  :name "login"
                  :placeholder "Search your GitHub account"
                  :value @login
                  :on-change #(reset! login (.-value (.-target %)))
                  :on-key-press (fn [e] (when (= (.-key e) "Enter")
                                          (search-github-user @login)))}]]]]]

     [:div {:class "pf-c-toolbar__item"}
      [:button {:class "pf-c-button pf-m-primary"
                :type "button"
                :on-click #(search-github-user @login)}
       "Search"]]]
    [:div {:class "pf-c-toolbar__expandable-content"}
     [:div {:class "pf-c-toolbar__group"}]]]
   [:div {:class "pf-c-toolbar__content pf-m-hidden"}
    [:div {:class "pf-c-toolbar__group pf-m-hidden"}]]])


(defn render-code-block [code]
  [:div {:class "pf-c-code-block"}
   [:div {:class "pf-c-code-block__header"}
    [:div {:class "pf-c-code-block__actions"}

     [:div {:class "pf-c-code-block__actions-item"}
      [:button {:class "pf-c-button pf-m-plain"
                :type "button"
                :aria-label "Copy to clipboard"}
       [:i {:class "fas fa-copy", :aria-hidden "true"}]]]

     [:div {:class "pf-c-code-block__actions-item"}
      [:button {:class "pf-c-button pf-m-plain"
                :type "button"}
       [:i {:class "fas fa-key"}]]]]]

   [:div {:class "pf-c-code-block__content"}
    [:pre {:class "pf-c-code-block__pre"}
     [:code {:class "pf-c-code-block__code"} code]]]])


(defn li-with-icon [icon text]
  [:li {:class "pf-c-list__item"}
   [:span {:class "pf-c-list__item-icon "}
    [:i {:class icon}]]
   [:span {:class "pf-c-list__item-text"} text]])


;; https://api.github.com/users/FrostyX
(defn render-modal-profile-body []
  (when (not @full-user)
    (query-full-github-user @dialog-for-user))

  (when (not @pubkey)
    (query-pubkey-from-github @dialog-for-user))

  (let [user @full-user]
    [:div {:class "pf-l-grid"}
     [:div {:class "pf-l-grid__item pf-m-3-col"}
      [:img {:src (:avatar_url user) :width 170 :height 170}]]
     [:div {:class "pf-l-grid__item pf-m-9-col"}
      [:p {:class "pf-u-font-size-2xl"} (:name user)]
      [:p {:class "pf-u-font-size-xl"}
       [:a {:href (:html_url user) :target "_blank"} (:login user)]]

      [:div {:id "following"}
       [:ul {:class "pf-c-list pf-m-plain"}
        [:li
         [:i {:class "fas fa-user-friends pf-u-color-200"}]
         " " (:followers user) [:span {:class "pf-u-color-200"} " followers · "]]
        [:li (:following user) [:span {:class "pf-u-color-200"} " following · "]]
        [:li (:public_repos user) [:span {:class "pf-u-color-200"} " repos · "]]
        [:li (:public_gists user) [:span {:class "pf-u-color-200"} " gists"]]]]

      [:div {:id "locations"}
       [:ul {:class "pf-c-list pf-m-plain"}
        (li-with-icon "fas fa-building fa-fw" (:company user))
        (li-with-icon "fas fa-map-marker fa-fw" (:location user))
        (li-with-icon "fas fa-external-link-alt fa-fw"
                      [:a {:href (:blog user) :target "_blank"} (:blog user)])]]]

     [:div {:id "pubkey"}
      (render-code-block @pubkey)]]))


(defn render-modal-profile []
  (when @dialog-for-user
    [:div {:class "pf-c-backdrop"}
     [:div {:class "pf-l-bullseye"}
      [:div {:class "pf-c-modal-box pf-m-md"
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
        (render-modal-profile-body)

        (when (some #{@pubkey} @authorized-keys)
          [:div {:id "known-key"}
           [:p {:class "pf-u-danger-color-100"}
            (str "This public key was already found in authorized keys. "
                 "Do you want to delete it?")]])]

       [:footer {:class "pf-c-modal-box__footer"}

        (if (some #{@pubkey} @authorized-keys)
          [:button {:class "pf-c-button pf-m-danger"
                    :type "button"
                    :on-click #(delete-pubkey @pubkey)}
           "Delete"]

          [:button {:class "pf-c-button pf-m-primary"
                    :type "button"
                    :on-click #(append-pubkey @pubkey)}
           "Authorize"])

        [:button {:class "pf-c-button pf-m-link"
                  :type "button"
                  :on-click #(close-modal-profile)}
         "Cancel"]]]]]))

(defn pubkey-already-enabled? [user]
  (some #(str/ends-with? % (str "github.com/" (:login user))) @authorized-keys))


(defn render-table []
  [:table {:role "grid"
           :class "ct-table pf-c-table pf-m-grid-md pf-m-compact"}
   [:thead
    [:tr
     [:th {:scope "col"} "Avatar"]
     [:th {:scope "col"} "Username"]
     [:th {:scope "col"} "GitHub URL"]
     [:th {:scope "col"} "Enable"]]]
   [:tbody {:role "rowgroup"}
    (for [user @users]
      [:tr
       [:td {} [:img {:src (:avatar_url user) :width 50 :height 50}]]
       [:td {} [:a {:href (:html_url user) :target "_blank"} (:login user)]]
       [:td {} [:a {:href (:html_url user) :target "_blank"} (:html_url user)]]
       [:td {}
        (if (pubkey-already-enabled? user)
            [:button {:class "pf-c-button pf-m-secondary pf-m-danger"
                      :on-click #(reset! dialog-for-user user)}
             "Deny this user"]

            [:button {:class "pf-c-button pf-m-secondary"
                      :on-click #(reset! dialog-for-user user)}
             "Allow this user"])]])]])


(defn home-page []
  (load-pubkeys)
  [:div
   (render-modal-profile)
   (render-form)
   (render-table)])


;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))
