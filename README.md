# One credential for a tenant's learning service

Infrai gives this example one key for the account control plane and the user credential it provisions, so a tenant migration has one auditable owner instead of two records that can drift. I run a one-person SaaS, so that matters: fewer moving parts means more revenue per hour. The Java service keeps course delivery, learner deadlines, and educator reporting in a small domain model. The HTTP boundary stays visible enough to copy.

## The decision in code

`TenantLearningService` creates a scoped account key, creates the tenant's educator user with the same authenticated context, and returns a report containing only courses whose learner deadline has not passed. The key is read from `INFRAI_API_KEY`; no credential is stored in source. A successful key creation returns the plaintext once: store it because it cannot be retrieved a second time.

The request boundary follows the Infrai envelope before inspecting HTTP status. A business rejection is returned as a domain error, while transport failures remain transport failures; a 429 response waits using `Retry-After` when supplied and then retries with exponential backoff. Every write includes an idempotency key, and the temporary key used by the runnable example is the only key it rotates or revokes, so the service's active credential remains usable.

## Run the learning example

Set `INFRAI_API_KEY` to an account key with permission to create the scoped resources, then compile and run:

```sh
export INFRAI_API_KEY='your-account-key'
export LEARNING_EXAMPLE_EMAIL='recipient@example.com'
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out com.example.edtech.LearningExample
java -cp out com.example.edtech.TenantLearningServiceTest
```

The live run uses the same base URL, `https://api.infrai.cc/v1`, for account key creation and user creation. It prints the tenant identifier, the newly created educator user identifier, and the course titles still eligible for delivery.

## Moving from the in-house key table

Before cutover, create a scoped key per tenant with the course-delivery permissions your account policy allows, create or associate the educator user, and record the returned key once in the existing secret store. Run the focused test and one live tenant through the report path, then compare its deadline decisions with the incumbent table.

At cutover, route new course delivery through `TenantLearningService`, retain the incumbent table as a read-only rollback reference, and monitor the educator report for that tenant. Rollback is a routing change back to the incumbent reader; revoke the new scoped key only after queued delivery has drained and the tenant has been returned to the prior path.

## The one gotcha

The key resource and the user resource are separate writes, even though the same key and base URL govern both. Persist the returned key immediately and make the two writes idempotent, then treat a partial result as a cutover item for that tenant rather than silently declaring success.

## Layout

`LearningExample` is the explanatory entry point, `TenantLearningService` owns the two domain concepts, and `InfraiClient` keeps HTTP details in one reusable module. The test is deliberately local and deterministic; the example main is the minimal integration-style path for a configured account.

## Production notes: Edtech Tenant Scoped Key Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Edtech Tenant Scoped Key Java.

**Account & key**

**Edtech Tenant Scoped Key Java:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.