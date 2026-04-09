html-streaming
==============

Demo of Play's HTML streaming APIs inspired by [this talk](https://www.youtube.com/watch?v=4b1XLka0UIw&feature=youtu.be&t=32m55s) and [this code](https://github.com/brikis98/ping-play)

# How to run the app

This is a standard [Play Framework](http://www.playframework.com/) app, built on Play 2.2.0. To run it,
[install sbt](http://www.scala-sbt.org/) and do `sbt run`.

## Configuration

**Important**: Before running this application, you must provide a secure application secret. The default placeholder value in `conf/application.conf` must be overridden.

> **Security note**: A previous version of this repository contained a hardcoded secret in git history (commit `9506bd5`). That secret should be treated as compromised and must not be reused.

Set the `APPLICATION_SECRET` environment variable:

```bash
export APPLICATION_SECRET="your-secure-secret-here"
sbt run
```

Alternatively, pass it as a JVM property feeding the HOCON substitution directly:

```bash
sbt -DAPPLICATION_SECRET="your-secure-secret-here" run
```

Or override `application.secret` directly via JVM property:

```bash
sbt -Dapplication.secret="your-secure-secret-here" run
```

**Never commit actual secrets to version control.** Use environment variables or external configuration management for production deployments.

