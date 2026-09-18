# COMP3011 Assignment 1 - Speech to Text

A Spring Boot web app that records audio in the browser, sends it to the OpenAI
speech to text API, and shows the transcript. Also has the admin and statistics
endpoints from the supplied YAML.

## Running it

Needs a JDK 21 or newer (built on Temurin 25). Maven isn't needed, the project
has the wrapper.

```
cd Assignment1
./mvnw -DskipTests package
java -jar target/Assignment1-0.0.1-SNAPSHOT.jar
```

Then open http://localhost:8080/

The API key is read from the `OPENAI_API_KEY` environment variable, which is what
TITAN sets. To run locally without one, use the stub profile:

```
java -jar target/Assignment1-0.0.1-SNAPSHOT.jar --spring.profiles.active=stub
```

## Endpoints

| Method | Path | What it does |
| ------ | ---- | ------------ |
| GET | `/` | the recording page |
| POST | `/api/v1/transcriptions` | takes the audio, returns the text |
| GET | `/api/v1/admin/uptime` | server start, now, uptime in seconds |
| GET | `/api/v1/global/stats` | tokens used since the server started |
| POST | `/api/v1/admin/shutdown` | graceful shutdown, 409 if already shutting down |

## Layout

```
config/    the RestClient bean for openai
dto/       records matching the yaml schemas
service/   the classes that own decisions and state
web/       @RestController classes, they only map http
```

Controllers map HTTP and nothing else, services own the rules and the state.

## Choices worth explaining

**Virtual threads.** `spring.threads.virtual.enabled=true`. The transcription
request blocks while it waits for OpenAI, which can be seconds. With normal
platform threads each waiting request holds one of Tomcat's ~200 threads, so a
few hundred recordings at once would starve everything else including the admin
endpoints. Virtual threads are cheap enough that a blocked one doesn't stop other
requests being served.

**The token counters use `synchronized`.** `inputTokens += n` is a read, an add
and a write, which is the same race as the bank account in Practical 1. The
getter is synchronized too, otherwise a reader could catch one counter updated
and the other not and report a pair that was never actually true. Two separate
`AtomicLong`s would make each counter safe on its own but wouldn't fix that.
The locking is deliberately small and never held across the OpenAI call, so it
doesn't become the bottleneck under load.

**Shutdown uses `AtomicBoolean.compareAndSet`.** Checking then setting as two
steps would let two simultaneous requests both start a shutdown. The actual
shutdown runs on its own thread so the 202 gets sent before the server stops.

**Timeouts.** Connect 5s, read 30s. Without them a hung OpenAI response would
hold a request forever. A timeout only tells us we stopped waiting, not whether
OpenAI did the work, so nothing is retried automatically.

**Error codes.** 400 for a bad upload, 413 if the recording is too big, 502 if
OpenAI fails (the request was fine, the upstream wasn't), 409 for a repeat
shutdown. Built in one `@RestControllerAdvice` so every endpoint returns the
same error shape as the YAML.

## The API key

Read from `OPENAI_API_KEY` at runtime and never written into a file, logged or
sent to the browser. Startup logs whether a key was found, never the value.
`.gitignore` also blocks `.env` files.

The stub profile means local development needs no key at all, and the same code
runs on TITAN where the variable is set - no code changes between the two.

## Testing

Checked by hand against a running server:

- each endpoint returns the shape and status the YAML specifies
- timestamps come out as RFC 3339 strings, not epoch numbers
- error paths: 400 on an empty upload, 400 with no multipart body, 413 on a 30MB
  upload, 405 on the wrong method, 404 on an unknown path, 502 when OpenAI can't
  be reached
- three simultaneous shutdown requests give exactly one 202 and two 409s
- **250 concurrent uploads against the stub: all 250 returned 200, no failures,
  4.9 seconds total.** The token counters finished on exactly 2500 / 1250, which
  is 250 x (10 in, 5 out) with no lost updates, so the synchronized counters hold
  up under that load.
