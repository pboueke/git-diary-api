# git-diary-api

Simple API that manages a git repository containing diary/blog posts

## Endpoints

All operations below are working directly with the git repository. There is an expectation that the repository is managed only by this API, merge conflicts are not handled by the API. While the manual addition of a post directly in the repository is supported, it is not recommended

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

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2019 Pedro Boueke
