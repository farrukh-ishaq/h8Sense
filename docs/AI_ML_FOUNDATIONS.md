# AI / ML Foundation Exercises

This project is a good foundation for learning applied AI systems because it already has:
- prompt-based inference
- structured output parsing
- persistence
- human feedback
- a UI layer
- observability

These exercises are ordered from easiest to most useful.

## Exercise 1: Run the app in mock mode

Goal: understand the end-to-end pipeline without external dependencies.

Set:
```dotenv
LLM_PROVIDER=none
APP_CONTENT_FETCH_ENABLED=false
APP_CONTENT_BACKFILL_ENABLED=false
APP_SAMPLE_DATA_ENABLED=true
```

Observe:
- seeded analyses appear
- `modelUsed` becomes `MockChatModel`
- you can inspect API responses without paying for inference

## Exercise 2: Compare mock vs Ollama outputs

Goal: understand what the LLM changes.

Steps:
1. analyze the same payload in mock mode
2. switch to Ollama
3. analyze the same payload again
4. compare:
   - confidence
   - category
   - references
   - raw response

Try with:
```bash
curl -X POST http://localhost:8080/api/v1/content/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "content": "All Muslims support terrorism and want to destroy our way of life.",
    "sourceType": "SOCIAL_MEDIA",
    "platform": "CLI",
    "author": "exercise"
  }'
```

## Exercise 3: Tighten the prompt contract

Goal: learn prompt engineering for structured outputs.

Edit:
- `src/main/java/com/islamophobia/detector/ai/IslamicContentAnalyzer.java`

Ideas:
- require stricter JSON only
- add explicit refusal rules
- separate hateful stereotypes from factual criticism
- require a short “reasoning summary” field that is safe to store

Validation:
- run tests
- analyze 3-5 example inputs
- check that parsing still succeeds

## Exercise 4: Add a new classification category

Goal: learn taxonomy design and downstream impact.

Edit:
- `ViolationCategory`
- `ContentAnalysisService.resolveCategory()`
- `MainView` category filter

Good candidate categories:
- `DOG_WHISTLE`
- `EXCLUSIONARY_POLICY`
- `HARASSMENT`

## Exercise 5: Add a heuristic pre-filter before LLM inference

Goal: reduce cost and improve throughput.

Where:
- `ContentAnalysisService`
- or a new service called before `IslamicContentAnalyzer`

Examples:
- skip LLM when content is too short
- skip when no monitored keywords exist
- send only likely-risky items to the model

Measure:
- number of LLM calls before/after
- number of false negatives introduced

## Exercise 6: Build a tiny evaluation set

Goal: start thinking like an ML engineer.

Create a local spreadsheet or JSON file with:
- sample text
- expected label
- expected severity
- notes

Then test:
- accuracy by category
- borderline cases
- failure cases

Keep at least these groups:
- explicit hate speech
- subtle bias
- factual criticism
- neutral reporting
- ambiguous sarcasm

## Exercise 7: Capture human feedback for retraining ideas

Goal: turn the existing feedback system into a future learning signal.

Current building blocks already exist:
- `UserFeedback`
- `FeedbackTracking`

Your task:
1. add an export endpoint or SQL query
2. group disagreements by category
3. inspect which prompts/classes fail most often

This is the bridge between app development and real ML iteration.

## Exercise 8: Explore retrieval-augmented generation

Goal: move from “single prompt” to “prompt + context”.

Future idea:
- store curated Quran/Hadith/reference passages
- retrieve relevant passages before inference
- inject retrieved context into the prompt

This project does not yet implement vector search, but it is a natural next step.

## Exercise 9: Add model comparison mode

Goal: compare multiple providers on the same input.

Idea:
- run the same content through mock / Ollama / OpenAI
- store outputs side by side
- compare disagreement rate and cost

This is excellent groundwork for later evaluation dashboards.

## Good habits while learning AI here

- keep prompts versioned
- store raw outputs for review
- separate “operator-safe explanation” from raw model text
- do not silently trust one model response
- add small deterministic tests around parsing and mapping logic

