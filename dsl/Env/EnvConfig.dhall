{-
    Sample unipipe service broker cofigration 
    using ssh-key method to access the git repo
-}

let Config = ./EnvSchema.dhall
let GitAccess = ./GitAccess.dhall
let ExampleSsh 
    : Config
    = {
        app = {basic-auth-username = "user", basic-auth-password ="password"}
        , git = GitAccess.SSH {local = "local path", remote = "remote path", ssh-key = "---"}
        , server.port = 8075
    }
in ExampleSsh
