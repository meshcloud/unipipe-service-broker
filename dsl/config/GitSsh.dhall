-- Git configuration schema to access the git repo via ssh
{-
    SSH configuration to access git repository
    local : The path where the local Git Repo shall be created/used. Defaults to tmp/git
    remote : The remote Git repository to push the repo to
    ssh-key : This is the SSH key to be used for accessing the remote repo. Linebreaks must be replaced with spaces
-}
{ local : Text, remote : Text, ssh-key : Text }
