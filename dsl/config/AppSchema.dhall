{-
    Base schema for Unipipe service broker configuration
    
    Application configuration
    basic-auth-username :
        The service broker API itself is secured via HTTP Basic Auth.
        The basic auth username for requests against the API.
    basic-auth-password :
        The basic auth password for requests against the API

    Git access configuration
        Union of git configurations,to give a choice to user to use HTTPS config or SSH config 

        SSH configuration :
            local : 
                The path where the local Git Repo shall be created/used. Defaults to tmp/git
            remote : 
                The remote Git repository to push the repo to
            ssh-key : 
                This is the SSH key to be used for accessing the remote repo. Linebreaks must be replaced with spaces
        
        HTTPS configuration :
            local : 
                The path where the local Git Repo shall be created/used. Defaults to tmp/git
            remote : 
                The remote Git repository to push the repo to
            username : 
                To access the git repo HTTPS username
            password : 
                To access the git repo HTTPS password

    Server configuration 
        port : port to used by the application

-}
let GitAccess = ./GitAccess.dhall

let AppConfig = { basic-auth-password : Text, basic-auth-username : Text }

let ServerCofig = { port : Natural }

in  { app : AppConfig, git : GitAccess, server : ServerCofig }
