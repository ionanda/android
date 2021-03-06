ohmage
======

This is the repository for the ohmage Android application.

DEPENDENCIES
------------

There is a dependency on `ReminderLib` which can be found at
[https://github.com/ohmage/android-reminder-lib](https://github.com/ohmage/android-reminder-lib) and
must be installed to maven local. (run `.gradlew uploadArchives` in the android-reminder-lib
directory.


HOW TO BUILD THE PROJECT
------------------------

This android project uses the gradle build system. You should have the Google Repository component
of the sdk and the newest version of the build tools. To build a debug version of the app run:

    ./gradlew assembleDebug

To build the release version of the app add the `signing.gradle` file as described below and run:

    ./gradlew assembleRelease


SIGNING
-------

To create a signed APK to distribute, create `ohmage/signing.gradle` with the the location of your
keystore and the signing information. It should look something like this:

    android {
        signingConfigs {
            release {
                storeFile new File(System.getProperty('user.home'), '.android/ohmage-key.keystore')
                storePassword "password"
                keyAlias "ohm-key"
                keyPassword "password"
            }
        }
        buildTypes {
            release {
                signingConfig signingConfigs.release
            }
        }
    }

CONTRIBUTE
----------

If you would like to contribute code to the ohmage android client you can do so through
GitHub by forking the repository and sending a pull request.

In general if you are contributing we ask you to follow the
[AOSP coding style guidelines](http://source.android.com/source/code-style.html).
If you are using an IDE such as Eclipse, it is easiest to use the
[AOSP style formatters](http://source.android.com/source/using-eclipse.html#eclipse-formatting). If
you are using Android Studio the default formatting should conform to the AOSP guidelines.

You may [file an issue](https://github.com/ohmage/android/issues/new) if you find bugs or would
like to add a new feature.

LICENSE
-------

    Copyright (C) 2013 ohmage

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[gist]: https://gist.github.com/f2prateek/5606337
[sealskej]: https://gist.github.com/f2prateek/5606337/#comment-903996
