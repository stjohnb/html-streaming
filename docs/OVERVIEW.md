# html-streaming Overview

## Purpose

A Play Framework 2.2.0 demo application (Scala) that showcases progressive HTML streaming — sending HTML to the browser in chunks as data becomes available, rather than waiting for all data before responding. The app illustrates several streaming composition strategies using Play's `Enumerator`/`Iteratee` API and a custom `.scala.stream` template format.

## Architecture

```
app/
├── controllers/
│   └── MainController.scala    # All HTTP endpoints + mock services (CartService, WaitlistService)
├── ui/
│   ├── HtmlStream.scala        # Core streaming abstraction wrapping Enumerator[Html]
│   └── Pagelet.scala           # Wraps HTML in script tags for incremental DOM injection
├── views/
│   ├── index.scala.html        # Non-streaming layout (Bootstrap navbar)
│   ├── index.scala.stream      # Streaming layout base template
│   ├── full.scala.stream       # Full pagelet streaming template
│   ├── cart.scala.html         # Cart content component (blue)
│   ├── waitlist.scala.html     # Waitlist content component (green)
│   └── pagelet.scala.html      # Pagelet wrapper (renders `<script>` injection tag)
└── assets/
    └── stylesheets/index.less
conf/
├── application.conf            # App secret, logging levels
└── routes                      # 6 GET endpoints → MainController
project/
├── plugins.sbt                 # Play 2.2.0 sbt plugin
└── build.properties            # SBT 0.13.6
build.sbt                       # Registers "stream" template type → HtmlStreamFormat
public/                         # Static assets (Bootstrap CSS/JS, favicon)
test/
└── IntegrationTest.java        # HtmlUnit integration tests for all endpoints
ThreatDragonModels/             # OWASP Threat Dragon security model (empty diagrams)
```

## Key Patterns

### 1. `HtmlStream` — the core abstraction (`app/ui/HtmlStream.scala`)

`HtmlStream` is a case class wrapping `Enumerator[Html]`. It exposes:

- `andThen(other)` — sequential composition: stream `this` to completion, then stream `other`
- `HtmlStream.interleave(streams*)` — parallel composition: emit chunks from whichever stream produces output first
- `HtmlStream.flatten(Future[HtmlStream])` — lifts an async stream into a synchronous one
- `HtmlStream(Future[Html])` — wraps a single async HTML value as a one-chunk stream

`HtmlStreamImplicits.toEnumerator` provides an implicit conversion so `HtmlStream` can be passed directly to `Ok.chunked(...)`. It filters empty chunks (which signal EOF in chunked encoding).

### 2. `.scala.stream` templates

Registered in `build.sbt` as a custom template type:
```scala
templatesTypes += ("stream" -> "ui.HtmlStreamFormat")
```
These templates return `HtmlStream` instead of `Html`, allowing static markup to be mixed with streaming `HtmlStream` values. Files: `index.scala.stream`, `full.scala.stream`.

### 3. `Pagelet` — incremental DOM injection (`app/ui/Pagelet.scala`)

`Pagelet.renderStream(htmlFuture, id)` wraps completed HTML in a `<script>` tag that injects content into a DOM placeholder by ID. This enables the `full` endpoint to stream pagelet chunks out-of-order while the browser progressively fills in page sections.

### 4. Mock services with simulated latency

`CartService` (500 ms delay) and `WaitlistService` (3000 ms delay) live in `MainController.scala` and use `Promise.timeout` to simulate slow backend calls — making the streaming benefit visible in the browser.

## Endpoints (`conf/routes`)

| Method | Path          | Controller action     | Description |
|--------|---------------|-----------------------|-------------|
| GET    | /             | `index`               | Non-streaming: waits for both services, renders all at once |
| GET    | /helloEnum    | `helloEnumerator`     | Basic chunked text: streams three strings |
| GET    | /repeat       | `repeat`              | Infinite stream: repeats "Hello" every 200 ms |
| GET    | /enumerators  | `enumerators`         | Interleaved text streams (200 ms and 1000 ms) |
| GET    | /andThen      | `andThen`             | Sequential HTML streaming: cart first, then waitlist |
| GET    | /interleaved  | `interleaved`         | Parallel HTML streaming: whichever resolves first |
| GET    | /full         | `full`                | Pagelet streaming: placeholders filled as data arrives |
| GET    | /assets/...   | Assets controller     | Static files |

## Configuration

| File | Key settings |
|------|-------------|
| `conf/application.conf` | `application.secret`, `application.langs = "en"`, logging: root=ERROR, play=INFO, app=DEBUG |
| `build.sbt` | `name = "html-streaming"`, `version = "1.0-SNAPSHOT"`, custom `stream` template type |
| `project/plugins.sbt` | `play.Project` plugin 2.2.0 |
| `project/build.properties` | `sbt.version = 0.13.6` |

## Data Flow

```
Browser GET /full
    │
    ▼
MainController.full
    ├── CartService.cart   ──► Future[String] (resolves in ~500ms)
    └── WaitlistService.waitlist ──► Future[String] (resolves in ~3000ms)
    │
    ▼
Each Future[String] mapped to Future[Html] via views.html.cart/waitlist
    │
    ▼
Pagelet.renderStream wraps each in a script-injection HtmlStream
    │
    ▼
HtmlStream.interleave merges both streams
    │
    ▼
Ok.chunked(views.stream.full(body))
    │   (implicit HtmlStream → Enumerator[Html] conversion)
    ▼
Browser receives chunks progressively; script tags inject content into placeholders
```

## Testing

`test/IntegrationTest.java` uses Play's `WithBrowser` (HtmlUnit) to verify that the four main streaming endpoints return a page containing "My shop".

## Security Notes

A Threat Dragon model exists at `ThreatDragonModels/Test title/Test title.json` but contains empty diagrams — intended as a placeholder for future threat modeling work.
