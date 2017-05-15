# Contributing to the Optimizely Java SDK

We welcome contributions and feedback! All contributors must sign our [Contributor License Agreement (CLA)](https://docs.google.com/a/optimizely.com/forms/d/e/1FAIpQLSf9cbouWptIpMgukAKZZOIAhafvjFCV8hS00XJLWQnWDFtwtA/viewform) to be eligible to contribute. Please read the [README](README.md) to set up your development environment, then read the guidelines below for information on submitting your code.

## Development process

1. Create a branch off of `master`: `git checkout -b YOUR_NAME/branch_name`.
2. Commit your changes. Make sure to add tests!
3. Run `./gradlew clean check` to automatically catch potential bugs.
4. `git push` your changes to GitHub.
5. Make sure that all unit tests are passing and that there are no merge conflicts between your branch and `master`.
6. Open a pull request from `YOUR_NAME/branch_name` to `master`.
7. A repository maintainer will review your pull request and, if all goes well, squash and merge it!

## Pull request acceptance criteria

* **All code must have test coverage.** We use JUnit. Changes in functionality should have accompanying unit tests. Bug fixes should have accompanying regression tests.
  * Tests are located in `core-api/src/test`.
* Version will be bumped automatically through Gradle after your change is merged.
* Make sure `./gradlew check` runs successfully before submitting.

## Style

Refer to the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

## License

All contributions are under the CLA mentioned above. For this project, Optimizely uses the Apache 2.0 license, and so asks that by contributing your code, you agree to license your contribution under the terms of the [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0). Your contributions should also include the following header:

```
/****************************************************************************
 * Copyright YEAR, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/
 ```

The YEAR above should be the year of the contribution. If work on the file has been done over multiple years, list each year in the section above. Example: Optimizely writes the file and releases it in 2014. No changes are made in 2015. Change made in 2016. YEAR should be “2014, 2016”.

## Contact

If you have questions, please contact developers@optimizely.com.
