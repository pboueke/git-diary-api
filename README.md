# git-diary-api

Simple API that manages a git repository containing diary/blog posts.

Simple:

* Posts only contain a title and a body
* The only metadata is the date, contained in the post name
* No databases, only a git repository
* All posts contained in a single fonder, `posts`

## Endpoints

All operations below are working directly with the git repository beeing managed. There is an expectation that the repository is managed only by this API, merge conflicts are not handled by the API. While the manual addition of a post directly in the repository is supported, it is not recommended. All requests must be authorized with a header: `Authorization Token <token>`.

* GET /posts/
    * returns a JSON list of strings containing all the post names

* PUT /post/new
    * receives an object like `{"title": "string", "body": "string"}` and creates a new post with this info

* GET /post/:name
    * returns the contents of the post with the selected name as plain text

* DELETE /post/:name
    * deletes the post with the selected name

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

Git must be installed in the API host, and the managed repository must be cloned there. A ssh key is required for pushing changes to the remote repository.

## Config

The `config.json` file must be configured with info related to your repository and user.

## Running

To start a web server for the application, run:

    lein ring server

## License

MIT
