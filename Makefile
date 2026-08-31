# Ports are off the usual defaults so this stack can run beside another one.
MUSIC_DB_PORT   ?= 5433
MUSIC_HTTP_PORT ?= 8088
OLLAMA_MODEL    ?= qwen3:8b

.PHONY: help db db-stop db-reset backend backend-offline frontend test test-backend \
        test-frontend test-e2e typecheck lint build check clean

help:
	@echo "make db            start PostgreSQL on $(MUSIC_DB_PORT)"
	@echo "make backend       run the API on $(MUSIC_HTTP_PORT) with $(OLLAMA_MODEL)"
	@echo "make backend-offline  the same, with no language model at all"
	@echo "make frontend      run the interface on 5173"
	@echo "make test          backend and frontend test suites"
	@echo "make test-e2e      Playwright, against a running stack"
	@echo "make check         test, lint, typecheck and production build"
	@echo "make db-reset      throw the database away and start again"

db:
	MUSIC_DB_PORT=$(MUSIC_DB_PORT) docker compose up -d postgres

db-stop:
	docker compose stop postgres

db-reset:
	docker compose down -v
	MUSIC_DB_PORT=$(MUSIC_DB_PORT) docker compose up -d postgres

backend: db
	cd backend && MUSIC_HTTP_PORT=$(MUSIC_HTTP_PORT) MUSIC_DB_PORT=$(MUSIC_DB_PORT) \
		OLLAMA_MODEL=$(OLLAMA_MODEL) ./mvnw quarkus:dev

# Deterministic teaching with no model in the loop. This is how the e2e tests expect it.
backend-offline: db
	cd backend && MUSIC_HTTP_PORT=$(MUSIC_HTTP_PORT) MUSIC_DB_PORT=$(MUSIC_DB_PORT) \
		MUSIC_LLM_ENABLED=false ./mvnw quarkus:dev

frontend:
	cd frontend && pnpm dev

# Needs Docker: the backend integration tests get a throwaway Postgres from Dev Services.
test-backend:
	cd backend && ./mvnw test

test-frontend:
	cd frontend && pnpm test

test: test-backend test-frontend

# Requires a running stack. Start `make db` and `make backend-offline` first; Playwright
# starts the frontend itself.
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

clean:
	cd backend && ./mvnw clean
	rm -rf frontend/dist frontend/node_modules/.vite frontend/test-results
