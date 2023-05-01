css:
	npx sass --load-path=node_modules --style compressed \
		public/css/style.scss:public/css/style.css

build:
	npx shadow-cljs release app
