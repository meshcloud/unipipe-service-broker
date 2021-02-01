-- Git configuration schema to access the git repo via https
{-
    HTTPS configuration to access git repository
    local : The path where the local Git Repo shall be created/used. Defaults to tmp/git
    remote : The remote Git repository to push the repo to
    username : To access the git repo HTTPS username
    password : To access the git repo HTTPS password
-}
{ local : Text, remote : Text, username : Text, password : Text }
