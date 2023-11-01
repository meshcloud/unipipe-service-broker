export const githubWorkflow = `
name: Update Instances
concurrency: osb-instances # signal that we're modifying instances

env:
  # Use Github Repository Secrets to set your credentials.
  MY_SECRET_KEY: \${{ secrets.MY_SECRET_KEY }}

on:
  # run on any push to main
  push:
    branches:
      - "main"

# A workflow run is made up of one or more jobs that can run sequentially or in parallel
jobs:
  process-vnets:
    # The type of runner that the job will run on
    runs-on: ubuntu-latest
    steps:
      - name: install unipipe cli
        uses: meshcloud/setup-unipipe@v1
      - name: checkout # Checks-out your repository under $GITHUB_WORKSPACE, so your job can access it
        uses: actions/checkout@main
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