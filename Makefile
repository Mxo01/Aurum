.PHONY: start stop restart

start:
	@cd aurum-fe && npm start &
	@cd aurum-be && docker compose up -d
	@cd aurum-be && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

stop:
	@cd aurum-be && docker compose down
	@pkill -f "ng serve" || true
	@pkill -f "spring-boot" || true

restart:
	@$(MAKE) stop
	@$(MAKE) start
