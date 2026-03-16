# Makefile for Aurum Project

.PHONY: start stop restart restart-fe restart-be

# Trick to handle 'make restart fe' or 'make restart be'
ifeq (restart,$(firstword $(MAKECMDGOALS)))
  RESTART_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  $(eval $(RESTART_ARGS):;@:)
endif

start:
	@echo "🚀 Starting Aurum Frontend (in background)..."
	@cd aurum-fe && npm start &
	@echo "🐳 Starting Aurum Database (Docker)..."
	@cd aurum-be && docker compose up -d
	@echo "⚙️ Starting Aurum Backend..."
	@cd aurum-be && ./mvnw spring-boot:run

stop:
	@echo "🛑 Stopping Aurum Services..."
	@cd aurum-be && docker compose down
	@echo "💀 Killing Frontend and Backend processes..."
	@pkill -f "ng serve" || true
	@pkill -f "spring-boot" || true
	@echo "✅ Done."

restart:
	@if [ "$(RESTART_ARGS)" = "fe" ]; then \
		$(MAKE) restart-fe; \
	elif [ "$(RESTART_ARGS)" = "be" ]; then \
		$(MAKE) restart-be; \
	else \
		echo "Usage: make restart [fe|be]"; \
	fi

restart-fe:
	@echo "🔄 Restarting Frontend..."
	@pkill -f "ng serve" || true
	@cd aurum-fe && npm start &
	@echo "✅ Frontend restart initiated in background."

restart-be:
	@echo "🔄 Restarting Backend..."
	@pkill -f "spring-boot" || true
	@cd aurum-be && ./mvnw spring-boot:run &
	@echo "✅ Backend restart initiated in background."
