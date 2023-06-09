# Cockpit plugin - SSH Keys

TODO Rename required, there is already SSH key management in Cockpit,
see Accounts > select your user > Authorized public SSH keys.

This Cockpit plugin allows you to search GitHub users and authorize
their public SSH keys with just one click.

In the future, I would like to support searching in FAS as well, but
it requires authorization for any kind of request, so I don't know how.

## Screenshots

Search your GitHub username

![Screenshot](screenshots/search.png)

Pick the correct account

![Screenshot](screenshots/table.png)

Authorize your public key

![Screenshot](screenshots/modal.png)


## Project structure

At least before I do some cleanup, the project is a bit messy:

- `manifest.json` - Entrypoint for Cockpit
- `core.cljs` - The main Clojurescript code
- `package.json` - Javascript dependencies
- `shadow-cljs.edn` - Defines targets for `npx` commands, see below


## Development

Install Javascript dependencies

```
npm install
```

Compile CSS files from Sass

```
make css
```

Build and see changes in your browser

```
npx shadow-cljs watch app
# or
npx shadow-cljs compile app
```

You might want REPL

```
npx shadow-cljs browser-repl
```

## Building for Cockpit

The build results from previous commands work as standalone website
but not as a Cockpit plugin. For that, we need to do:

```
npx shadow-cljs release app
```

Adding this into Cockpit is easy

```
ln -s ~/git/cockpit-ssh-keys ~/.local/share/cockpit/cockpit-ssh-keys
```

Now you should be able to see http://127.0.0.1:9090/cockpit-ssh-keys/ssh-keys

This process is of course meant only for developers. Once this plugin
is finished, it will be packaged as an RPM and installable via
Cockpit's Application page.
