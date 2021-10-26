export const githubWorkflow = `
name: Update Instances
concurrency: osb-instances # signal that we're modifying instances

env:
  unipipe-version: v0.6.4
  # Use Github Repository Secrets to set your credentials.
  MY_SECRET_KEY: \${{ secrets.MY_SECRET_KEY }}

on:
  # run on any push to master
  push:
    branches:
      - "master"

# A workflow run is made up of one or more jobs that can run sequentially or in parallel
jobs:
  process-vnets:
    # The type of runner that the job will run on
    runs-on: ubuntu-latest
    steps:
      ### Installing UniPipe-CLI
      - name: mkdir-bin-folder
        run: mkdir -p ~/.local/bin
      - name: unipipe-cli-cache
        id: unipipe-cli-cache
        uses: actions/cache@v2
        with:
          path: ~/.local/bin
          key: \${{ runner.os }}-unipipe-cli-\${{env.unipipe-version}}
      - name: add-bin-folder-to-path
        run: echo "~/.local/bin" >> $GITHUB_PATH
      - name: download-unipipe-cli
        if: steps.unipipe-cli-cache.outputs.cache-hit != 'true'
        run: |
          cd ~/.local/bin
          wget --no-verbose https://github.com/meshcloud/unipipe-cli/releases/download/\${{env.unipipe-version}}/unipipe-x86_64-unknown-linux-gnu
          mv unipipe-x86_64-unknown-linux-gnu unipipe
          chmod +x unipipe
      - name: checkout # Checks-out your repository under $GITHUB_WORKSPACE, so your job can access it
        uses: actions/checkout@master
      ### Transform your example instances
      - name: transform-example
        run: |
          unipipe --version
          unipipe list ./
          # Set your handler
          unipipe transform --registry-of-handlers=YOUR_EXAMPLE_HANDLER_FILE.js ./
      - name: Configure git for Bot
        run: |
          git config --global user.email "platform-operator@example.com"
          git config --global user.name "Bot by example.com"
      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v1
      - name: execute-terraform-templates
        run: |
          git checkout master
          chmod +x example/execute-terraform-templates.sh
          example/execute-terraform-templates.sh
          git add .
          git diff-index --quiet HEAD || git commit -m "Updating Examples"
          git push
`