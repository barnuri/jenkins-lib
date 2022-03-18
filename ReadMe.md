# Jenkins Library

## For import this library put in your jenkins file

```
@Library('jenkins-lib')_
```

---
## Function Structure

every file name in folder vars is name of function

to create new function create new file in vars folder with the name of your function

and then create function with name call

example if you want to create function with name init

filename: vars/init.groovy
file code:

```
#!/usr/bin/env groovy

def call(someArg) {
    echo "init func from shared lib"
    echo someArg
}
```

