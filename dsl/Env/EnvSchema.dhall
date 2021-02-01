{-
    Base schema for Unipipe service broker configuration
-}

let GitAccess = ./GitAccess.dhall
let AppConfig = { basic-auth-password : Text, basic-auth-username : Text }
let ServerCofig = { port : Natural }

in 
{ app : AppConfig
, git : GitAccess 
, server : ServerCofig
}