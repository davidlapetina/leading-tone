# Ports are off the usual defaults so this stack can run beside another one.
MUSIC_HTTP_PORT ?= 8088

.PHONY: help backend backend-offline frontend test test-backend test-frontend \
        test-e2e typecheck lint build check clean wipe

help:
	@echo "make backend       run the API on $(MUSIC_HTTP_PORT)"
	@echo "make backend-offline  the same, with no language model at all"
	@echo "make frontend      run the interface on 5173"
	@echo "make test          backend and frontend test suites"
	@echo "make test-e2e      Playwright, against a running stack"
	@echo "make check         test, lint, typecheck and production build"
	@echo "make wipe          throw the learner model away and start again"

backend:
	cd backend && MUSIC_HTTP_PORT=$(MUSIC_HTTP_PORT) ./mvnw quarkus:dev

# Deterministic teaching with no model in the loop. This is how the e2e tests expect it.
backend-offline:
	cd backend && MUSIC_HTTP_PORT=$(MUSIC_HTTP_PORT) \
		MUSIC_DEFAULTS_LLM_ENABLED=false ./mvnw quarkus:dev

frontend:
	cd frontend && pnpm dev

# Needs nothing installed: an in-memory database and the deterministic tutor.
test-backend:
	cd backend && ./mvnw test

test-frontend:
	cd frontend && pnpm test

test: test-backend test-frontend

# Requires a running backend; the specs switch the language model off themselves. Playwright
# starts the frontend.
test-e2e:
	@curl -sf http://localhost:$(MUSIC_HTTP_PORT)/q/health > /dev/null \
		|| (echo "The backend is not running. Start 'make backend-offline' first." && exit 1)
	cd frontend && pnpm exec playwright test

typecheck:
	cd frontend && pnpm typecheck

lint:
	cd frontend && pnpm lint

build:
	cd backend && ./mvnw package -DskipTests
	cd frontend && pnpm build

check: test lint typecheck build

# Progress lives in one file, so starting over is deleting it.
wipe:
	rm -rf backend/data

clean:
	cd backend && ./mvnw clean
	rm -rf frontend/dist frontend/node_modules/.vite frontend/test-results
