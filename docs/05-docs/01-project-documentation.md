# Project Documentation

## Example

As you go through the steps below, please refer to my eShop sandbox project [https://github.com/optivem/atdd-accelerator-eshop](https://github.com/optivem/atdd-accelerator-eshop), see: README.

## 1. Update README

Create a README file in the System Repository.

In the System Repository README file, add the following:

- System Name (this could be the title at the top of your README)
- Background Context: Write a few sentences to explain what this project is and why you're making it.
- Contributors: Add the names of the Project member(s) and links to their GitHub profiles.
- Licence: MIT Licence

## 2. Create Documentation (GitHub Pages)

Based on good practices, the README.md file should be short, whereas technical documentation should be held separately (e.g., use case diagram, architecture diagram, testing procedure - which we'll be adding in a subsequent section).

I'm going to be using GitHub Pages, though I leave it up to you how you want to do it. You can choose to do GitHub Pages, or decide to directly write your documentation in README, or any other alternatives on GitHub.

Setting up the docs folder:

1. Create folder /docs
2. Add a file index.md and write something in there

Setting up GitHub Pages:

1. Go to Settings → Pages → Build and deployment:
2. Select Source: Deploy from a branch
3. Select Branch: main
4. Select Folder: /docs
5. Click "Save"

Setting up the GitHub Pages Status Badge:

1. Go to GitHub Actions → pages-build-deployment → "Create status badge". *Note: You can find it by going to the top menu where you see "Code", "Issues", "Pull Requests", "Actions"... Click on "Actions" there. Then, in the top-right corner, click on the ellipsis "..." → "Create status badge"*
2. Copy-paste the status badge in your README.md file.

Adding the GitHub Pages Website Link:

1. Open up your GitHub Repository on [github.com](http://github.com)
2. In the README file, you should see the status "passing"
3. In the top-right, in the "About" click on the "Gear" symbol, and in "Website" field, check "Use your GitHub Pages website"
4. Create a section in your README called "Documentation" and copy-paste the website link there too

## Checklist

1. README includes Background Context
2. README includes Contributors (with links to their GitHub profiles)
3. [If GitHub Pages] Status badge appears on README
4. [If GitHub Pages] Website link is on the README
5. [If GitHub Pages] Website link is in the Repository About section
