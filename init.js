// Init our ClojureScript code
cockpit_ssh_keys.core.init_BANG_();

// Send a 'init' message.  This tells integration tests that we are ready to go
cockpit.transport.wait(function() { });
