{-
    Sample unipipe service broker configuration
    using ssh-key method to access the git repo
    
    Application configuration
    basic-auth-username :
        The service broker API itself is secured via HTTP Basic Auth.
        The basic auth username for requests against the API.
    basic-auth-password :
        The basic auth password for requests against the API.

    Git access configuration
         
         SSH configuration :
            local : 
                The path where the local Git Repo shall be created/used. Defaults to tmp/git
            remote : 
                The remote Git repository to push the repo to
            ssh-key : 
                This is the SSH key to be used for accessing the remote repo. Linebreaks must be replaced with spaces as sample shown below
                -----BEGIN RSA PRIVATE KEY----- Hgiud8z89ijiojdobdikdosaa+hnjk789hdsanlklmladlsagasHOHAo7869+bcG x9tD2aI3...ysKQfmAnDBdG4= -----END RSA PRIVATE KEY-----
    
    Server configuration 
    port : port to used by the application
    
-}
let Config = ./AppSchema.dhall

let GitAccess = ./GitAccess.dhall

let ExampleSsh
    : Config
    = { app = { basic-auth-username = "user", basic-auth-password = "password" }
      , git =
          GitAccess.SSH
            { local = "local path", remote = "remote path", ssh-key = "-----BEGIN RSA PRIVATE KEY----- Hgiud8z89ijiojdobdikdosaa+hnjk789hdsanlklmladlsagasHOHAo7869+bcG x9tD2aI3...ysKQfmAnDBdG4= -----END RSA PRIVATE KEY-----" }
      , server.port = 8075
      }

in  ExampleSsh
